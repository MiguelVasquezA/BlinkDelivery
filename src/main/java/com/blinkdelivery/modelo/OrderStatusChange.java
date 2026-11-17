package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro inmutable de un cambio de estado en el ciclo de vida de un {@link Order} (RN18).
 *
 * @author BlinkDelivery
 * @version 1.0
 */
public final class OrderStatusChange implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String orderId;
    private final OrderStatus estado;
    private final LocalDateTime fecha;

    /**
     * @param orderId Identificador del pedido (obligatorio).
     * @param estado  Estado al que pasó el pedido (obligatorio).
     * @param fecha   Momento del cambio (obligatorio).
     * @throws IllegalArgumentException Si algún dato es nulo o vacío.
     */
    public OrderStatusChange(String orderId, OrderStatus estado, LocalDateTime fecha) {
        this.orderId = Validaciones.texto(orderId, "El id del pedido");
        this.estado = Validaciones.noNulo(estado, "El estado del pedido");
        this.fecha = Validaciones.noNulo(fecha, "La fecha del cambio de estado");
    }

    /** @return Identificador del pedido. */
    public String getOrderId() {
        return orderId;
    }

    /** @return Estado registrado. */
    public OrderStatus getEstado() {
        return estado;
    }

    /** @return Momento del cambio de estado. */
    public LocalDateTime getFecha() {
        return fecha;
    }

    /** @return Línea para el archivo plano: {@code idPedido;ESTADO;fecha}. */
    public String toRegistro() {
        return orderId + ";" + estado.name() + ";" + fecha;
    }

    @Override
    public String toString() {
        return fecha.format(FORMATO_FECHA) + " - " + estado;
    }
}
