package com.blinkdelivery.modelo;

public class Order {
    private String id;
    private String customerId;
    private String restaurantId;
    private String productId;
    private int quantity;
    private double total;
    private String status;
    private String driverId;

    public Order(String id, String customerId, String restaurantId, String productId, int quantity, double total) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.productId = productId;
        this.quantity = quantity;
        this.total = total;
        this.status = "Creado";
        this.driverId = "Sin Asignar";
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getTotal() { return total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    @Override
    public String toString() {
        return id + ";" + customerId + ";" + restaurantId + ";" + productId + ";" + quantity + ";" + total + ";" + status + ";" + driverId;
    }
}
