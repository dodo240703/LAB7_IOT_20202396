package com.example.lab7_20202396.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo para representar un ingreso
 */
public class Income {
    private String id;
    private String title;
    private double amount;
    private String description;
    private Date date;  // Usar Date en lugar de long
    private String userId;

    // Constructor vacío requerido para Firebase
    public Income() {
    }

    public Income(String id, String title, double amount, String description, Date date, String userId) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.userId = userId;
    }

    // Constructor alternativo que acepta long para compatibilidad con código existente
    public Income(String id, String title, double amount, String description, long timestamp, String userId) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = new Date(timestamp);
        this.userId = userId;
    }

    // Constructor sin ID para cuando se crea un nuevo ingreso
    public Income(String title, double amount, String description, Date date, String userId) {
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.userId = userId;
    }

    // Constructor sin ID que acepta long para compatibilidad con código existente
    public Income(String title, double amount, String description, long timestamp, String userId) {
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = new Date(timestamp);
        this.userId = userId;
    }

    // Convertir a Map para Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("amount", amount);
        result.put("description", description);
        result.put("date", date);  // Firebase convertirá automáticamente Date a Timestamp
        result.put("userId", userId);
        return result;
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

    // Métodos adicionales para manejar conversiones de timestamp
    public long getDateAsTimestamp() {
        return date != null ? date.getTime() : 0;
    }

    // Renombrado para evitar conflictos con sobrecarga de setDate
    public void setDateFromTimestamp(long timestamp) {
        this.date = new Date(timestamp);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
