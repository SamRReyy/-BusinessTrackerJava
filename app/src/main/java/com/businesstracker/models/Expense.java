package com.businesstracker.models;

public class Expense {
    private String id;
    private String businessId;
    private String type;
    private double amount;
    private String date;
    private String description;
    private String photo; // base64 or URI string

    public Expense() {}

    public Expense(String id, String businessId, String type,
                   double amount, String date, String description) {
        this.id = id;
        this.businessId = businessId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
}
