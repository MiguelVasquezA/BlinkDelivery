package com.blinkdelivery.modelo;

/**
 * Representa un artículo o producto perteneciente al menú de un restaurante.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class Product {
    private String id;
    private String restaurantId;
    private String name;
    private String description;
    private double price;
    private boolean available;

    /**
     * Crea un nuevo producto disponible comercialmente.
     *
     * @param id Código identificador único del producto.
     * @param restaurantId ID del restaurante al que pertenece el producto.
     * @param name Nombre del plato o bebida.
     * @param description Breve descripción del producto.
     * @param price Precio de venta en moneda local.
     */
    public Product(String id, String restaurantId, String name, String description, double price) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = true;
    }

    /** @return El identificador del producto. */
    public String getId() { return id; }

    /** @return El ID del restaurante propietario del producto. */
    public String getRestaurantId() { return restaurantId; }

    /** @return El nombre comercial del producto. */
    public String getName() { return name; }

    /** @return La descripción del producto. */
    public String getDescription() { return description; }

    /** @return El precio unitario del producto. */
    public double getPrice() { return price; }

    /** @return {@code true} si el producto está disponible en inventario; {@code false} de lo contrario. */
    public boolean isAvailable() { return available; }

    /** @param available El nuevo estado de disponibilidad del producto. */
    public void setAvailable(boolean available) { this.available = available; }

    /**
     * Formatea los atributos del producto separados por punto y coma.
     *
     * @return Cadena serializada del producto.
     */
    @Override
    public String toString() {
        return id + ";" + restaurantId + ";" + name + ";" + description + ";" + price + ";" + available;
    }
}
