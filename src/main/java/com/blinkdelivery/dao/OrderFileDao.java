package com.blinkdelivery.dao;

import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.PercentageCommissionPolicy;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de persistencia en disco plano para la entidad {@link Order}.
 * <p>
 * Formato actual de cada línea (ver {@link Order#toRegistro()}):
 * {@code id;clienteId;restauranteId;domiciliarioId|-;ESTADO;fechaHora;prodId:cant:precio|...}
 * </p>
 * <p>
 * También lee el formato anterior de un solo producto
 * ({@code id;clienteId;restauranteId;productoId;cantidad;total;estado;domiciliarioId});
 * esas líneas se migran al formato actual la próxima vez que se reescribe el archivo.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class OrderFileDao {
    private static final String RUTA = "data/orders.txt";
    private static final String SIN_DOMICILIARIO = "-";
    private static final String SIN_ASIGNAR_ANTERIOR = "Sin Asignar";

    private final CustomerFileDao customerDao = new CustomerFileDao();
    private final RestaurantFileDao restaurantDao = new RestaurantFileDao();
    private final ProductFileDao productDao = new ProductFileDao();
    private final DriverFileDao driverDao = new DriverFileDao();

    /**
     * Guarda un pedido nuevo al final del archivo de texto.
     *
     * @param order Pedido a registrar.
     * @throws IOException Si falla la escritura en disco.
     */
    public void guardar(Order order) throws IOException {
        List<String> lineas = ArchivoUtil.leerArchivo(RUTA);
        lineas.add(order.toRegistro());
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /**
     * Genera un listado completo de pedidos guardados, reconstruyendo el cliente,
     * restaurante, productos y domiciliario de cada uno. Las líneas cuyo cliente,
     * restaurante o productos ya no existen se omiten.
     *
     * @return Lista de instancias de {@link Order}.
     * @throws IOException Si falla la lectura del archivo.
     */
    public List<Order> listar() throws IOException {
        List<Order> orders = new ArrayList<>();
        for (String linea : ArchivoUtil.leerArchivo(RUTA)) {
            String[] partes = linea.split(";");
            Order o = null;
            if (partes.length == 7) {
                o = desdeFormatoActual(partes);
            } else if (partes.length == 8) {
                o = desdeFormatoAnterior(partes);
            }
            if (o != null) {
                orders.add(o);
            }
        }
        return orders;
    }

    /**
     * Obtiene una orden buscando por su código ID único.
     *
     * @param id Identificador de la orden.
     * @return La orden encontrada o {@code null}.
     * @throws IOException Si falla la lectura del archivo.
     */
    public Order buscarPorId(String id) throws IOException {
        for (Order o : listar()) {
            if (o.getId().equalsIgnoreCase(id)) {
                return o;
            }
        }
        return null;
    }

    /**
     * Sobrescribe el registro de una orden existente para actualizar su estado o domiciliario asignado.
     *
     * @param order Orden con la información modificada.
     * @throws IOException Si no se puede actualizar el archivo plano.
     */
    public void actualizar(Order order) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (Order o : listar()) {
            lineas.add(o.getId().equalsIgnoreCase(order.getId()) ? order.toRegistro() : o.toRegistro());
        }
        ArchivoUtil.guardarArchivo(RUTA, lineas);
    }

    /** Reconstruye un pedido guardado en el formato actual, o {@code null} si faltan referencias. */
    private Order desdeFormatoActual(String[] p) throws IOException {
        Customer cliente = customerDao.buscarPorId(p[1]);
        Restaurant restaurante = restaurantDao.buscarPorId(p[2]);
        if (cliente == null || restaurante == null) {
            return null;
        }
        List<OrderItem> items = new ArrayList<>();
        for (String linea : p[6].split("\\|")) {
            String[] campos = linea.split(":");
            Product producto = campos.length == 3 ? productDao.buscarPorId(campos[0]) : null;
            if (producto == null) {
                return null;
            }
            items.add(new OrderItem(producto, Integer.parseInt(campos[1]), Double.parseDouble(campos[2])));
        }
        return new Order(p[0], cliente, restaurante, cliente.getDireccionPrincipal(), items,
                OrderStatus.desdeTexto(p[4]), LocalDateTime.parse(p[5]), buscarDomiciliario(p[3]),
                new PercentageCommissionPolicy());
    }

    /** Reconstruye un pedido guardado en el formato anterior de un solo producto. */
    private Order desdeFormatoAnterior(String[] p) throws IOException {
        Customer cliente = customerDao.buscarPorId(p[1]);
        Restaurant restaurante = restaurantDao.buscarPorId(p[2]);
        Product producto = productDao.buscarPorId(p[3]);
        if (cliente == null || restaurante == null || producto == null) {
            return null;
        }
        int cantidad = Integer.parseInt(p[4]);
        double precioUnitario = Double.parseDouble(p[5]) / cantidad;
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(producto, cantidad, precioUnitario));
        // El formato anterior no guardaba la fecha; se usa la fecha de migración.
        return new Order(p[0], cliente, restaurante, cliente.getDireccionPrincipal(), items,
                OrderStatus.desdeTexto(p[6]), LocalDateTime.now(), buscarDomiciliario(p[7]),
                new PercentageCommissionPolicy());
    }

    private DeliveryDriver buscarDomiciliario(String id) throws IOException {
        if (SIN_DOMICILIARIO.equals(id) || SIN_ASIGNAR_ANTERIOR.equalsIgnoreCase(id)) {
            return null;
        }
        return driverDao.buscarPorId(id);
    }
}
