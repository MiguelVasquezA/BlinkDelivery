package com.blinkdelivery.modelo;

/**
 * Representa un establecimiento aliado registrado en el sistema.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class Restaurant {
    private String id;
    private String name;
    private String category;
    private boolean open;

    /**
     * Registra un restaurante. Por defecto, inicia en estado abierto.
     *
     * @param id Identificador único del restaurante.
     * @param name Nombre comercial del restaurante.
     * @param category Tipo o categoría de comida que ofrece.
     */
    public Restaurant(String id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.open = true;
    }

    /** @return El identificador del restaurante. */
    public String getId() { return id; }

    /** @param id El nuevo identificador del restaurante. */
    public void setId(String id) { this.id = id; }

    /** @return El nombre del restaurante. */
    public String getName() { return name; }

    /** @param name El nuevo nombre comercial del restaurante. */
    public void setName(String name) { this.name = name; }

    /** @return La categoría del restaurante. */
    public String getCategory() { return category; }

    /** @param category La nueva categoría del restaurante. */
    public void setCategory(String category) { this.category = category; }

    /** @return {@code true} si el restaurante está abierto para recibir pedidos; {@code false} de lo contrario. */
    public boolean isOpen() { return open; }

    /** @param open El estado de apertura del restaurante. */
    public void setOpen(boolean open) { this.open = open; }

    /**
     * Convierte los atributos del restaurante a una cadena delimitada para archivo de texto.
     *
     * @return Cadena con la información del restaurante.
     */
    @Override
    public String toString() {
        return id + ";" + name + ";" + category + ";" + open;
    }
}
