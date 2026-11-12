package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Persona registrada en BlinkDelivery. Agrupa los datos de contacto comunes a
 * los distintos roles del sistema ({@link Customer} y {@link DeliveryDriver}).
 * <p>
 * La identidad de una persona es su {@code id} (documento de identidad), que es
 * inmutable. Dos personas son iguales si son del mismo rol y tienen el mismo id.
 * </p>
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public abstract class Person implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String nombre;
    private String telefono;
    private String email;

    /**
     * Inicializa los datos comunes de una persona.
     *
     * @param id       Documento de identidad (obligatorio).
     * @param nombre   Nombre completo (obligatorio).
     * @param telefono Teléfono de 7 a 10 dígitos (obligatorio solo si el rol lo exige).
     * @param telefono Teléfono de 7 a 10 dígitos (obligatorio).
     * @param email    Correo electrónico (opcional; si se informa debe ser válido).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o inválido.
     */
    protected Person(String id, String nombre, String telefono, String email) {
        this.id = Validaciones.texto(id, "El documento de identidad");
        setNombre(nombre);
        setTelefono(telefono);
        setEmail(email);
    }

    /** @return Documento de identidad de la persona. */
    public String getId() {
        return id;
    }

    /** @return Nombre completo de la persona. */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre Nuevo nombre completo.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setNombre(String nombre) {
        this.nombre = Validaciones.texto(nombre, "El nombre");
    }


    /** @return Teléfono de contacto, o cadena vacía si no fue registrado. */
    /** @return Teléfono de contacto. */
    public String getTelefono() {
        return telefono;
    }


    /** @return {@code true} si la persona tiene un teléfono registrado. */
    public boolean tieneTelefono() {
        return telefono != null && !telefono.isEmpty();
    }

    /**
     * Indica si el rol exige teléfono (RN26: obligatorio para domiciliarios,
     * opcional para quien solo es cliente).
     *
     * @return {@code true} si el teléfono es obligatorio para este rol
     */
    protected boolean telefonoObligatorio() {
        return false;
    }

    /**
     * @param telefono Nuevo teléfono de 7 a 10 dígitos; puede ser vacío si el rol no lo exige.
     * @throws IllegalArgumentException Si no cumple el formato o falta siendo obligatorio.
     */
    public final void setTelefono(String telefono) {
        this.telefono = telefonoObligatorio()
                ? Validaciones.telefono(telefono)
                : Validaciones.telefonoOpcional(telefono);
    /**
     * @param telefono Nuevo teléfono de 7 a 10 dígitos.
     * @throws IllegalArgumentException Si es vacío o no cumple el formato.
     */
    public final void setTelefono(String telefono) {
        this.telefono = Validaciones.telefono(telefono);
    }

    /** @return Correo electrónico, o cadena vacía si no fue registrado. */
    public String getEmail() {
        return email;
    }

    /**
     * @param email Nuevo correo; {@code null} o vacío lo deja sin registrar.
     * @throws IllegalArgumentException Si se informa con un formato inválido.
     */
    public final void setEmail(String email) {
        this.email = Validaciones.emailOpcional(email);
    }

    /** @return Alias de {@link #getId()}. */
    public String getIdentification() {
        return getId();
    }

    /** @return Alias de {@link #getNombre()}. */
    public String getName() {
        return getNombre();
    }

    /** @param name Alias de {@link #setNombre(String)}. */
    public final void setName(String name) {
        setNombre(name);
    }

    /** @return Alias de {@link #getTelefono()}. */
    public String getPhone() {
        return getTelefono();
    }

    /** @param phone Alias de {@link #setTelefono(String)}. */
    public final void setPhone(String phone) {
        setTelefono(phone);
    }

    /**
     * Dos personas son iguales si pertenecen a la misma clase concreta (rol) y
     * tienen el mismo documento de identidad.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Person) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return nombre + " (ID " + id + ")";
    }
}
