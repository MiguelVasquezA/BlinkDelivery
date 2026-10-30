package com.blinkdelivery.modelo;

/**
 * Representa un cliente dentro del sistema BlinkDelivery.
 * Contiene la información básica de contacto y ubicación del usuario.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class Customer {
    private String identification;
    private String name;
    private String phone;
    private String address;

    /**
     * Crea una nueva instancia de un cliente.
     *
     * @param identification Documento de identidad único del cliente.
     * @param name Nombre completo del cliente.
     * @param phone Número de teléfono de contacto.
     * @param address Dirección de entrega habitual.
     */
    public Customer(String identification, String name, String phone, String address) {
        this.identification = identification;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    /** @return El documento de identidad del cliente. */
    public String getIdentification() { return identification; }

    /** @param identification El nuevo documento de identidad del cliente. */
    public void setIdentification(String identification) { this.identification = identification; }

    /** @return El nombre completo del cliente. */
    public String getName() { return name; }

    /** @param name El nuevo nombre del cliente. */
    public void setName(String name) { this.name = name; }

    /** @return El número de teléfono del cliente. */
    public String getPhone() { return phone; }

    /** @param phone El nuevo número de teléfono del cliente. */
    public void setPhone(String phone) { this.phone = phone; }

    /** @return La dirección de residencia o entrega. */
    public String getAddress() { return address; }

    /** @param address La nueva dirección de entrega. */
    public void setAddress(String address) { this.address = address; }

    /**
     * Convierte la información del cliente a un formato delimitado por punto y coma
     * apto para almacenamiento en archivos planos.
     *
     * @return Cadena con los datos formateados del cliente.
     */
    @Override
    public String toString() {
        return identification + ";" + name + ";" + phone + ";" + address;
    }
}
