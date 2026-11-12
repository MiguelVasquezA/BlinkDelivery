package com.blinkdelivery.service;

import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.dao.OrderFileDao;
import com.blinkdelivery.dao.OrderStatusHistoryFileDao;
import com.blinkdelivery.modelo.Address;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.DriverStatus;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.OrderStatusChange;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para la gestión de pedidos.
 * <p>
 * Reglas que aplica:
 * </p>
 * <ul>
 *   <li>SP01 / RN07 / RN08: el pedido pertenece a un único cliente registrado.</li>
 *   <li>SP04: el pedido tiene al menos un producto.</li>
 *   <li>RN09: todos los productos son del mismo restaurante, que debe estar abierto (RN15)
 *       y los productos disponibles (RN16).</li>
 *   <li>RN17 / RN27 / SP05: la dirección de entrega está registrada por el mismo cliente.</li>
 *   <li>RN18: cada cambio de estado del ciclo de vida queda registrado en el historial.</li>
 *   <li>RN10 / RN11: como máximo un domiciliario por pedido, que debe estar disponible.</li>
 *   <li>RN20: la plataforma cobra el 10 % de comisión sobre cada pedido entregado.</li>
 * </ul>
 */
public class OrderService {

    /** Prefijo de los identificadores de pedido. */
    private static final String PREFIJO_ID = "PED-";

    /** DAO que guarda y lee los pedidos en el archivo de texto. */
    private final OrderFileDao orderDao = new OrderFileDao();

    /** DAO del historial de estados de los pedidos. */
    private final OrderStatusHistoryFileDao historialDao = new OrderStatusHistoryFileDao();

    /** DAO usado para actualizar el estado de los domiciliarios. */
    private final DriverFileDao driverDao = new DriverFileDao();

    private final CustomerService customerService = new CustomerService();
    private final RestaurantService restaurantService = new RestaurantService();
    private final ProductService productService = new ProductService();
    private final DeliveryDriverService driverService = new DeliveryDriverService();

    // ---------------------------------------------------------------- Creación

    /**
     * Crea un pedido de un solo producto, entregado en la dirección principal del cliente.
     *
     * @param idCliente     identificación del cliente que hace el pedido
     * @param idRestaurante identificador del restaurante
     * @param idProducto    identificador del producto pedido
     * @param cantidad      unidades del producto (mayor que cero)
     * @return el pedido creado, en estado {@link OrderStatus#RECIBIDO}
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es inválido o no existe el cliente, restaurante o producto
     * @throws IllegalStateException    si el restaurante está cerrado o el producto no está disponible
     *                                  ni pertenece al restaurante
     * @see #crearPedido(String, String, String, Map)
     */
    public Order crearPedido(String idCliente, String idRestaurante,
                             String idProducto, int cantidad) throws IOException {
        Map<String, Integer> productos = new LinkedHashMap<>();
        productos.put(ServiceValidations.texto(idProducto, "El id del producto"), cantidad);
        return crearPedido(idCliente, idRestaurante, null, productos);
    }

    /**
     * Crea un pedido con uno o varios productos de un mismo restaurante.
     * <p>
     * El pedido queda en estado {@link OrderStatus#RECIBIDO}, sin domiciliario y con
     * la comisión del 10 % (RN20). Cada línea congela el precio vigente del producto.
     * Si un producto aparece repetido, sus cantidades se suman.
     * </p>
     *
     * @param idCliente     identificación del cliente (debe estar registrado, SP01)
     * @param idRestaurante restaurante al que se hace el pedido
     * @param idDireccion   dirección de entrega registrada por el cliente; si es
     *                      {@code null} o vacía se usa su dirección principal
     * @param productos     mapa id de producto → cantidad (al menos un producto)
     * @return el pedido creado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es inválido, no existe alguna entidad
     *                                  o la dirección no pertenece al cliente (RN27)
     * @throws IllegalStateException    si el restaurante está cerrado, algún producto no está
     *                                  disponible o es de otro restaurante (RN09)
     */
    public Order crearPedido(String idCliente, String idRestaurante, String idDireccion,
                             Map<String, Integer> productos) throws IOException {
        Customer cliente = customerService.obtenerPorId(ServiceValidations.texto(idCliente, "El id del cliente"));
        Restaurant restaurante = restaurantService.obtenerPorId(
                ServiceValidations.texto(idRestaurante, "El id del restaurante"));
        if (!restaurante.estaAbierto()) {
            throw new IllegalStateException("El restaurante '" + restaurante.getNombre() + "' está cerrado.");
        }

        Address direccion = resolverDireccion(cliente, idDireccion);

        if (productos == null || productos.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto (SP04).");
        }
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : productos.entrySet()) {
            String idProducto = ServiceValidations.texto(e.getKey(), "El id del producto");
            Integer cantidad = e.getValue();
            if (cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad del producto " + idProducto + " debe ser mayor que cero.");
            }
            cantidades.merge(idProducto, cantidad, Integer::sum);
        }

        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cantidades.entrySet()) {
            Product producto = productService.obtenerDeRestaurante(e.getKey(), restaurante.getId());
            if (!producto.isDisponible()) {
                throw new IllegalStateException("El producto '" + producto.getNombre() + "' no está disponible.");
            }
            items.add(new OrderItem(producto, e.getValue()));
        }

        Order pedido = new Order(siguienteId(), cliente, restaurante, direccion, items);
        orderDao.guardar(pedido);
        registrarHistorial(pedido);
        return pedido;
    }

    // ---------------------------------------------------------------- Ciclo de vida

    /**
     * Cambia el estado de un pedido validando que la transición esté permitida
     * y lo registra en el historial (RN18).
     * <p>
     * Las transiciones válidas las define {@link OrderStatus#puedeCambiarA(OrderStatus)}.
     * Para pasar a {@link OrderStatus#EN_CAMINO} el pedido debe tener domiciliario.
     * Al llegar a un estado final se libera al domiciliario.
     * </p>
     *
     * @param idPedido    identificador del pedido
     * @param nuevoEstado estado destino (nombre o descripción, sin distinguir mayúsculas ni tildes)
     * @return el pedido con el estado actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el pedido no existe o el estado es inválido
     * @throws IllegalStateException    si la transición no está permitida o falta el domiciliario
     */
    public Order cambiarEstado(String idPedido, String nuevoEstado) throws IOException {
        return cambiarEstado(idPedido, OrderStatus.desdeTexto(nuevoEstado));
    }

    /**
     * Variante tipada de {@link #cambiarEstado(String, String)}.
     *
     * @param idPedido    identificador del pedido
     * @param nuevoEstado estado destino
     * @return el pedido con el estado actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el pedido no existe o el estado es nulo
     * @throws IllegalStateException    si la transición no está permitida o falta el domiciliario
     */
    public Order cambiarEstado(String idPedido, OrderStatus nuevoEstado) throws IOException {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo.");
        }
        Order pedido = obtenerPorId(idPedido);
        pedido.cambiarEstado(nuevoEstado);
        orderDao.actualizar(pedido);
        registrarHistorial(pedido);

        if (pedido.getEstado().esFinal()) {
            liberarDomiciliario(pedido);
        }
        return pedido;
    }

    /**
     * Cancela un pedido que aún no ha salido a entrega.
     *
     * @param idPedido identificador del pedido
     * @return el pedido cancelado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el pedido no existe
     * @throws IllegalStateException    si ya está en camino o finalizado
     */
    public Order cancelarPedido(String idPedido) throws IOException {
        return cambiarEstado(idPedido, OrderStatus.CANCELADO);
    }

    /**
     * Consulta el estado actual de un pedido (RF09).
     *
     * @param idPedido identificador del pedido
     * @return estado actual
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el pedido no existe
     */
    public OrderStatus consultarEstado(String idPedido) throws IOException {
        return obtenerPorId(idPedido).getEstado();
    }

    /**
     * Lista los cambios de estado registrados para un pedido (RN18).
     *
     * @param idPedido identificador del pedido
     * @return historial en orden cronológico (los pedidos anteriores a esta
     *         funcionalidad pueden no tener historial)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el pedido no existe
     */
    public List<OrderStatusChange> historialEstados(String idPedido) throws IOException {
        return historialDao.listarPorPedido(obtenerPorId(idPedido).getId());
    }

    // ---------------------------------------------------------------- Domiciliarios

    /**
     * Asigna un domiciliario a un pedido que está listo para recoger (RN10).
     * El domiciliario queda {@link DriverStatus#EN_ENTREGA} mientras atiende el pedido.
     *
     * @param idPedido       identificador del pedido
     * @param idDomiciliario identificación del domiciliario a asignar
     * @return el pedido con el domiciliario asignado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si no existe el pedido o el domiciliario
     * @throws IllegalStateException    si el pedido no está listo para recoger, ya tiene
     *                                  domiciliario o el domiciliario no está disponible
     */
    public Order asignarDomiciliario(String idPedido, String idDomiciliario) throws IOException {
        Order pedido = obtenerPorId(idPedido);

        if (pedido.getEstado() != OrderStatus.LISTO_PARA_RECOGER) {
            throw new IllegalStateException("Solo se puede asignar domiciliario a un pedido '"
                    + OrderStatus.LISTO_PARA_RECOGER + "'. Estado actual: '" + pedido.getEstado() + "'.");
        }
        if (pedido.tieneDomiciliario()) {
            throw new IllegalStateException("El pedido ya tiene un domiciliario asignado (RN10).");
        }

        DeliveryDriver driver = driverService.obtenerPorId(
                ServiceValidations.texto(idDomiciliario, "El id del domiciliario"));
        if (!driver.estaDisponible()) {
            throw new IllegalStateException("El domiciliario '" + driver.getNombre() + "' no está disponible ("
                    + driver.getEstado() + ").");
        }

        pedido.asignarDomiciliario(driver);
        driver.setEstado(DriverStatus.EN_ENTREGA);
        orderDao.actualizar(pedido);
        driverDao.actualizar(driver);
        return pedido;
    }

    /**
     * Asigna a un pedido el primer domiciliario disponible.
     *
     * @param idPedido identificador del pedido
     * @return el pedido con el domiciliario asignado
     * @throws IOException           si ocurre un error al leer o escribir el archivo
     * @throws IllegalStateException si no hay domiciliarios disponibles o el pedido
     *                               no puede recibir domiciliario
     */
    public Order asignarDomiciliarioAutomatico(String idPedido) throws IOException {
        DeliveryDriver disponible = driverService.obtenerPrimerDisponible();
        if (disponible == null) {
            throw new IllegalStateException("No hay domiciliarios disponibles en este momento.");
        }
        return asignarDomiciliario(idPedido, disponible.getId());
    }

    // ---------------------------------------------------------------- Consultas

    /**
     * Lista todos los pedidos registrados.
     *
     * @return lista de pedidos (vacía si no hay ninguno)
     * @throws IOException si ocurre un error al leer el archivo
     */
    public List<Order> listarPedidos() throws IOException {
        return orderDao.listar();
    }

    /**
     * Historial de pedidos de un cliente (RF12).
     *
     * @param idCliente identificación del cliente
     * @return lista de pedidos del cliente (vacía si no tiene ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public List<Order> listarPorCliente(String idCliente) throws IOException {
        String id = ServiceValidations.texto(idCliente, "El id del cliente");
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.getCustomerId().equals(id)) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * Lista los pedidos recibidos por un restaurante.
     *
     * @param idRestaurante identificador del restaurante
     * @return lista de pedidos del restaurante (vacía si no tiene ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public List<Order> listarPorRestaurante(String idRestaurante) throws IOException {
        String id = ServiceValidations.texto(idRestaurante, "El id del restaurante");
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.getRestaurantId().equals(id)) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * Lista los pedidos asignados a un domiciliario (RN11).
     *
     * @param idDomiciliario identificación del domiciliario
     * @return lista de pedidos del domiciliario (vacía si no tiene)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public List<Order> listarPorDomiciliario(String idDomiciliario) throws IOException {
        String id = ServiceValidations.texto(idDomiciliario, "El id del domiciliario");
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.tieneDomiciliario() && o.getDomiciliario().getId().equals(id)) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * Lista los pedidos que se encuentran en un estado determinado.
     *
     * @param estado estado a filtrar (nombre o descripción, sin distinguir mayúsculas ni tildes)
     * @return lista de pedidos en ese estado (vacía si no hay ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el estado es inválido
     */
    public List<Order> listarPorEstado(String estado) throws IOException {
        OrderStatus buscado = OrderStatus.desdeTexto(estado);
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.getEstado() == buscado) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * Pedidos entregados de un cliente en un restaurante: base de RN12 para calificar.
     *
     * @param idCliente     cliente
     * @param idRestaurante restaurante
     * @return pedidos entregados (vacía si no hay)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si algún id es vacío
     */
    public List<Order> listarEntregados(String idCliente, String idRestaurante) throws IOException {
        String rest = ServiceValidations.texto(idRestaurante, "El id del restaurante");
        List<Order> resultado = new ArrayList<>();
        for (Order o : listarPorCliente(idCliente)) {
            if (o.getRestaurantId().equals(rest) && o.getEstado() == OrderStatus.ENTREGADO) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * Busca un pedido por su identificador.
     *
     * @param idPedido identificador a buscar
     * @return el pedido encontrado, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    public Order buscarPorId(String idPedido) throws IOException {
        if (ServiceValidations.vacio(idPedido)) {
            return null;
        }
        String id = idPedido.trim();
        for (Order o : orderDao.listar()) {
            if (o.getId().equals(id)) {
                return o;
            }
        }
        return null;
    }

    /**
     * Obtiene un pedido por su identificador y falla si no existe.
     *
     * @param idPedido identificador a buscar
     * @return el pedido encontrado
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe un pedido con ese id
     */
    public Order obtenerPorId(String idPedido) throws IOException {
        Order pedido = buscarPorId(idPedido);
        if (pedido == null) {
            throw new IllegalArgumentException("No existe un pedido con el id " + idPedido + ".");
        }
        return pedido;
    }

    // ---------------------------------------------------------------- Comisiones (RN20)

    /**
     * Comisión que cobra la plataforma por un pedido (RN20): el 10 % del total,
     * solo si el pedido fue entregado. Los pedidos en curso o cancelados no
     * generan comisión.
     *
     * @param idPedido identificador del pedido
     * @return valor de la comisión, o {@code 0} si el pedido no está entregado
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el pedido
     */
    public double calcularComision(String idPedido) throws IOException {
        return comision(obtenerPorId(idPedido));
    }

    /**
     * Valor neto que recibe el restaurante: total del pedido menos la comisión.
     *
     * @param idPedido identificador del pedido
     * @return total menos comisión (sin descuento si el pedido no está entregado)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el pedido
     */
    public double calcularNetoRestaurante(String idPedido) throws IOException {
        Order pedido = obtenerPorId(idPedido);
        return pedido.getTotal() - comision(pedido);
    }

    /**
     * Suma de las comisiones de todos los pedidos entregados (RN20).
     *
     * @return total de comisiones de la plataforma
     * @throws IOException si ocurre un error al leer el archivo
     */
    public double calcularTotalComisiones() throws IOException {
        double total = 0;
        for (Order o : orderDao.listar()) {
            total += comision(o);
        }
        return total;
    }

    /**
     * Suma de las comisiones de los pedidos entregados de un restaurante (RN20).
     *
     * @param idRestaurante restaurante
     * @return total de comisiones cobradas al restaurante
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public double calcularComisionesPorRestaurante(String idRestaurante) throws IOException {
        double total = 0;
        for (Order o : listarPorRestaurante(idRestaurante)) {
            total += comision(o);
        }
        return total;
    }

    // ---------------------------------------------------------------- Auxiliares

    private static double comision(Order pedido) {
        return pedido.getEstado() == OrderStatus.ENTREGADO ? pedido.getComisionPlataforma() : 0.0;
    }

    /** Dirección de entrega: la indicada (validando que sea del cliente, RN27) o la principal. */
    private static Address resolverDireccion(Customer cliente, String idDireccion) {
        if (ServiceValidations.vacio(idDireccion)) {
            return cliente.getDireccionPrincipal();
        }
        String id = idDireccion.trim();
        if (!cliente.getDirecciones().stream().anyMatch(a -> a.getId().equals(id))) {
            throw new IllegalArgumentException("La dirección " + id + " no está registrada por el cliente "
                    + cliente.getId() + " (RN27).");
        }
        return cliente.obtenerDireccion(id);
    }

    /** Registra el estado actual del pedido en el historial (RN18). */
    private void registrarHistorial(Order pedido) throws IOException {
        historialDao.guardar(new OrderStatusChange(pedido.getId(), pedido.getEstado(), LocalDateTime.now()));
    }

    /** Marca como disponible al domiciliario asignado a un pedido, si tiene uno. */
    private void liberarDomiciliario(Order pedido) throws IOException {
        if (!pedido.tieneDomiciliario()) {
            return;
        }
        DeliveryDriver driver = driverService.buscarPorId(pedido.getDomiciliario().getId());
        if (driver != null && driver.getEstado() == DriverStatus.EN_ENTREGA) {
            driver.setEstado(DriverStatus.DISPONIBLE);
            driverDao.actualizar(driver);
        }
    }

    /** Genera el siguiente id {@code PED-NNNN} a partir del mayor existente (SP08). */
    private String siguienteId() throws IOException {
        int max = 0;
        for (Order o : orderDao.listar()) {
            String id = o.getId();
            if (id.startsWith(PREFIJO_ID) && id.substring(PREFIJO_ID.length()).matches("\\d+")) {
                max = Math.max(max, Integer.parseInt(id.substring(PREFIJO_ID.length())));
            }
        }
        return PREFIJO_ID + String.format("%04d", max + 1);
    }
}
