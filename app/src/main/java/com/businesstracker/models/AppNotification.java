package com.businesstracker.models;

public class AppNotification {
    private String id;
    private String type; // "budget", "task", "info"
    private String title;
    private String message;
    private boolean read;
    private String createdAt;

    public AppNotification() {}

    public AppNotification(String id, String type, String title,
                           String message, boolean read, String createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
