package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Categoría del menú de un restaurante (Entradas, Bebidas, Postres, etc.) con la
 * que se agrupan los {@link Product}.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String nombre;
    private String descripcion;

    /**
     * Crea una categoría.
     *
     * @param id          Identificador único (obligatorio).
     * @param nombre      Nombre de la categoría (obligatorio).
     * @param descripcion Descripción (opcional).
     * @throws IllegalArgumentException Si el id o el nombre son nulos o vacíos.
     */
    public Category(String id, String nombre, String descripcion) {
        this.id = Validaciones.texto(id, "El id de la categoría");
        setNombre(nombre);
        setDescripcion(descripcion);
    }

    /** @return Identificador de la categoría. */
    public String getId() {
        return id;
    }

    /** @return Nombre de la categoría. */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre Nuevo nombre.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setNombre(String nombre) {
        this.nombre = Validaciones.texto(nombre, "El nombre de la categoría");
    }

    /** @return Descripción, o cadena vacía si no tiene. */
    public String getDescripcion() {
        return descripcion;
    }

    /** @param descripcion Nueva descripción (opcional). */
    public final void setDescripcion(String descripcion) {
        this.descripcion = Validaciones.opcional(descripcion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Category) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
