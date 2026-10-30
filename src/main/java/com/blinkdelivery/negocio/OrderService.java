package com.blinkdelivery.negocio;

import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.dao.OrderFileDao;
import com.blinkdelivery.dao.ProductFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para la gestión de pedidos.
 * <p>
 * Coordina la creación del pedido, la validación de su ciclo de estados,
 * la asignación de domiciliarios y el cálculo de la comisión de la plataforma.
 * </p>
 */
public class OrderService {

    /** Estado inicial de todo pedido recién creado. */
    public static final String CREADO = "Creado";

    /** Estado del pedido una vez confirmado. */
    public static final String CONFIRMADO = "Confirmado";

    /** Estado del pedido mientras el restaurante lo prepara. */
    public static final String EN_PREPARACION = "En preparación";

    /** Estado del pedido cuando está listo para que lo recoja el domiciliario. */
    public static final String LISTO = "Listo para recoger";

    /** Estado del pedido mientras el domiciliario lo lleva al cliente. */
    public static final String EN_CAMINO = "En camino";

    /** Estado final del pedido cuando fue entregado al cliente. */
    public static final String ENTREGADO = "Entregado";

    /** Estado final del pedido cuando fue cancelado. */
    public static final String CANCELADO = "Cancelado";

    /** Valor que usa la clase {@link Order} cuando aún no tiene domiciliario. */
    public static final String SIN_ASIGNAR = "Sin Asignar";

    /** Porcentaje de comisión que cobra la plataforma sobre el total del pedido (10 %). */
    public static final double TASA_COMISION = 0.10;

    /** Transiciones de estado permitidas: estado actual y estados a los que puede pasar. */
    private static final Map<String, List<String>> TRANSICIONES = Map.of(
            CREADO, List.of(CONFIRMADO, CANCELADO),
            CONFIRMADO, List.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, List.of(LISTO, CANCELADO),
            LISTO, List.of(EN_CAMINO, CANCELADO),
            EN_CAMINO, List.of(ENTREGADO),
            ENTREGADO, List.of(),
            CANCELADO, List.of()
    );

    /** DAO que guarda y lee los pedidos en el archivo de texto. */
    private final OrderFileDao orderDao = new OrderFileDao();

    /** DAO usado para verificar la existencia del cliente. */
    private final CustomerFileDao customerDao = new CustomerFileDao();

    /** DAO usado para verificar la existencia y el estado del restaurante. */
    private final RestaurantFileDao restaurantDao = new RestaurantFileDao();

    /** DAO usado para verificar la existencia y disponibilidad del producto. */
    private final ProductFileDao productDao = new ProductFileDao();

    /** DAO usado para consultar y actualizar a los domiciliarios. */
    private final DriverFileDao driverDao = new DriverFileDao();

    /**
     * Crea un pedido nuevo en estado {@link #CREADO} y sin domiciliario asignado.
     * <p>
     * Verifica que el cliente, el restaurante y el producto existan, que el
     * restaurante esté abierto y que el producto pertenezca a ese restaurante
     * y esté disponible. El total se calcula como precio por cantidad.
     * </p>
     *
     * @param idCliente     identificación del cliente que hace el pedido
     * @param idRestaurante identificador del restaurante
     * @param idProducto    identificador del producto pedido
     * @param cantidad      unidades del producto (mayor que cero)
     * @return el pedido creado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si algún dato es inválido o no existe el cliente, restaurante o producto
     * @throws IllegalStateException    si el restaurante está cerrado o el producto no está disponible
     *                                  ni pertenece al restaurante
     */
    public Order crearPedido(String idCliente, String idRestaurante,
                             String idProducto, int cantidad) throws IOException {
        idCliente = texto(idCliente, "El id del cliente");
        idRestaurante = texto(idRestaurante, "El id del restaurante");
        idProducto = texto(idProducto, "El id del producto");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        Customer cliente = buscarCliente(idCliente);
        if (cliente == null) {
            throw new IllegalArgumentException("No existe un cliente con la identificación " + idCliente + ".");
        }

        Restaurant restaurante = buscarRestaurante(idRestaurante);
        if (restaurante == null) {
            throw new IllegalArgumentException("No existe un restaurante con el id " + idRestaurante + ".");
        }
        if (!restaurante.isOpen()) {
            throw new IllegalStateException("El restaurante '" + restaurante.getName() + "' está cerrado.");
        }

        Product producto = buscarProducto(idProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe un producto con el id " + idProducto + ".");
        }
        if (!producto.getRestaurantId().equals(restaurante.getId())) {
            throw new IllegalStateException("El producto no pertenece al restaurante seleccionado.");
        }
        if (!producto.isAvailable()) {
            throw new IllegalStateException("El producto '" + producto.getName() + "' no está disponible.");
        }

        double total = producto.getPrice() * cantidad;
        String idPedido = "PED-" + String.format("%04d", orderDao.listar().size() + 1);

        Order pedido = new Order(idPedido, cliente.getIdentification(), restaurante.getId(),
                producto.getId(), cantidad, total);
        orderDao.guardar(pedido);
        return pedido;
    }

    /**
     * Cambia el estado de un pedido validando que la transición esté permitida.
     * <p>
     * Para pasar a {@link #EN_CAMINO} el pedido debe tener domiciliario asignado.
     * Al pasar a {@link #ENTREGADO} o {@link #CANCELADO} se libera al domiciliario.
     * </p>
     *
     * @param idPedido    identificador del pedido
     * @param nuevoEstado estado destino (no distingue mayúsculas de minúsculas)
     * @return el pedido con el estado actualizado
     * @throws IOException              si ocurre un error al leer o escribir el archivo
     * @throws IllegalArgumentException si el pedido no existe o el estado es inválido
     * @throws IllegalStateException    si la transición no está permitida o falta el domiciliario
     */
    public Order cambiarEstado(String idPedido, String nuevoEstado) throws IOException {
        Order pedido = obtenerPorId(idPedido);
        String destino = normalizarEstado(nuevoEstado);

        List<String> permitidos = TRANSICIONES.get(pedido.getStatus());
        if (permitidos == null || !permitidos.contains(destino)) {
            throw new IllegalStateException("No se puede pasar de '" + pedido.getStatus()
                    + "' a '" + destino + "'.");
        }
        if (destino.equals(EN_CAMINO) && SIN_ASIGNAR.equals(pedido.getDriverId())) {
            throw new IllegalStateException("Debe asignar un domiciliario antes de poner el pedido En camino.");
        }

        pedido.setStatus(destino);
        orderDao.actualizar(pedido);

        if (destino.equals(ENTREGADO) || destino.equals(CANCELADO)) {
            liberarDomiciliario(pedido);
        }
        return pedido;
    }

    /**
     * Asigna un domiciliario a un pedido que está listo para recoger.
     * El domiciliario queda no disponible mientras atiende el pedido.
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

        if (!LISTO.equals(pedido.getStatus())) {
            throw new IllegalStateException("Solo se puede asignar domiciliario a un pedido '" + LISTO
                    + "'. Estado actual: '" + pedido.getStatus() + "'.");
        }
        if (!SIN_ASIGNAR.equals(pedido.getDriverId())) {
            throw new IllegalStateException("El pedido ya tiene un domiciliario asignado.");
        }

        DeliveryDriver driver = buscarDomiciliario(texto(idDomiciliario, "El id del domiciliario"));
        if (driver == null) {
            throw new IllegalArgumentException("No existe un domiciliario con la identificación " + idDomiciliario + ".");
        }
        if (!driver.isAvailable()) {
            throw new IllegalStateException("El domiciliario '" + driver.getName() + "' no está disponible.");
        }

        driver.setAvailable(false);
        driverDao.actualizar(driver);

        pedido.setDriverId(driver.getIdentification());
        orderDao.actualizar(pedido);
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
        for (DeliveryDriver d : driverDao.listar()) {
            if (d.isAvailable()) {
                return asignarDomiciliario(idPedido, d.getIdentification());
            }
        }
        throw new IllegalStateException("No hay domiciliarios disponibles en este momento.");
    }

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
     * Lista los pedidos de un cliente.
     *
     * @param idCliente identificación del cliente
     * @return lista de pedidos del cliente (vacía si no tiene ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el id es vacío
     */
    public List<Order> listarPorCliente(String idCliente) throws IOException {
        String id = texto(idCliente, "El id del cliente");
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
        String id = texto(idRestaurante, "El id del restaurante");
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.getRestaurantId().equals(id)) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    /**
     * Lista los pedidos que se encuentran en un estado determinado.
     *
     * @param estado estado a filtrar (no distingue mayúsculas de minúsculas)
     * @return lista de pedidos en ese estado (vacía si no hay ninguno)
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si el estado es inválido
     */
    public List<Order> listarPorEstado(String estado) throws IOException {
        String buscado = normalizarEstado(estado);
        List<Order> resultado = new ArrayList<>();
        for (Order o : orderDao.listar()) {
            if (o.getStatus().equals(buscado)) {
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
        if (idPedido == null) {
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

    /**
     * Calcula la comisión que cobra la plataforma por un pedido.
     *
     * @param idPedido identificador del pedido
     * @return valor de la comisión (total del pedido por {@link #TASA_COMISION})
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el pedido
     */
    public double calcularComision(String idPedido) throws IOException {
        return obtenerPorId(idPedido).getTotal() * TASA_COMISION;
    }

    /**
     * Calcula el valor neto que recibe el restaurante después de descontar la comisión.
     *
     * @param idPedido identificador del pedido
     * @return total del pedido menos la comisión
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el pedido
     */
    public double calcularNetoRestaurante(String idPedido) throws IOException {
        Order pedido = obtenerPorId(idPedido);
        return pedido.getTotal() - pedido.getTotal() * TASA_COMISION;
    }

    /**
     * Marca como disponible al domiciliario asignado a un pedido, si tiene uno.
     *
     * @param pedido pedido del que se libera el domiciliario
     * @throws IOException si ocurre un error al leer o escribir el archivo
     */
    private void liberarDomiciliario(Order pedido) throws IOException {
        if (SIN_ASIGNAR.equals(pedido.getDriverId())) {
            return;
        }
        DeliveryDriver driver = buscarDomiciliario(pedido.getDriverId());
        if (driver != null) {
            driver.setAvailable(true);
            driverDao.actualizar(driver);
        }
    }

    /**
     * Convierte el texto de un estado a su valor oficial, sin distinguir
     * mayúsculas de minúsculas.
     *
     * @param estado texto del estado
     * @return el estado con su escritura oficial
     * @throws IllegalArgumentException si el estado es vacío o no existe
     */
    private String normalizarEstado(String estado) {
        String buscado = texto(estado, "El estado");
        for (String valido : TRANSICIONES.keySet()) {
            if (valido.equalsIgnoreCase(buscado)) {
                return valido;
            }
        }
        throw new IllegalArgumentException("Estado inválido: '" + estado + "'.");
    }

    /**
     * Busca un cliente por su identificación.
     *
     * @param id identificación del cliente
     * @return el cliente, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    private Customer buscarCliente(String id) throws IOException {
        for (Customer c : customerDao.listar()) {
            if (c.getIdentification().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Busca un restaurante por su identificador.
     *
     * @param id identificador del restaurante
     * @return el restaurante, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    private Restaurant buscarRestaurante(String id) throws IOException {
        for (Restaurant r : restaurantDao.listar()) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Busca un producto por su identificador.
     *
     * @param id identificador del producto
     * @return el producto, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    private Product buscarProducto(String id) throws IOException {
        for (Product p : productDao.listar()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Busca un domiciliario por su identificación.
     *
     * @param id identificación del domiciliario
     * @return el domiciliario, o {@code null} si no existe
     * @throws IOException si ocurre un error al leer el archivo
     */
    private DeliveryDriver buscarDomiciliario(String id) throws IOException {
        for (DeliveryDriver d : driverDao.listar()) {
            if (d.getIdentification().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Valida que un texto no sea nulo ni vacío.
     *
     * @param valor texto a validar
     * @param campo nombre del campo, usado en el mensaje de error
     * @return el texto sin espacios al inicio ni al final
     * @throws IllegalArgumentException si el texto es nulo o vacío
     */
    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacío.");
        }
        return valor.trim();
    }
}