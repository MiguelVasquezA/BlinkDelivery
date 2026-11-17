package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Dirección de entrega registrada por un {@link Customer}.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Ciudad asignada cuando no se especifica otra. */
    public static final String CIUDAD_POR_DEFECTO = "Valledupar";

    private final String id;
    private String etiqueta;
    private String direccionExacta;
    private String barrio;
    private String ciudad;
    private String referencia;

    /**
     * Crea una dirección en la ciudad por defecto ({@value #CIUDAD_POR_DEFECTO}).
     *
     * @param id              Identificador único de la dirección (obligatorio).
     * @param etiqueta        Nombre corto: Casa, Trabajo, etc. (obligatorio).
     * @param direccionExacta Nomenclatura de la dirección (obligatoria).
     * @param barrio          Barrio (opcional).
     * @param referencia      Punto de referencia (opcional).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo o vacío.
     */
    public Address(String id, String etiqueta, String direccionExacta, String barrio, String referencia) {
        this(id, etiqueta, direccionExacta, barrio, CIUDAD_POR_DEFECTO, referencia);
    }

    /**
     * Crea una dirección completa.
     *
     * @param id              Identificador único de la dirección (obligatorio).
     * @param etiqueta        Nombre corto: Casa, Trabajo, etc. (obligatorio).
     * @param direccionExacta Nomenclatura de la dirección (obligatoria).
     * @param barrio          Barrio (opcional).
     * @param ciudad          Ciudad; si es nula o vacía se usa {@value #CIUDAD_POR_DEFECTO}.
     * @param referencia      Punto de referencia (opcional).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo o vacío.
     */
    public Address(String id, String etiqueta, String direccionExacta, String barrio,
                   String ciudad, String referencia) {
        this.id = Validaciones.texto(id, "El id de la dirección");
        setEtiqueta(etiqueta);
        setDireccionExacta(direccionExacta);
        setBarrio(barrio);
        setCiudad(ciudad);
        setReferencia(referencia);
    }

    /** @return Identificador de la dirección. */
    public String getId() {
        return id;
    }

    /** @return Etiqueta de la dirección (Casa, Trabajo, etc.). */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * @param etiqueta Nueva etiqueta.
     * @throws IllegalArgumentException Si es nula o vacía.
     */
    public final void setEtiqueta(String etiqueta) {
        this.etiqueta = Validaciones.texto(etiqueta, "La etiqueta de la dirección");
    }

    /** @return Nomenclatura exacta de la dirección. */
    public String getDireccionExacta() {
        return direccionExacta;
    }

    /**
     * @param direccionExacta Nueva nomenclatura.
     * @throws IllegalArgumentException Si es nula o vacía.
     */
    public final void setDireccionExacta(String direccionExacta) {
        this.direccionExacta = Validaciones.texto(direccionExacta, "La dirección exacta");
    }

    /** @return Barrio, o cadena vacía si no fue registrado. */
    public String getBarrio() {
        return barrio;
    }

    /** @param barrio Nuevo barrio (opcional). */
    public final void setBarrio(String barrio) {
        this.barrio = Validaciones.opcional(barrio);
    }

    /** @return Ciudad de la dirección. */
    public String getCiudad() {
        return ciudad;
    }

    /** @param ciudad Nueva ciudad; si es nula o vacía se usa {@value #CIUDAD_POR_DEFECTO}. */
    public final void setCiudad(String ciudad) {
        String c = Validaciones.opcional(ciudad);
        this.ciudad = c.isEmpty() ? CIUDAD_POR_DEFECTO : c;
    }

    /** @return Punto de referencia, o cadena vacía si no fue registrado. */
    public String getReferencia() {
        return referencia;
    }

    /** @param referencia Nuevo punto de referencia (opcional). */
    public final void setReferencia(String referencia) {
        this.referencia = Validaciones.opcional(referencia);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Address) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(etiqueta).append(": ").append(direccionExacta);
        if (!barrio.isEmpty()) {
            sb.append(", ").append(barrio);
        }
        sb.append(", ").append(ciudad);
        if (!referencia.isEmpty()) {
            sb.append(" (").append(referencia).append(")");
        }
        return sb.toString();
    }
}
