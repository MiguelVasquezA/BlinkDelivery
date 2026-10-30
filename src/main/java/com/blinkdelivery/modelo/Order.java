package com.blinkdelivery.modelo;

/**
 * Representa una solicitud de pedido realizada por un cliente a un restaurante.
 *
 * @author Miguel Vasquez
 * @version 1.0
 */
public class Order {
    private String id;
    private String customerId;
    private String restaurantId;
    private String productId;
    private int quantity;
    private double total;
    private String status;
    private String driverId;

    /**
     * Inicializa una nueva orden con estado base "Creado" y sin repartidor asignado.
     *
     * @param id Código único del pedido.
     * @param customerId Documento de identidad del cliente.
     * @param restaurantId Identificador del restaurante.
     * @param productId Identificador del producto solicitado.
     * @param quantity Cantidad solicitada.
     * @param total Valor total calculado a pagar.
     */
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

    /** @return Código identificador del pedido. */
    public String getId() { return id; }

    /** @return Identificación del cliente solicitante. */
    public String getCustomerId() { return customerId; }

    /** @return ID del restaurante de origen. */
    public String getRestaurantId() { return restaurantId; }

    /** @return ID del producto pedido. */
    public String getProductId() { return productId; }

    /** @return Unidades pedidas del producto. */
    public int getQuantity() { return quantity; }

    /** @return Precio total de la transacción. */
    public double getTotal() { return total; }

    /** @return Estado actual en el ciclo de vida del pedido. */
    public String getStatus() { return status; }

    /** @param status El nuevo estado del pedido. */
    public void setStatus(String status) { this.status = status; }

    /** @return Documento del domiciliario asignado. */
    public String getDriverId() { return driverId; }

    /** @param driverId El documento del nuevo domiciliario asignado. */
    public void setDriverId(String driverId) { this.driverId = driverId; }

    /**
     * Serializa los campos de la orden a texto separado por punto y coma.
     *
     * @return Cadena formateada para persistencia en disco.
     */
    @Override
    public String toString() {
        return id + ";" + customerId + ";" + restaurantId + ";" + productId + ";" + quantity + ";" + total + ";" + status + ";" + driverId;
    }
}
