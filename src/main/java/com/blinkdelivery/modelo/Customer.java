package com.blinkdelivery.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cliente de BlinkDelivery. Hereda los datos de contacto de {@link Person} y
 * administra sus direcciones de entrega.
 * <p>
 * Invariante: un cliente siempre tiene al menos una dirección. La primera
 * dirección de la lista es la dirección principal.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Customer extends Person {
    private static final long serialVersionUID = 1L;

    private final ArrayList<Address> direcciones = new ArrayList<>();

    /**
     * Crea un cliente con su dirección principal.
     *
     * @param id                 Documento de identidad (obligatorio).
     * @param nombre             Nombre completo (obligatorio).
     * @param telefono           Teléfono de 7 a 10 dígitos (obligatorio).
     * @param email              Correo electrónico (opcional).
     * @param direccionPrincipal Dirección principal de entrega (obligatoria).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o inválido.
     */
    public Customer(String id, String nombre, String telefono, String email, Address direccionPrincipal) {
        super(id, nombre, telefono, email);
        agregarDireccion(direccionPrincipal);
    }

    /**
     * Constructor de compatibilidad usado por la persistencia en archivo plano:
     * crea el cliente sin correo y convierte el texto de la dirección en su
     * {@link Address} principal (id {@code <documento>-DIR-1}, etiqueta "Principal").
     *
     * @param identification Documento de identidad (obligatorio).
     * @param name           Nombre completo (obligatorio).
     * @param phone          Teléfono de 7 a 10 dígitos (obligatorio).
     * @param address        Dirección exacta de entrega (obligatoria).
     * @throws IllegalArgumentException Si algún dato es nulo, vacío o inválido.
     */
    public Customer(String identification, String name, String phone, String address) {
        this(identification, name, phone, null,
                new Address(Validaciones.texto(identification, "El documento de identidad") + "-DIR-1",
                        "Principal", address, null, null));
    }

    /**
     * Agrega una dirección de entrega al cliente.
     *
     * @param direccion Dirección a agregar.
     * @throws IllegalArgumentException Si es nula o ya existe una dirección con el mismo id.
     */
    public final void agregarDireccion(Address direccion) {
        Validaciones.noNulo(direccion, "La dirección");
        if (direcciones.contains(direccion)) {
            throw new IllegalArgumentException("El cliente ya tiene registrada la dirección con id " + direccion.getId() + ".");
        }
        direcciones.add(direccion);
    }

    /**
     * Elimina una dirección del cliente.
     *
     * @param idDireccion Id de la dirección a eliminar.
     * @throws IllegalArgumentException Si la dirección no existe.
     * @throws IllegalStateException    Si es la única dirección del cliente.
     */
    public void eliminarDireccion(String idDireccion) {
        Address direccion = obtenerDireccion(idDireccion);
        if (direcciones.size() == 1) {
            throw new IllegalStateException("El cliente debe conservar al menos una dirección.");
        }
        direcciones.remove(direccion);
    }

    /** @return Vista de solo lectura de las direcciones; la primera es la principal. */
    public List<Address> getDirecciones() {
        return Collections.unmodifiableList(direcciones);
    }

    /** @return Dirección principal del cliente (nunca nula). */
    public Address getDireccionPrincipal() {
        return direcciones.get(0);
    }

    /**
     * Marca como principal una dirección ya registrada.
     *
     * @param idDireccion Id de la dirección.
     * @throws IllegalArgumentException Si la dirección no pertenece al cliente.
     */
    public final void setDireccionPrincipal(String idDireccion) {
        Address direccion = obtenerDireccion(idDireccion);
        direcciones.remove(direccion);
        direcciones.add(0, direccion);
    }

    /**
     * @param direccion Dirección a verificar.
     * @return {@code true} si la dirección está registrada para este cliente.
     */
    public boolean tieneDireccion(Address direccion) {
        return direccion != null && direcciones.contains(direccion);
    }

    /**
     * Busca una dirección del cliente por su id.
     *
     * @param idDireccion Id de la dirección.
     * @return La dirección encontrada.
     * @throws IllegalArgumentException Si el id es vacío o la dirección no existe.
     */
    public Address obtenerDireccion(String idDireccion) {
        String id = Validaciones.texto(idDireccion, "El id de la dirección");
        for (Address a : direcciones) {
            if (a.getId().equals(id)) {
                return a;
            }
        }
        throw new IllegalArgumentException("El cliente no tiene una dirección con id " + id + ".");
    }

    // ---- Alias de compatibilidad con las capas dao/service existentes ----

    /** @return Dirección exacta de la dirección principal. */
    public String getAddress() {
        return getDireccionPrincipal().getDireccionExacta();
    }

    /**
     * Actualiza la dirección exacta de la dirección principal.
     *
     * @param address Nueva dirección exacta.
     * @throws IllegalArgumentException Si es nula o vacía.
     */
    public final void setAddress(String address) {
        getDireccionPrincipal().setDireccionExacta(address);
    }

    /**
     * Línea de texto plano usada por {@code CustomerFileDao}:
     * {@code id;nombre;telefono;direccionPrincipal}.
     *
     * @return Registro delimitado por punto y coma.
     */
    public String toRegistro() {
        return getId() + ";" + getNombre() + ";" + getTelefono() + ";" + getAddress();
    }

    @Override
    public String toString() {
        return getNombre() + " (ID " + getId() + ") - Tel: " + getTelefono();
    }
}
