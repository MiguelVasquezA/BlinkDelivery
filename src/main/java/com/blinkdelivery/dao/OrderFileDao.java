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
import java.util.List;

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
 *
 * @author BlinkDelivery
 * @version 2.0
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
    private final AddresFileDao addressDao = new AddresFileDao();

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
                ps.setString(6, order.getEstado().name());
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
            throw new IOException("No se pudo guardar el pedido en la base de datos.", e);
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
            ps.setString(1, order.getEstado().name());
            if (order.tieneDomiciliario()) {
                ps.setInt(2, Ids.entero(order.getDomiciliario().getIdentification()));
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setBigDecimal(3, BigDecimal.valueOf(order.getTotal()));
            ps.setInt(4, Ids.pedido(order.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("No se pudo actualizar el pedido en la base de datos.", e);
        }
    }

    /**
     * @return Todos los pedidos. Se omiten los que referencian datos que ya no existen.
     * @throws IOException si falla la base de datos
     */
    public List<Order> listar() throws IOException {
        return consultar(SELECCION + " ORDER BY id_pedido", null);
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
        List<Order> r = consultar(SELECCION + " WHERE id_pedido = ?",
                ps -> ps.setInt(1, Ids.pedido(id)));
        return r.isEmpty() ? null : r.get(0);
    }

    // ------------------------------------------------------------ auxiliares

    /** Parámetros opcionales de una consulta. */
    @FunctionalInterface
    private interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    private List<Order> consultar(String sql, Parametros parametros) throws IOException {
        List<Order> pedidos = new ArrayList<>();
        try (Connection c = Db.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (parametros != null) {
                parametros.asignar(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = construir(c, rs);
                    if (o != null) {
                        pedidos.add(o);
                    }
                }
            }
        } catch (SQLException e) {
            throw new IOException("No se pudieron leer los pedidos de la base de datos.", e);
        }
        return pedidos;
    }

    private Order construir(Connection c, ResultSet rs) throws SQLException, IOException {
        String id = Ids.pedido(rs.getInt("id_pedido"));
        Customer cliente = customerDao.buscarPorId(String.valueOf(rs.getInt("id_cliente")));
        Restaurant restaurante = restaurantDao.buscarPorId(String.valueOf(rs.getInt("id_restaurante")));
        if (cliente == null || restaurante == null) {
            return null;
        }

        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_producto, cantidad, precio_unitario FROM detalle_pedido WHERE id_pedido = ? "
                        + "ORDER BY id_detalle_pedido")) {
            ps.setInt(1, rs.getInt("id_pedido"));
            try (ResultSet det = ps.executeQuery()) {
                while (det.next()) {
                    Product producto = productDao.buscarPorId(String.valueOf(det.getInt("id_producto")));
                    if (producto == null) {
                        return null;
                    }
                    items.add(new OrderItem(producto, det.getInt("cantidad"), det.getBigDecimal("precio_unitario").doubleValue()));
                }
            }
        }
        if (items.isEmpty()) {
            return null;
        }

        Address entrega = direccionDe(cliente, rs.getInt("id_direccion"));
        int idDomiciliario = rs.getInt("id_domiciliario");
        DeliveryDriver domiciliario = rs.wasNull() ? null
                : driverDao.buscarPorId(String.valueOf(idDomiciliario));

        return new Order(id, cliente, restaurante, entrega, items,
                OrderStatus.desdeTexto(rs.getString("estado")), aFechaLocal(rs.getObject("fecha_pedido", OffsetDateTime.class)),
                domiciliario, new PercentageCommissionPolicy());
    }

    /** Dirección de entrega guardada; si ya no existe, la principal del cliente. */
    private Address direccionDe(Customer cliente, int idDireccion) throws IOException {
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
