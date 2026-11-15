package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pedido que un {@link Customer} hace a un {@link Restaurant}.
 * <p>
 * Es el experto en su propia liquidación (GRASP Information Expert): calcula su
 * subtotal a partir de sus {@link OrderItem}, delega la comisión en una
 * {@link CommissionPolicy} intercambiable (patrón Strategy) y controla las
 * transiciones de su ciclo de vida con {@link OrderStatus}.
 * </p>
 * <p>
 * Liquidación: el cliente paga el {@link #getTotal() total} (igual al subtotal de
 * los productos); la plataforma descuenta su {@link #getComisionPlataforma() comisión}
 * y el restaurante recibe el {@link #getNetoRestaurante() neto}.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String id;
    private final Customer cliente;
    private final Restaurant restaurante;
    private Address direccionEntrega;
    private DeliveryDriver domiciliario;
    private final ArrayList<OrderItem> items = new ArrayList<>();
    private OrderStatus estado;
    private final LocalDateTime fechaHora;
    private CommissionPolicy politicaComision;

    /**
     * Crea un pedido nuevo en estado {@link OrderStatus#RECIBIDO}, con la fecha y
     * hora actuales y la comisión por defecto del 10 %.
     *
     * @param id               Identificador único (obligatorio).
     * @param cliente          Cliente que hace el pedido (obligatorio).
     * @param restaurante      Restaurante que lo prepara (obligatorio).
     * @param direccionEntrega Dirección registrada por el cliente (obligatoria).
     * @param items            Líneas del pedido (al menos una, todas del mismo restaurante).
     * @throws IllegalArgumentException Si algún dato es nulo, vacío o inconsistente.
     */
    public Order(String id, Customer cliente, Restaurant restaurante, Address direccionEntrega,
                 List<OrderItem> items) {
        this(id, cliente, restaurante, direccionEntrega, items, new PercentageCommissionPolicy());
    }

    /**
     * Crea un pedido nuevo en estado {@link OrderStatus#RECIBIDO} con una política
     * de comisión específica.
     *
     * @param id               Identificador único (obligatorio).
     * @param cliente          Cliente que hace el pedido (obligatorio).
     * @param restaurante      Restaurante que lo prepara (obligatorio).
     * @param direccionEntrega Dirección registrada por el cliente (obligatoria).
     * @param items            Líneas del pedido (al menos una, todas del mismo restaurante).
     * @param politicaComision Estrategia de comisión (obligatoria).
     * @throws IllegalArgumentException Si algún dato es nulo, vacío o inconsistente.
     */
    public Order(String id, Customer cliente, Restaurant restaurante, Address direccionEntrega,
                 List<OrderItem> items, CommissionPolicy politicaComision) {
        this(id, cliente, restaurante, direccionEntrega, items, OrderStatus.RECIBIDO,
                LocalDateTime.now(), null, politicaComision);
    }

    /**
     * Reconstruye un pedido existente con todo su estado (por ejemplo, al cargarlo desde disco).
     *
     * @param id               Identificador único (obligatorio).
     * @param cliente          Cliente que hizo el pedido (obligatorio).
     * @param restaurante      Restaurante que lo prepara (obligatorio).
     * @param direccionEntrega Dirección registrada por el cliente (obligatoria).
     * @param items            Líneas del pedido (al menos una, todas del mismo restaurante).
     * @param estado           Estado actual (obligatorio).
     * @param fechaHora        Fecha y hora de creación (obligatoria).
     * @param domiciliario     Domiciliario asignado, o {@code null} si aún no tiene.
     * @param politicaComision Estrategia de comisión (obligatoria).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o inconsistente.
     */
    public Order(String id, Customer cliente, Restaurant restaurante, Address direccionEntrega,
                 List<OrderItem> items, OrderStatus estado, LocalDateTime fechaHora,
                 DeliveryDriver domiciliario, CommissionPolicy politicaComision) {
        this.id = Validaciones.texto(id, "El id del pedido");
        this.cliente = Validaciones.noNulo(cliente, "El cliente del pedido");
        this.restaurante = Validaciones.noNulo(restaurante, "El restaurante del pedido");
        this.direccionEntrega = validarDireccion(direccionEntrega);
        Validaciones.noNulo(items, "La lista de productos del pedido");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe tener al menos un producto.");
        }
        for (OrderItem item : items) {
            this.items.add(validarItem(item));
        }
        this.estado = Validaciones.noNulo(estado, "El estado del pedido");
        this.fechaHora = Validaciones.noNulo(fechaHora, "La fecha del pedido");
        this.domiciliario = domiciliario;
        this.politicaComision = Validaciones.noNulo(politicaComision, "La política de comisión");
    }

    // ---- Datos del pedido ----

    /** @return Identificador del pedido. */
    public String getId() {
        return id;
    }

    /** @return Cliente que hizo el pedido. */
    public Customer getCliente() {
        return cliente;
    }

    /** @return Restaurante que prepara el pedido. */
    public Restaurant getRestaurante() {
        return restaurante;
    }

    /** @return Dirección de entrega. */
    public Address getDireccionEntrega() {
        return direccionEntrega;
    }

    /**
     * Cambia la dirección de entrega mientras el pedido no haya sido aceptado.
     *
     * @param direccionEntrega Nueva dirección, registrada por el mismo cliente.
     * @throws IllegalArgumentException Si es nula o no pertenece al cliente.
     * @throws IllegalStateException    Si el pedido ya no está en estado {@link OrderStatus#RECIBIDO}.
     */
    public final void setDireccionEntrega(Address direccionEntrega) {
        exigirEstado(OrderStatus.RECIBIDO, "cambiar la dirección de entrega");
        this.direccionEntrega = validarDireccion(direccionEntrega);
    }

    /** @return Domiciliario asignado, o {@code null} si aún no tiene. */
    public DeliveryDriver getDomiciliario() {
        return domiciliario;
    }

    /** @return {@code true} si el pedido ya tiene domiciliario asignado. */
    public boolean tieneDomiciliario() {
        return domiciliario != null;
    }

    /**
     * Asigna el domiciliario que llevará el pedido.
     *
     * @param domiciliario Domiciliario a asignar.
     * @throws IllegalArgumentException Si es nulo.
     * @throws IllegalStateException    Si el pedido ya tiene domiciliario, ya salió a entrega o está finalizado.
     */
    public void asignarDomiciliario(DeliveryDriver domiciliario) {
        Validaciones.noNulo(domiciliario, "El domiciliario");
        if (this.domiciliario != null) {
            throw new IllegalStateException("El pedido ya tiene un domiciliario asignado.");
        }
        if (estado == OrderStatus.EN_CAMINO || estado.esFinal()) {
            throw new IllegalStateException("No se puede asignar domiciliario a un pedido en estado '" + estado + "'.");
        }
        this.domiciliario = domiciliario;
    }

    /** @return Vista de solo lectura de las líneas del pedido. */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Agrega una línea al pedido mientras no haya sido aceptado.
     *
     * @param item Línea a agregar.
     * @throws IllegalArgumentException Si es nula o el producto es de otro restaurante.
     * @throws IllegalStateException    Si el pedido ya no está en estado {@link OrderStatus#RECIBIDO}.
     */
    public void agregarItem(OrderItem item) {
        exigirEstado(OrderStatus.RECIBIDO, "agregar productos");
        items.add(validarItem(item));
    }

    /** @return Estado actual del pedido. */
    public OrderStatus getEstado() {
        return estado;
    }

    /**
     * Cambia el estado del pedido respetando el flujo definido en {@link OrderStatus}.
     *
     * @param nuevoEstado Estado destino.
     * @throws IllegalArgumentException Si es nulo.
     * @throws IllegalStateException    Si la transición no está permitida o se intenta
     *                                  poner {@link OrderStatus#EN_CAMINO} sin domiciliario.
     */
    public void cambiarEstado(OrderStatus nuevoEstado) {
        Validaciones.noNulo(nuevoEstado, "El nuevo estado");
        if (!estado.puedeCambiarA(nuevoEstado)) {
            throw new IllegalStateException("No se puede pasar de '" + estado + "' a '" + nuevoEstado + "'.");
        }
        if (nuevoEstado == OrderStatus.EN_CAMINO && domiciliario == null) {
            throw new IllegalStateException("Debe asignar un domiciliario antes de poner el pedido 'En camino'.");
        }
        this.estado = nuevoEstado;
    }

    /** @return Fecha y hora de creación del pedido. */
    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    /** @return Política de comisión aplicada. */
    public CommissionPolicy getPoliticaComision() {
        return politicaComision;
    }

    /**
     * Cambia la política de comisión mientras el pedido no esté finalizado.
     *
     * @param politicaComision Nueva política.
     * @throws IllegalArgumentException Si es nula.
     * @throws IllegalStateException    Si el pedido ya fue entregado o cancelado.
     */
    public final void setPoliticaComision(CommissionPolicy politicaComision) {
        Validaciones.noNulo(politicaComision, "La política de comisión");
        if (estado.esFinal()) {
            throw new IllegalStateException("No se puede cambiar la comisión de un pedido '" + estado + "'.");
        }
        this.politicaComision = politicaComision;
    }

    // ---- Liquidación (Information Expert) ----

    /** @return Suma de los subtotales de todas las líneas. */
    public double getSubtotal() {
        double subtotal = 0;
        for (OrderItem item : items) {
            subtotal += item.getSubtotal();
        }
        return subtotal;
    }

    /** @return Comisión de la plataforma calculada por la {@link CommissionPolicy}. */
    public double getComisionPlataforma() {
        return politicaComision.calcularComision(getSubtotal());
    }

    /** @return Valor que paga el cliente (subtotal de los productos). */
    public double getTotal() {
        return getSubtotal();
    }

    /** @return Valor que recibe el restaurante: total menos la comisión de la plataforma. */
    public double getNetoRestaurante() {
        return getTotal() - getComisionPlataforma();
    }

    // ---- Alias de compatibilidad con las capas service/presentación existentes ----

    /** @return Id del cliente que hizo el pedido. */
    public String getCustomerId() {
        return cliente.getId();
    }

    /** @return Id del restaurante. */
    public String getRestaurantId() {
        return restaurante.getId();
    }

    /** @return Descripción legible del estado actual. */
    public String getStatus() {
        return estado.getDescripcion();
    }

    /**
     * Línea de texto plano usada por {@code OrderFileDao}:
     * {@code id;clienteId;restauranteId;domiciliarioId|-;ESTADO;fechaHora;items}
     * donde {@code items} es {@code productoId:cantidad:precio|productoId:cantidad:precio...}.
     *
     * @return Registro delimitado por punto y coma.
     */
    public String toRegistro() {
        StringBuilder lineas = new StringBuilder();
        for (OrderItem item : items) {
            if (lineas.length() > 0) {
                lineas.append('|');
            }
            lineas.append(item.getProducto().getId()).append(':')
                    .append(item.getCantidad()).append(':')
                    .append(item.getPrecioUnitario());
        }
        return id + ";" + cliente.getId() + ";" + restaurante.getId() + ";"
                + (domiciliario == null ? "-" : domiciliario.getId()) + ";"
                + estado.name() + ";" + fechaHora + ";" + lineas;
    }

    // ---- Validaciones internas ----

    private Address validarDireccion(Address direccion) {
        Validaciones.noNulo(direccion, "La dirección de entrega");
        if (!cliente.tieneDireccion(direccion)) {
            throw new IllegalArgumentException("La dirección de entrega debe estar registrada por el cliente "
                    + cliente.getId() + ".");
        }
        return direccion;
    }

    private OrderItem validarItem(OrderItem item) {
        Validaciones.noNulo(item, "La línea del pedido");
        if (!restaurante.getId().equals(item.getProducto().getRestaurantId())) {
            throw new IllegalArgumentException("El producto '" + item.getProducto().getNombre()
                    + "' no pertenece al restaurante '" + restaurante.getNombre() + "'.");
        }
        return item;
    }

    private void exigirEstado(OrderStatus requerido, String accion) {
        if (estado != requerido) {
            throw new IllegalStateException("Solo se puede " + accion + " en un pedido '" + requerido
                    + "'. Estado actual: '" + estado + "'.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Order) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Pedido " + id + " - " + restaurante.getNombre() + " - " + cliente.getNombre()
                + " - " + fechaHora.format(FORMATO_FECHA) + " - " + estado
                + " - Total: " + String.format("$%,.0f", getTotal());
    }
}
