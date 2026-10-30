package com.blinkdelivery.modelo;

public class Product {
    private String id;
    private String restaurantId;
    private String name;
    private String description;
    private double price;
    private boolean available;

    public Product(String id, String restaurantId, String name, String description, double price) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = true;
    }

    public String getId() { return id; }
    public String getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return id + ";" + restaurantId + ";" + name + ";" + description + ";" + price + ";" + available;
    }
}
