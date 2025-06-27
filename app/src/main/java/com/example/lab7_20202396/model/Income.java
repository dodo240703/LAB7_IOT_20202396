package com.example.lab7_20202396.model;

import java.util.Date;

public class Income {
    private String id;
    private String title;
    private double amount;
    private String description;
    private Date date;
    private String userId;

    public Income() {
        // Constructor vacío requerido para Firebase
    }

    public Income(String id, String title, double amount, String description, Date date, String userId) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.userId = userId;
    }

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
}
