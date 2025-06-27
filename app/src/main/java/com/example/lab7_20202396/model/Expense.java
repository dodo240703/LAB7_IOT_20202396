package com.example.lab7_20202396.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos para los gastos
 */
public class Expense {

    private String id;
    private String title;
    private double amount;
    private String description;
    private Date date;
    private String userId;
    private String imageUrl;

    // Constructor vacío requerido por Firebase
    public Expense() {}

    // Constructor principal
    public Expense(String title, double amount, String description, long timestamp, String userId, String imageUrl) {
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = new Date(timestamp);
        this.userId = userId;
        this.imageUrl = imageUrl;
    }

    // Constructor adicional para Firestore (con ID como primer parámetro)
    public Expense(String id, String title, double amount, String description, long timestamp, String userId, String imageUrl) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = new Date(timestamp);
        this.userId = userId;
        this.imageUrl = imageUrl;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Convierte el objeto a un Map para Firebase
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("amount", amount);
        result.put("description", description);
        result.put("date", date);
        result.put("userId", userId);
        result.put("imageUrl", imageUrl);
        return result;
    }
}
