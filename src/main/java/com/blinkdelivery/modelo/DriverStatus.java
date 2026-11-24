package com.blinkdelivery.modelo;

/**
 * Estados de disponibilidad de un {@link DeliveryDriver}.
 * <p>
 * Lectura desde la base: {@link #desdeBaseDatos} nunca lanza excepción; un texto desconocido se
 * registra en consola y se devuelve {@link #DISPONIBLE}. Escritura: {@link #aBaseDatos()} devuelve
 * el valor en minúsculas que exige el check constraint {@code estado_domiciliario_valido}.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 3.0
 */
public enum DriverStatus {
    /** Conectado y libre para recibir un pedido. */
    DISPONIBLE("Disponible"),
    /** Atendiendo un pedido en este momento. */
    EN_ENTREGA("En entrega"),
    /** No está trabajando en este momento. */
    DESCONECTADO("Desconectado");

    /** Valor por defecto cuando la base trae un estado desconocido. */
    static final DriverStatus POR_DEFECTO = DISPONIBLE;

    private final String descripcion;

    DriverStatus(String descripcion) {
        this.descripcion = descripcion;
    }

    /** @return Texto legible del estado. */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte un texto en un estado. Acepta el nombre de la constante, la descripción y los
     * valores de la base. Nunca lanza excepción: un texto desconocido devuelve {@link #POR_DEFECTO}.
     *
     * @param texto Texto a convertir (puede ser {@code null}).
     * @return El estado correspondiente.
     */
    public static DriverStatus desdeTexto(String texto) {
        DriverStatus encontrado = buscar(texto);
        return encontrado == null ? EnumTolerante.desconocido("DriverStatus", texto, POR_DEFECTO) : encontrado;
    }

    /**
     * Convierte un texto escrito por el usuario (API o formulario). Rechaza los valores
     * desconocidos en lugar de usar un valor por defecto.
     *
     * @throws IllegalArgumentException si el texto no corresponde a ningún estado
     */
    public static DriverStatus desdeEntrada(String texto) {
        DriverStatus encontrado = buscar(texto);
        if (encontrado == null) {
            throw new IllegalArgumentException("Estado de domiciliario inválido: '" + texto + "'.");
        }
        return encontrado;
    }

    /** @return el estado que corresponde al texto, o {@code null} si no se reconoce. */
    private static DriverStatus buscar(String texto) {
        switch (EnumTolerante.clave(texto)) {
            case "disponible": case "activo": case "available":
                return DISPONIBLE;
            case "en_entrega": case "ocupado":
                return EN_ENTREGA;
            case "desconectado": case "inactivo":
                return DESCONECTADO;
            default:
                return null;
        }
    }

    /** Lee el estado guardado en {@code domiciliario.estado}. Mismo criterio que {@link #desdeTexto}. */
    public static DriverStatus desdeBaseDatos(String texto) {
        return desdeTexto(texto);
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
