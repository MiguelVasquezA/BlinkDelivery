package com.blinkdelivery.modelo;

/**
 * Representa la calificación y opinión dejada por un usuario al finalizar un pedido.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class Rating {
    private String orderId;
    private int score;
    private String comment;

    /**
     * Crea un objeto de calificación.
     *
     * @param orderId ID de la orden evaluada.
     * @param score Puntuación asignada (entre 1 y 5).
     * @param comment Comentario u observación textual.
     */
    public Rating(String orderId, int score, String comment) {
        this.orderId = orderId;
        this.score = score;
        this.comment = comment;
    }

    /** @return Código del pedido calificado. */
    public String getOrderId() { return orderId; }

    /** @return Puntuación numérica asignada. */
    public int getScore() { return score; }

    /** @return Texto del comentario dejado por el usuario. */
    public String getComment() { return comment; }

    /**
     * Representación delimitada en formato de archivo plano.
     *
     * @return Cadena con la calificación guardada.
     */
    @Override
    public String toString() {
        return orderId + ";" + score + ";" + comment;
    }
}
