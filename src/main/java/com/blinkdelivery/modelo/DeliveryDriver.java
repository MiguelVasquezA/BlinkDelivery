package com.blinkdelivery.modelo;

public class DeliveryDriver {
    private String identification;
    private String name;
    private String phone;
    private String vehicleType;
    private boolean available;

    public DeliveryDriver(String identification, String name, String phone, String vehicleType) {
        this.identification = identification;
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.available = true;
    }

    public String getIdentification() { return identification; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getVehicleType() { return vehicleType; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return identification + ";" + name + ";" + phone + ";" + vehicleType + ";" + available;
    }
}
