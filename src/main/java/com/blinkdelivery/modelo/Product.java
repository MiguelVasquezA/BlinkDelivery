package com.blinkdelivery.modelo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Producto (plato o bebida) del menú de un {@link Restaurant}.
 *
 * @author Miguel Vasquez
 * @version 2.0
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String nombre;
    private String descripcion;
    private double precio;
    private boolean disponible;
    private Category categoria;
    private final String restaurantId;

    /**
     * Crea un producto disponible.
     *
     * @param id           Identificador único (obligatorio).
     * @param nombre       Nombre comercial (obligatorio).
     * @param descripcion  Descripción (obligatoria).
     * @param precio       Precio unitario (no negativo).
     * @param categoria    Categoría del menú (opcional, puede ser {@code null}).
     * @param restaurantId Id del restaurante propietario (obligatorio).
     * @throws IllegalArgumentException Si algún dato obligatorio es nulo, vacío o el precio es negativo.
     */
    public Product(String id, String nombre, String descripcion, double precio,
                   Category categoria, String restaurantId) {
        this.id = Validaciones.texto(id, "El id del producto");
        this.restaurantId = Validaciones.texto(restaurantId, "El id del restaurante del producto");
        setNombre(nombre);
        setDescripcion(descripcion);
        setPrecio(precio);
        setCategoria(categoria);
        this.disponible = true;
    }

    /**
     * Constructor de compatibilidad usado por la persistencia en archivo plano
     * (producto disponible y sin categoría).
     *
     * @param id           Identificador único.
     * @param restaurantId Id del restaurante propietario.
     * @param name         Nombre comercial.
     * @param description  Descripción.
     * @param price        Precio unitario (no negativo).
     * @throws IllegalArgumentException Si algún dato es nulo, vacío o el precio es negativo.
     */
    public Product(String id, String restaurantId, String name, String description, double price) {
        this(id, name, description, price, null, restaurantId);
    }

    /** @return Identificador del producto. */
    public String getId() {
        return id;
    }

    /** @return Nombre comercial del producto. */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre Nuevo nombre.
     * @throws IllegalArgumentException Si es nulo o vacío.
     */
    public final void setNombre(String nombre) {
        this.nombre = Validaciones.texto(nombre, "El nombre del producto");
    }

    /** @return Descripción del producto. */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * @param descripcion Nueva descripción.
     * @throws IllegalArgumentException Si es nula o vacía.
     */
    public final void setDescripcion(String descripcion) {
        this.descripcion = Validaciones.texto(descripcion, "La descripción del producto");
    }

    /** @return Precio unitario vigente en el catálogo. */
    public double getPrecio() {
        return precio;
    }

    /**
     * Cambia el precio de catálogo. No afecta a los pedidos ya creados, porque
     * {@link OrderItem} congela el precio al momento de la compra.
     *
     * @param precio Nuevo precio (no negativo).
     * @throws IllegalArgumentException Si es negativo.
     */
    public final void setPrecio(double precio) {
        this.precio = Validaciones.noNegativo(precio, "El precio del producto");
    }

    /** @return {@code true} si el producto se puede pedir. */
    public boolean isDisponible() {
        return disponible;
    }

    /** @param disponible Nueva disponibilidad. */
    public final void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    /** @return Categoría del menú, o {@code null} si no tiene. */
    public Category getCategoria() {
        return categoria;
    }

    /** @param categoria Nueva categoría ({@code null} lo deja sin categoría). */
    public final void setCategoria(Category categoria) {
        this.categoria = categoria;
    }

    /** @return Id del restaurante propietario. */
    public String getRestaurantId() {
        return restaurantId;
    }

    // ---- Alias de compatibilidad con las capas dao/service existentes ----

    /** @return Alias de {@link #getNombre()}. */
    public String getName() {
        return getNombre();
    }

    /** @return Alias de {@link #getDescripcion()}. */
    public String getDescription() {
        return getDescripcion();
    }

    /** @return Alias de {@link #getPrecio()}. */
    public double getPrice() {
        return getPrecio();
    }

    /** @return Alias de {@link #isDisponible()}. */
    public boolean isAvailable() {
        return isDisponible();
    }

    /** @param available Alias de {@link #setDisponible(boolean)}. */
    public final void setAvailable(boolean available) {
        setDisponible(available);
    }

    /**
     * Línea de texto plano usada por {@code ProductFileDao}:
     * {@code id;restaurantId;nombre;descripcion;precio;disponible}.
     *
     * @return Registro delimitado por punto y coma.
     */
    public String toRegistro() {
        return id + ";" + restaurantId + ";" + nombre + ";" + descripcion + ";" + precio + ";" + disponible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((Product) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre + " - " + String.format("$%,.0f", precio) + (disponible ? "" : " (agotado)");
    }
}
