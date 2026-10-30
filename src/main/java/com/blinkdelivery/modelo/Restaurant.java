package com.blinkdelivery.modelo;

public class Restaurant {
    private String id;
    private String name;
    private String category;
    private boolean open;

    public Restaurant(String id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.open = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }

    @Override
    public String toString() {
        return id + ";" + name + ";" + category + ";" + open;
    }
}
