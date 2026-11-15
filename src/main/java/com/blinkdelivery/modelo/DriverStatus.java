package com.blinkdelivery.modelo;

/**
 * Estados de disponibilidad de un {@link DeliveryDriver}.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public enum DriverStatus {
    /** Conectado y libre para recibir un pedido. */
    DISPONIBLE("Disponible"),
    /** Atendiendo un pedido en este momento. */
    EN_ENTREGA("En entrega"),
    /** No está trabajando en este momento. */
    DESCONECTADO("Desconectado");

    private final String descripcion;

    DriverStatus(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return Texto legible del estado. */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte un texto en un estado. Acepta el nombre de la constante, la
     * descripción y los valores de la versión anterior {@code true} (disponible)
     * y {@code false} (en entrega).
     *
     * @param texto Texto a convertir.
     * @return El estado correspondiente.
     * @throws IllegalArgumentException Si el texto es vacío o no corresponde a ningún estado.
     */
    public static DriverStatus desdeTexto(String texto) {
        String clave = Validaciones.claveEnum(Validaciones.texto(texto, "El estado del domiciliario"));
        if (clave.equals("TRUE")) {
            return DISPONIBLE;
        }
        if (clave.equals("FALSE")) {
            return EN_ENTREGA;
        }
        for (DriverStatus estado : values()) {
            if (estado.name().equals(clave)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de domiciliario inválido: '" + texto + "'.");
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
