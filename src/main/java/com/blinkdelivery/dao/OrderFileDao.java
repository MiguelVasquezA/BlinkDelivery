package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Address;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.PercentageCommissionPolicy;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import com.blinkdelivery.modelo.ZonaHoraria;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * DAO de pedidos sobre PostgreSQL (tablas {@code pedido} y {@code detalle_pedido}).
 * <p>
 * Nombre heredado: antes persistía en {@code data/orders.txt}. Reglas:
 * </p>
 * <ul>
 *   <li>El id del pedido es {@code PED-NNNN} en la aplicación y {@code NNNN} en la base.</li>
 *   <li>{@code total} se guarda; subtotal y comisión se recalculan al leer, a partir de
 *       {@code detalle_pedido}, con la política de comisión del modelo.</li>
 *   <li>{@code fecha_pedido} es TIMESTAMPTZ (migración 009); se lee y escribe en hora de Bogotá.</li>
 * </ul>
 * <p>
 * Al listar no se hace una consulta por pedido: se cargan pedidos y líneas en dos consultas
 * y los catálogos (clientes, restaurantes, productos, domiciliarios) en una consulta cada uno.
 * </p>
 *
 * @author BlinkDelivery
 * @version 3.0
 */
public class OrderFileDao {
    private static final String SELECCION = """
            SELECT id_pedido, id_cliente, id_restaurante, id_domiciliario, id_direccion, estado, fecha_pedido
              FROM pedido
            """;

    private final CustomerFileDao customerDao = new CustomerFileDao();
    private final RestaurantFileDao restaurantDao = new RestaurantFileDao();
    private final ProductFileDao productDao = new ProductFileDao();
    private final DriverFileDao driverDao = new DriverFileDao();

    /** Fila de {@code pedido} tal como sale de la base. */
    private record Fila(int idPedido, int idCliente, int idRestaurante, Integer idDomiciliario,
                        int idDireccion, String estado, OffsetDateTime fecha) {
    }

    /** Línea de {@code detalle_pedido} tal como sale de la base. */
    private record Linea(int idPedido, int idProducto, int cantidad, BigDecimal precioUnitario) {
    }

    /**
     * Registra un pedido nuevo con sus productos.
     *
     * @param order pedido a guardar
     * @throws IOException si falla la base de datos
     */
    public void guardar(Order order) throws IOException {
        int idPedido = Ids.pedido(order.getId());
        int idCliente = Ids.entero(order.getCustomerId());
        try (Connection c = Db.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO pedido (id_pedido, id_cliente, id_restaurante, id_domiciliario, id_direccion, "
                            + "estado, fecha_pedido, total, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)")) {
                ps.setInt(1, idPedido);
                ps.setInt(2, idCliente);
                ps.setInt(3, Ids.entero(order.getRestaurantId()));
                if (order.tieneDomiciliario()) {
                    ps.setInt(4, Ids.entero(order.getDomiciliario().getIdentification()));
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setInt(5, idDireccion(c, idCliente, order.getDireccionEntrega()));
                ps.setString(6, order.getEstado().aBaseDatos());
                ps.setObject(7, aFechaBase(order.getFechaHora()));
                ps.setBigDecimal(8, BigDecimal.valueOf(order.getTotal()));
                ps.executeUpdate();
            }
            for (OrderItem item : order.getItems()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario) "
                                + "VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idPedido);
                    ps.setInt(2, Ids.entero(item.getProducto().getId()));
                    ps.setInt(3, item.getCantidad());
                    ps.setBigDecimal(4, BigDecimal.valueOf(item.getPrecioUnitario()));
                    ps.executeUpdate();
                }
            }
            c.commit();
        } catch (SQLException e) {
            throw Db.error("No se pudo guardar el pedido en la base de datos.", e);
        }
    }

    /**
     * Actualiza estado, domiciliario y total de un pedido. Los productos no cambian.
     *
     * @param order pedido con los datos nuevos
     * @throws IOException si falla la base de datos
     */
    public void actualizar(Order order) throws IOException {
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE pedido SET estado = ?, id_domiciliario = ?, total = ? WHERE id_pedido = ?")) {
            ps.setString(1, order.getEstado().aBaseDatos());
            if (order.tieneDomiciliario()) {
                ps.setInt(2, Ids.entero(order.getDomiciliario().getIdentification()));
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setBigDecimal(3, BigDecimal.valueOf(order.getTotal()));
            ps.setInt(4, Ids.pedido(order.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw Db.error("No se pudo actualizar el pedido en la base de datos.", e);
        }
    }

    /**
     * @return Todos los pedidos. Se omiten los que referencian datos que ya no existen.
     * @throws IOException si falla la base de datos
     */
    public List<Order> listar() throws IOException {
        return consultar("", null);
    }

    /**
     * @param id id del pedido ("PED-0003")
     * @return el pedido, o {@code null} si no existe
     * @throws IOException si falla la base de datos
     */
    public Order buscarPorId(String id) throws IOException {
        if (!Ids.esEntero(id.replaceFirst("(?i)^PED-", ""))) {
            return null;
        }
        List<Order> r = consultar(" WHERE id_pedido = ?", Ids.pedido(id));
        return r.isEmpty() ? null : r.get(0);
    }

    // ------------------------------------------------------------ auxiliares

    /**
     * Carga pedidos (y sus líneas) con dos consultas y arma los modelos con los catálogos
     * cargados en lote. Con {@code idUnico} solo trae ese pedido.
     */
    private List<Order> consultar(String condicion, Integer idUnico) throws IOException {
        List<Fila> filas = new ArrayList<>();
        List<Linea> lineas = new ArrayList<>();
        try (Connection c = Db.conectar()) {
            try (PreparedStatement ps = c.prepareStatement(SELECCION + condicion + " ORDER BY id_pedido")) {
                if (idUnico != null) {
                    ps.setInt(1, idUnico);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int dom = rs.getInt("id_domiciliario");
                        filas.add(new Fila(rs.getInt("id_pedido"), rs.getInt("id_cliente"),
                                rs.getInt("id_restaurante"), rs.wasNull() ? null : dom,
                                rs.getInt("id_direccion"), rs.getString("estado"),
                                rs.getObject("fecha_pedido", OffsetDateTime.class)));
                    }
                }
            }
            if (filas.isEmpty()) {
                return new ArrayList<>();
            }
            String sqlLineas = "SELECT id_pedido, id_producto, cantidad, precio_unitario FROM detalle_pedido"
                    + (idUnico == null ? "" : " WHERE id_pedido = ?") + " ORDER BY id_pedido, id_detalle_pedido";
            try (PreparedStatement ps = c.prepareStatement(sqlLineas)) {
                if (idUnico != null) {
                    ps.setInt(1, idUnico);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lineas.add(new Linea(rs.getInt("id_pedido"), rs.getInt("id_producto"),
                                rs.getInt("cantidad"), rs.getBigDecimal("precio_unitario")));
                    }
                }
            }
        } catch (SQLException e) {
            throw Db.error("No se pudieron leer los pedidos de la base de datos.", e);
        }

        // Catálogos: cada listar() toma su propia conexión, después de cerrar la anterior.
        Map<String, Customer> clientes = porId(customerDao.listar(), Customer::getIdentification);
        Map<String, Restaurant> restaurantes = porId(restaurantDao.listar(), Restaurant::getId);
        Map<String, Product> productos = porId(productDao.listar(), Product::getId);
        Map<String, DeliveryDriver> domiciliarios = porId(driverDao.listar(), DeliveryDriver::getIdentification);

        Map<Integer, List<Linea>> lineasPorPedido = new HashMap<>();
        for (Linea l : lineas) {
            lineasPorPedido.computeIfAbsent(l.idPedido(), k -> new ArrayList<>()).add(l);
        }

        List<Order> pedidos = new ArrayList<>();
        for (Fila f : filas) {
            try {
                Order o = construir(f, lineasPorPedido.getOrDefault(f.idPedido(), List.of()),
                        clientes, restaurantes, productos, domiciliarios);
                if (o != null) {
                    pedidos.add(o);
                }
            } catch (RuntimeException e) {
                // Fila corrupta: se registra y se omite para no tumbar el listado completo.
                Db.omitirFila(e);
            }
        }
        return pedidos;
    }

    private static Order construir(Fila f, List<Linea> lineas, Map<String, Customer> clientes,
                                   Map<String, Restaurant> restaurantes, Map<String, Product> productos,
                                   Map<String, DeliveryDriver> domiciliarios) {
        Customer cliente = clientes.get(String.valueOf(f.idCliente()));
        Restaurant restaurante = restaurantes.get(String.valueOf(f.idRestaurante()));
        if (cliente == null || restaurante == null) {
            return null;
        }
        List<OrderItem> items = new ArrayList<>();
        for (Linea l : lineas) {
            Product producto = productos.get(String.valueOf(l.idProducto()));
            if (producto == null) {
                return null;
            }
            items.add(new OrderItem(producto, l.cantidad(), l.precioUnitario().doubleValue()));
        }
        if (items.isEmpty()) {
            return null;
        }
        DeliveryDriver domiciliario = f.idDomiciliario() == null
                ? null : domiciliarios.get(String.valueOf(f.idDomiciliario()));
        return new Order(Ids.pedido(f.idPedido()), cliente, restaurante,
                direccionDe(cliente, f.idDireccion()), items,
                OrderStatus.desdeTexto(f.estado()), aFechaLocal(f.fecha()),
                domiciliario, new PercentageCommissionPolicy());
    }

    private static <T> Map<String, T> porId(List<T> lista, Function<T, String> clave) {
        Map<String, T> mapa = new HashMap<>();
        for (T t : lista) {
            mapa.put(clave.apply(t), t);
        }
        return mapa;
    }

    /** Dirección de entrega guardada; si ya no existe, la principal del cliente. */
    private static Address direccionDe(Customer cliente, int idDireccion) {
        for (Address a : cliente.getDirecciones()) {
            if (a.getId().equals(String.valueOf(idDireccion))) {
                return a;
            }
        }
        return cliente.getDireccionPrincipal();
    }

    /**
     * Id de la dirección de entrega: el suyo si ya está en la base; si no, la principal del cliente.
     */
    private int idDireccion(Connection c, int idCliente, Address direccion) throws SQLException {
        if (direccion != null && Ids.esEntero(direccion.getId())) {
            return Ids.entero(direccion.getId());
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_direccion FROM direccion WHERE id_cliente = ? ORDER BY es_principal DESC, id_direccion LIMIT 1")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("El cliente " + idCliente + " no tiene dirección registrada.");
                }
                return rs.getInt(1);
            }
        }
    }

    /** Convierte una fecha local de negocio (Bogotá) al tipo que guarda PostgreSQL. */
    static OffsetDateTime aFechaBase(LocalDateTime fecha) {
        return fecha.atZone(ZonaHoraria.BOGOTA).toOffsetDateTime();
    }

    /** Convierte la fecha guardada a hora local de Bogotá. */
    static LocalDateTime aFechaLocal(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.atZoneSameInstant(ZonaHoraria.BOGOTA).toLocalDateTime();
    }
}
