package com.blinkdelivery.modelo;

/**
 * Representa un repartidor o domiciliario dentro del sistema de entregas.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class DeliveryDriver {
    private String identification;
    private String name;
    private String phone;
    private String vehicleType;
    private boolean available;

    /**
     * Crea una instancia de domiciliario con estado inicial disponible.
     *
     * @param identification Documento de identidad único.
     * @param name Nombre completo del domiciliario.
     * @param phone Teléfono de contacto.
     * @param vehicleType Medio de transporte (ej. Moto, Bicicleta).
     */
    public DeliveryDriver(String identification, String name, String phone, String vehicleType) {
        this.identification = identification;
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.available = true;
    }

    /** @return La identificación del domiciliario. */
    public String getIdentification() { return identification; }

    /** @return El nombre completo del domiciliario. */
    public String getName() { return name; }

    /** @return El número de contacto del domiciliario. */
    public String getPhone() { return phone; }

    /** @return El tipo de vehículo utilizado. */
    public String getVehicleType() { return vehicleType; }

    /** @return {@code true} si está libre para asignar un domicilio; {@code false} si está ocupado. */
    public boolean isAvailable() { return available; }

    /** @param available El nuevo estado de ocupación/disponibilidad. */
    public void setAvailable(boolean available) { this.available = available; }

    /**
     * Convierte los datos del repartidor en una línea de texto plano.
     *
     * @return Cadena estructurada con los datos del domiciliario.
     */
    @Override
    public String toString() {
        return identification + ";" + name + ";" + phone + ";" + vehicleType + ";" + available;
    }
}
