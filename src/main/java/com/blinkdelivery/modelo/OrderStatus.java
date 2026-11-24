package com.blinkdelivery.modelo;

/**
 * Estados del ciclo de vida de un {@link Order}.
 * <p>
 * Cada estado conoce a qué estados puede pasar (Information Expert), de modo que
 * la regla del flujo de pedidos vive en un único lugar:
 * </p>
 * <pre>
 * RECIBIDO → ACEPTADO → EN_PREPARACION → LISTO_PARA_ENTREGAR → EN_CAMINO → ENTREGADO
 * (cualquier estado anterior a EN_CAMINO) → CANCELADO
 * </pre>
 * <p>
 * Lectura y escritura con la base: {@link #desdeTexto} nunca lanza excepción (un valor desconocido
 * se registra en consola y se devuelve {@link #RECIBIDO}); {@link #aBaseDatos()} devuelve el texto
 * exacto que exige el check constraint {@code estado_pedido_valido}.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 3.0
 */
public enum OrderStatus {
    /** El pedido fue registrado y espera respuesta del restaurante. */
    RECIBIDO("Recibido", "recibido"),
    /** El restaurante aceptó el pedido. */
    ACEPTADO("Aceptado", "aceptado"),
    /** El restaurante está preparando el pedido. */
    EN_PREPARACION("En preparación", "en preparación"),
    /** El restaurante terminó el pedido; ya puede tomarlo un domiciliario. */
    LISTO_PARA_ENTREGAR("Listo para entregar", "asignado"),
    /** El domiciliario lleva el pedido al cliente. */
    EN_CAMINO("En camino", "en camino"),
    /** Estado final: el pedido fue entregado al cliente. */
    ENTREGADO("Entregado", "entregado"),
    /** Estado final: el pedido fue cancelado. */
    CANCELADO("Cancelado", "cancelado");

    /** Valor por defecto cuando la base trae un estado desconocido. */
    static final OrderStatus POR_DEFECTO = RECIBIDO;

    private final String descripcion;
    private final String valorBaseDatos;

    OrderStatus(String descripcion, String valorBaseDatos) {
        this.descripcion = descripcion;
        this.valorBaseDatos = valorBaseDatos;
    }

    /** @return Texto legible del estado, apto para mostrar en pantalla. */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * @return texto exacto que se guarda en {@code pedido.estado} (minúsculas, con espacios como
     *         en el check constraint).
     */
    public String aBaseDatos() {
        return valorBaseDatos;
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
                return destino == LISTO_PARA_ENTREGAR || destino == CANCELADO;
            case LISTO_PARA_ENTREGAR:
                return destino == EN_CAMINO || destino == CANCELADO;
            case EN_CAMINO:
                return destino == ENTREGADO;
            default:
                return false;
        }
    }

    /**
     * Convierte un texto en un estado. Acepta el nombre de la constante, la descripción, los
     * valores de la base ({@code en camino}, {@code asignado}...) y los nombres de versiones
     * anteriores. No distingue mayúsculas, tildes, espacios ni guiones. Nunca lanza excepción:
     * un texto desconocido se registra en consola y devuelve {@link #POR_DEFECTO}.
     *
     * @param texto Texto a convertir (puede ser {@code null}).
     * @return El estado correspondiente.
     */
    public static OrderStatus desdeTexto(String texto) {
        OrderStatus encontrado = buscar(texto);
        return encontrado == null ? EnumTolerante.desconocido("OrderStatus", texto, POR_DEFECTO) : encontrado;
    }

    /**
     * Convierte un texto escrito por el usuario (API o formulario). A diferencia de
     * {@link #desdeTexto}, rechaza los valores desconocidos para no cambiar un pedido por error.
     *
     * @throws IllegalArgumentException si el texto no corresponde a ningún estado
     */
    public static OrderStatus desdeEntrada(String texto) {
        OrderStatus encontrado = buscar(texto);
        if (encontrado == null) {
            throw new IllegalArgumentException("Estado de pedido inválido: '" + texto + "'.");
        }
        return encontrado;
    }

    /** @return el estado que corresponde al texto, o {@code null} si no se reconoce. */
    private static OrderStatus buscar(String texto) {
        switch (EnumTolerante.clave(texto)) {
            case "recibido": case "creado": case "pendiente":
                return RECIBIDO;
            case "aceptado": case "confirmado":
                return ACEPTADO;
            case "en_preparacion": case "preparando":
                return EN_PREPARACION;
            case "listo_para_entregar": case "listo": case "listo_para_recoger": case "asignado":
                return LISTO_PARA_ENTREGAR;
            case "en_camino":
                return EN_CAMINO;
            case "entregado":
                return ENTREGADO;
            case "cancelado":
                return CANCELADO;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
