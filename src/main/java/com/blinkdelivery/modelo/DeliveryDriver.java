package com.blinkdelivery.modelo;

/**
 * Domiciliario que recoge y entrega los pedidos. Hereda los datos de contacto
 * de {@link Person}.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class DeliveryDriver extends Person {
    private static final long serialVersionUID = 1L;

    /** Valor usado cuando el vehículo no tiene placa registrada (por ejemplo, bicicleta). */
    public static final String SIN_PLACA = "Sin placa";

    private String placaVehiculo;
    private String tipoVehiculo;
    private DriverStatus estado;
    private double calificacionPromedio;

    /**
     * Crea un domiciliario completo, disponible y sin calificaciones.
     *
     * @param id            Documento de identidad (obligatorio).
     * @param nombre        Nombre completo (obligatorio).
     * @param telefono      Teléfono de 7 a 10 dígitos (obligatorio).
     * @param email         Correo electrónico (opcional).
     * @param placaVehiculo Placa del vehículo (obligatoria; use {@link #SIN_PLACA} si no aplica).
     * @param tipoVehiculo  Tipo de vehículo: Moto, Bicicleta, etc. (obligatorio).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o inválido.
     */
    public DeliveryDriver(String id, String nombre, String telefono, String email,
                          String placaVehiculo, String tipoVehiculo) {
        super(id, nombre, telefono, email);
        setPlacaVehiculo(placaVehiculo);
        setTipoVehiculo(tipoVehiculo);
        this.estado = DriverStatus.DISPONIBLE;
        this.calificacionPromedio = 0.0;
    }

    /**
     * RN26: todo domiciliario debe tener teléfono registrado.
     *
     * @return siempre {@code true}
     */
    @Override
    protected boolean telefonoObligatorio() {
        return true;
    }

    /**
     * Constructor de compatibilidad usado por la persistencia en archivo plano:
     * crea el domiciliario sin correo y con placa {@link #SIN_PLACA}.
     *
     * @param identification Documento de identidad.
     * @param name           Nombre completo.
     * @param phone          Teléfono de 7 a 10 dígitos.
     * @param vehicleType    Tipo de vehículo.
     * @throws IllegalArgumentException Si algún dato es nulo, vacío o inválido.
     */
    public DeliveryDriver(String identification, String name, String phone, String vehicleType) {
        this(identification, name, phone, null, SIN_PLACA, vehicleType);
    }

    /** @return Placa del vehículo. */
    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    /**
     * @param placaVehiculo Nueva placa (se guarda en mayúsculas).
     * @throws IllegalArgumentException Si es nula o vacía.
     */
    public final void setPlacaVehiculo(String placaVehiculo) {
        String placa = Validaciones.texto(placaVehiculo, "La placa del vehículo");
        this.placaVehiculo = placa.equalsIgnoreCase(SIN_PLACA) ? SIN_PLACA : placa.toUpperCase();
    }

    /** @return Tipo de vehículo. */
    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    /**
     * @param tipoVehiculo Nuevo tipo de vehículo.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = Validaciones.texto(tipoVehiculo, "El tipo de vehículo");
    }

    /** @return Estado actual del domiciliario. */
    public DriverStatus getEstado() {
        return estado;
    }

    /**
     * @param estado Nuevo estado.
     * @throws IllegalArgumentException Si es nulo.
     */
    public final void setEstado(DriverStatus estado) {
        this.estado = Validaciones.noNulo(estado, "El estado del domiciliario");
    }

    /** @return Calificación promedio entre 0.0 (sin calificaciones) y 5.0. */
    public double getCalificacionPromedio() {
        return calificacionPromedio;
    }

    /**
     * @param calificacionPromedio Nueva calificación promedio entre 0.0 y 5.0.
     * @throws IllegalArgumentException Si está fuera del rango.
     */
    public final void setCalificacionPromedio(double calificacionPromedio) {
        Validaciones.noNegativo(calificacionPromedio, "La calificación promedio");
        if (calificacionPromedio > 5.0) {
            throw new IllegalArgumentException("La calificación promedio no puede ser mayor que 5.");
        }
        this.calificacionPromedio = calificacionPromedio;
    }

    /** @return {@code true} si el domiciliario está {@link DriverStatus#DISPONIBLE}. */
    public boolean estaDisponible() {
        return estado == DriverStatus.DISPONIBLE;
    }

    // ---- Alias de compatibilidad con las capas dao/service existentes ----

    /** @return Alias de {@link #getTipoVehiculo()}. */
    public String getVehicleType() {
        return getTipoVehiculo();
    }

    /** @return Alias de {@link #estaDisponible()}. */
    public boolean isAvailable() {
        return estaDisponible();
    }

    /**
     * Alias de compatibilidad: {@code true} pasa a {@link DriverStatus#DISPONIBLE} y
     * {@code false} a {@link DriverStatus#EN_ENTREGA}.
     *
     * @param available Nueva disponibilidad.
     */
    public final void setAvailable(boolean available) {
        setEstado(available ? DriverStatus.DISPONIBLE : DriverStatus.EN_ENTREGA);
    }

    /**
     * Línea de texto plano usada por {@code DriverFileDao}:
     * {@code id;nombre;telefono;tipoVehiculo;ESTADO}.
     *
     * @return Registro delimitado por punto y coma.
     */
    public String toRegistro() {
        return getId() + ";" + getNombre() + ";" + getTelefono() + ";" + tipoVehiculo + ";" + estado.name();
    }

    @Override
    public String toString() {
        return getNombre() + " (ID " + getId() + ") - " + tipoVehiculo + " " + placaVehiculo + " [" + estado + "]";
    }
}
