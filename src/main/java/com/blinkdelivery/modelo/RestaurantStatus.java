package com.blinkdelivery.modelo;

/**
 * Estados de operación de un {@link Restaurant}.
 * <p>
 * En la base se guarda {@code abierto} o {@code cerrado} ({@code restaurante.estado}). Nunca lanza
 * excepción al leer: un texto desconocido se registra en consola y se devuelve {@link #CERRADO},
 * para no recibir pedidos de un restaurante cuyo estado no se entiende.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 3.0
 */
public enum RestaurantStatus {
    /** El restaurante recibe pedidos. */
    ABIERTO("Abierto"),
    /** El restaurante no recibe pedidos. */
    CERRADO("Cerrado");

    /** Valor por defecto cuando la base trae un estado desconocido. */
    static final RestaurantStatus POR_DEFECTO = CERRADO;

    private final String descripcion;

    RestaurantStatus(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return Texto legible del estado. */
    public String getDescripcion() {
        return descripcion;
    }

    /** @return texto que se guarda en {@code restaurante.estado} ({@code abierto} o {@code cerrado}). */
    public String aBaseDatos() {
        return this == ABIERTO ? "abierto" : "cerrado";
    }

    /**
     * Convierte un texto en un estado. Acepta el nombre de la constante, la descripción y los
     * valores booleanos de versiones anteriores. Nunca lanza excepción.
     *
     * @param texto Texto a convertir (puede ser {@code null}).
     * @return El estado correspondiente.
     */
    public static RestaurantStatus desdeTexto(String texto) {
        switch (EnumTolerante.clave(texto)) {
            case "abierto": case "abierta": case "true":
                return ABIERTO;
            case "cerrado": case "cerrada": case "false":
                return CERRADO;
            default:
                return EnumTolerante.desconocido("RestaurantStatus", texto, POR_DEFECTO);
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
