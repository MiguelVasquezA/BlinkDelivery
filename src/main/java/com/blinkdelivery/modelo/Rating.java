package com.blinkdelivery.modelo;

public class Rating {
    private String orderId;
    private int score; // 1 a 5
    private String comment;

    public Rating(String orderId, int score, String comment) {
        this.orderId = orderId;
        this.score = score;
        this.comment = comment;
    }

    public String getOrderId() { return orderId; }
    public int getScore() { return score; }
    public String getComment() { return comment; }

    @Override
    public String toString() {
        return orderId + ";" + score + ";" + comment;
    }
}
