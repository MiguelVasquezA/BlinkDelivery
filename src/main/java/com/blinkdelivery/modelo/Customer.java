package com.blinkdelivery.modelo;

public class Customer {
    private String identification;
    private String name;
    private String phone;
    private String address;

    public Customer(String identification, String name, String phone, String address) {
        this.identification = identification;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getIdentification() { return identification; }
    public void setIdentification(String identification) { this.identification = identification; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return identification + ";" + name + ";" + phone + ";" + address;
    }
}
