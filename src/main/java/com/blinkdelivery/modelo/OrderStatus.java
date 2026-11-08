package com.blinkdelivery.modelo;

/**
 * Estados del ciclo de vida de un {@link Order}.
 * <p>
 * Cada estado conoce a qué estados puede pasar (Information Expert), de modo que
 * la regla del flujo de pedidos vive en un único lugar:
 * </p>
 * <pre>
 * RECIBIDO → ACEPTADO → EN_PREPARACION → LISTO_PARA_RECOGER → EN_CAMINO → ENTREGADO
 * (cualquier estado anterior a EN_CAMINO) → CANCELADO
 * </pre>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public enum OrderStatus {
    /** El pedido fue registrado y espera respuesta del restaurante. */
    RECIBIDO("Recibido"),
    /** El restaurante aceptó el pedido. */
    ACEPTADO("Aceptado"),
    /** El restaurante está preparando el pedido. */
    EN_PREPARACION("En preparación"),
    /** El pedido está listo para que el domiciliario lo recoja. */
    LISTO_PARA_RECOGER("Listo para recoger"),
    /** El domiciliario lleva el pedido al cliente. */
    EN_CAMINO("En camino"),
    /** Estado final: el pedido fue entregado al cliente. */
    ENTREGADO("Entregado"),
    /** Estado final: el pedido fue cancelado. */
    CANCELADO("Cancelado");

    private final String descripcion;

    OrderStatus(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return Texto legible del estado, apto para mostrar en pantalla. */
    public String getDescripcion() {
        return descripcion;
    }

    /** @return {@code true} si el estado es terminal (entregado o cancelado). */
    public boolean esFinal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    /**
     * Indica si desde este estado se puede pasar al estado destino.
     *
     * @param destino Estado al que se quiere pasar.
     * @return {@code true} si la transición está permitida.
     */
    public boolean puedeCambiarA(OrderStatus destino) {
        if (destino == null) {
            return false;
        }
        switch (this) {
            case RECIBIDO:
                return destino == ACEPTADO || destino == CANCELADO;
            case ACEPTADO:
                return destino == EN_PREPARACION || destino == CANCELADO;
            case EN_PREPARACION:
                return destino == LISTO_PARA_RECOGER || destino == CANCELADO;
            case LISTO_PARA_RECOGER:
                return destino == EN_CAMINO || destino == CANCELADO;
            case EN_CAMINO:
                return destino == ENTREGADO;
            default:
                return false;
        }
    }

    /**
     * Convierte un texto en un estado. Acepta el nombre de la constante
     * ({@code EN_CAMINO}) o la descripción ({@code "En camino"}), sin distinguir
     * mayúsculas ni tildes, y también los nombres de la versión anterior
     * ({@code "Creado"}, {@code "Confirmado"}, {@code "Listo"}).
     *
     * @param texto Texto a convertir.
     * @return El estado correspondiente.
     * @throws IllegalArgumentException Si el texto es vacío o no corresponde a ningún estado.
     */
    public static OrderStatus desdeTexto(String texto) {
        String clave = Validaciones.claveEnum(Validaciones.texto(texto, "El estado del pedido"));
        switch (clave) {
            case "CREADO":
                return RECIBIDO;
            case "CONFIRMADO":
                return ACEPTADO;
            case "LISTO":
                return LISTO_PARA_RECOGER;
            default:
                break;
        }
        for (OrderStatus estado : values()) {
            if (estado.name().equals(clave) || Validaciones.claveEnum(estado.descripcion).equals(clave)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pedido inválido: '" + texto + "'.");
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
