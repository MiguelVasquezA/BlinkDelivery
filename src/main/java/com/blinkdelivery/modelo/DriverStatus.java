package com.blinkdelivery.modelo;

import java.util.Locale;

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

    /**
     * Lee el estado tal como está guardado en {@code domiciliario.estado}. Acepta los nombres de
     * la base ({@code disponible}, {@code ocupado}, {@code inactivo}) y los de la aplicación.
     *
     * @param texto valor leído de la base, en cualquier mayúscula o minúscula
     * @return el estado correspondiente
     * @throws IllegalArgumentException si el texto no corresponde a ningún estado
     */
    public static DriverStatus desdeBaseDatos(String texto) {
        String clave = texto == null ? "" : texto.trim().toUpperCase(Locale.ROOT);
        switch (clave) {
            case "DISPONIBLE":
            case "ACTIVO":
                return DISPONIBLE;
            case "EN_ENTREGA":
            case "OCUPADO":
                return EN_ENTREGA;
            case "DESCONECTADO":
            case "INACTIVO":
                return DESCONECTADO;
            default:
                throw new IllegalArgumentException("Estado de domiciliario inválido en la base de datos: '" + texto + "'.");
        }
    }

    /** Valor que se guarda en {@code domiciliario.estado} (minúsculas, según el check constraint). */
    public String aBaseDatos() {
        switch (this) {
            case EN_ENTREGA:
                return "ocupado";
            case DESCONECTADO:
                return "inactivo";
            default:
                return "disponible";
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
