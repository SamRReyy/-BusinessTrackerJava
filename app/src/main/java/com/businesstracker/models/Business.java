package com.businesstracker.models;

public class Business {
    private String id;
    private String name;
    private String description;
    private double targetBudget;
    private double currentSpent;
    private double totalRevenue; // New field for profit calculation
    private int sellQuantity;    // Quantity sold for profit estimation
    private double unitPrice;    // Unit price for profit estimation
    private String color;
    private String status; // "now", "incoming", "done"
    private String createdAt;
    private boolean archived;

    public Business() {}

    public Business(String id, String name, String description,
                    double targetBudget, double currentSpent,
                    String color, String status, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.targetBudget = targetBudget;
        this.currentSpent = currentSpent;
        this.totalRevenue = 0;
        this.sellQuantity = 0;
        this.unitPrice = 0;
        this.color = color;
        this.status = status;
        this.createdAt = createdAt;
        this.archived = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTargetBudget() { return targetBudget; }
    public void setTargetBudget(double targetBudget) { this.targetBudget = targetBudget; }

    public double getCurrentSpent() { return currentSpent; }
    public void setCurrentSpent(double currentSpent) { this.currentSpent = currentSpent; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getSellQuantity() { return sellQuantity; }
    public void setSellQuantity(int sellQuantity) { this.sellQuantity = sellQuantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getProfit() {
        return totalRevenue - currentSpent;
    }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public double getProgressPercent() {
        if (targetBudget <= 0) return 0;
        return (currentSpent / targetBudget) * 100.0;
    }

    public double getRemaining() {
        return Math.max(0, targetBudget - currentSpent);
    }

    public boolean isOverBudget() {
        return getProgressPercent() > 100;
    }

    public boolean isNearBudget() {
        double p = getProgressPercent();
        return p >= 90 && p <= 100;
    }
}
