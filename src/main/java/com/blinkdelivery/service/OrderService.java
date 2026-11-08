package com.blinkdelivery.service;

import com.blinkdelivery.dao.CustomerFileDao;
import com.blinkdelivery.dao.DriverFileDao;
import com.blinkdelivery.dao.OrderFileDao;
import com.blinkdelivery.dao.ProductFileDao;
import com.blinkdelivery.dao.RestaurantFileDao;
import com.blinkdelivery.modelo.Customer;
import com.blinkdelivery.modelo.DeliveryDriver;
import com.blinkdelivery.modelo.DriverStatus;
import com.blinkdelivery.modelo.Order;
import com.blinkdelivery.modelo.OrderItem;
import com.blinkdelivery.modelo.OrderStatus;
import com.blinkdelivery.modelo.Product;
import com.blinkdelivery.modelo.Restaurant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para la gestión de pedidos.
 * <p>
 * Coordina la creación del pedido, la validación de su ciclo de estados,
 * la asignación de domiciliarios y el cálculo de la comisión de la plataforma.
 * </p>
 */
public class OrderService {

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
     * Crea un pedido nuevo en estado {@link OrderStatus#RECIBIDO} y sin domiciliario asignado.
     * <p>
     * Verifica que el cliente, el restaurante y el producto existan, que el
     * restaurante esté abierto y que el producto pertenezca a ese restaurante
     * y esté disponible. El pedido se entrega en la dirección principal del
     * cliente y congela el precio vigente del producto.
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

        String idPedido = "PED-" + String.format("%04d", orderDao.listar().size() + 1);

        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(producto, cantidad));
        Order pedido = new Order(idPedido, cliente, restaurante, cliente.getDireccionPrincipal(), items);
        orderDao.guardar(pedido);
        return pedido;
    }

    /**
     * Cambia el estado de un pedido validando que la transición esté permitida.
     * <p>
     * Las transiciones válidas las define {@link OrderStatus#puedeCambiarA(OrderStatus)}
     * y las aplica {@link Order#cambiarEstado(OrderStatus)}. Para pasar a
     * {@link OrderStatus#EN_CAMINO} el pedido debe tener domiciliario asignado.
     * Al pasar a un estado final se libera al domiciliario.
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
        Order pedido = obtenerPorId(idPedido);
        pedido.cambiarEstado(OrderStatus.desdeTexto(nuevoEstado));
        orderDao.actualizar(pedido);

        if (pedido.getEstado().esFinal()) {
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

        if (pedido.getEstado() != OrderStatus.LISTO_PARA_RECOGER) {
            throw new IllegalStateException("Solo se puede asignar domiciliario a un pedido '"
                    + OrderStatus.LISTO_PARA_RECOGER + "'. Estado actual: '" + pedido.getEstado() + "'.");
        }
        if (pedido.tieneDomiciliario()) {
            throw new IllegalStateException("El pedido ya tiene un domiciliario asignado.");
        }

        DeliveryDriver driver = buscarDomiciliario(texto(idDomiciliario, "El id del domiciliario"));
        if (driver == null) {
            throw new IllegalArgumentException("No existe un domiciliario con la identificación " + idDomiciliario + ".");
        }
        if (!driver.isAvailable()) {
            throw new IllegalStateException("El domiciliario '" + driver.getName() + "' no está disponible.");
        }

        pedido.asignarDomiciliario(driver);
        driver.setEstado(DriverStatus.EN_ENTREGA);
        driverDao.actualizar(driver);
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
     * @return valor de la comisión según la {@link com.blinkdelivery.modelo.CommissionPolicy} del pedido
     * @throws IOException              si ocurre un error al leer el archivo
     * @throws IllegalArgumentException si no existe el pedido
     */
    public double calcularComision(String idPedido) throws IOException {
        return obtenerPorId(idPedido).getComisionPlataforma();
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
        return obtenerPorId(idPedido).getNetoRestaurante();
    }

    /**
     * Marca como disponible al domiciliario asignado a un pedido, si tiene uno.
     *
     * @param pedido pedido del que se libera el domiciliario
     * @throws IOException si ocurre un error al leer o escribir el archivo
     */
    private void liberarDomiciliario(Order pedido) throws IOException {
        if (!pedido.tieneDomiciliario()) {
            return;
        }
        DeliveryDriver driver = buscarDomiciliario(pedido.getDomiciliario().getId());
        if (driver != null) {
            driver.setEstado(DriverStatus.DISPONIBLE);
            driverDao.actualizar(driver);
        }
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