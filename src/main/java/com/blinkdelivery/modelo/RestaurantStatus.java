package com.blinkdelivery.modelo;

/**
 * Estados de operación de un {@link Restaurant}.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public enum RestaurantStatus {
    /** El restaurante recibe pedidos. */
    ABIERTO("Abierto"),
    /** El restaurante no recibe pedidos. */
    CERRADO("Cerrado");

    private final String descripcion;

    RestaurantStatus(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return Texto legible del estado. */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte un texto en un estado. Acepta el nombre de la constante o los
     * valores de la versión anterior {@code true} (abierto) y {@code false} (cerrado).
     *
     * @param texto Texto a convertir.
     * @return El estado correspondiente.
     * @throws IllegalArgumentException Si el texto es vacío o no corresponde a ningún estado.
     */
    public static RestaurantStatus desdeTexto(String texto) {
        String clave = Validaciones.claveEnum(Validaciones.texto(texto, "El estado del restaurante"));
        if (clave.equals("TRUE")) {
            return ABIERTO;
        }
        if (clave.equals("FALSE")) {
            return CERRADO;
        }
        for (RestaurantStatus estado : values()) {
            if (estado.name().equals(clave)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de restaurante inválido: '" + texto + "'.");
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
