package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Calificación que un cliente deja a un restaurante después de recibir un pedido.
 * <p>
 * Es inmutable: una vez registrada forma parte del historial y alimenta el
 * promedio del restaurante ({@link Restaurant#getPromedioCalificacion()}).
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Rating implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Mínimo de estrellas permitido. */
    public static final int MIN_ESTRELLAS = 1;
    /** Máximo de estrellas permitido. */
    public static final int MAX_ESTRELLAS = 5;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String id;
    private final String orderId;
    private final String customerId;
    private final String restaurantId;
    private final int estrellas;
    private final String comentario;
    private final LocalDateTime fecha;

    /**
     * Crea una calificación con la fecha y hora actuales.
     *
     * @param id           Identificador único (obligatorio).
     * @param orderId      Id del pedido calificado (obligatorio).
     * @param customerId   Id del cliente que califica (obligatorio).
     * @param restaurantId Id del restaurante calificado (obligatorio).
     * @param estrellas    Puntuación de 1 a 5.
     * @param comentario   Comentario (opcional).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o las estrellas están fuera de rango.
     */
    public Rating(String id, String orderId, String customerId, String restaurantId,
                  int estrellas, String comentario) {
        this(id, orderId, customerId, restaurantId, estrellas, comentario, LocalDateTime.now());
    }

    /**
     * Crea una calificación con una fecha específica (por ejemplo, al reconstruirla desde disco).
     *
     * @param id           Identificador único (obligatorio).
     * @param orderId      Id del pedido calificado (obligatorio).
     * @param customerId   Id del cliente que califica (obligatorio).
     * @param restaurantId Id del restaurante calificado (obligatorio).
     * @param estrellas    Puntuación de 1 a 5.
     * @param comentario   Comentario (opcional).
     * @param fecha        Fecha y hora de la calificación (obligatoria).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o las estrellas están fuera de rango.
     */
    public Rating(String id, String orderId, String customerId, String restaurantId,
                  int estrellas, String comentario, LocalDateTime fecha) {
        if (estrellas < MIN_ESTRELLAS || estrellas > MAX_ESTRELLAS) {
            throw new IllegalArgumentException("La calificación debe estar entre " + MIN_ESTRELLAS
                    + " y " + MAX_ESTRELLAS + " estrellas.");
        }
        this.id = Validaciones.texto(id, "El id de la calificación");
        this.orderId = Validaciones.texto(orderId, "El id del pedido calificado");
        this.customerId = Validaciones.texto(customerId, "El id del cliente");
        this.restaurantId = Validaciones.texto(restaurantId, "El id del restaurante");
        this.estrellas = estrellas;
        this.comentario = Validaciones.opcional(comentario);
        this.fecha = Validaciones.noNulo(fecha, "La fecha de la calificación");
    }

    /** @return Identificador de la calificación. */
    public String getId() {
        return id;
    }

    /** @return Id del pedido calificado. */
    public String getOrderId() {
        return orderId;
    }

    /** @return Id del cliente que calificó. */
    public String getCustomerId() {
        return customerId;
    }

    /** @return Id del restaurante calificado. */
    public String getRestaurantId() {
        return restaurantId;
    }

    /** @return Puntuación de 1 a 5. */
    public int getEstrellas() {
        return estrellas;
    }

    /** @return Comentario, o cadena vacía si no dejó comentario. */
    public String getComentario() {
        return comentario;
    }

    /** @return Fecha y hora de la calificación. */
    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Rating) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String barra = "★".repeat(estrellas) + "☆".repeat(MAX_ESTRELLAS - estrellas);
        return barra + " (" + fecha.format(FORMATO_FECHA) + ")" + (comentario.isEmpty() ? "" : " - " + comentario);
    }
}
