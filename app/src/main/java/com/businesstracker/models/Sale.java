package com.businesstracker.models;

public class Sale {
    private String id;
    private String businessId;
    private String productName;
    private String quantity; // Changed to String for record-keeping
    private String unitPrice; // Changed to String for record-keeping
    private double totalAmount; // This is the "Total Money Received"
    private String date;

    public Sale() {}

    public Sale(String id, String businessId, String productName, String quantity, String unitPrice, double totalAmount, String date) {
        this.id = id;
        this.businessId = businessId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getUnitPrice() { return unitPrice; }
    public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
