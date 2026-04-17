package com.businesstracker.models;

public class Task {
    private String id;
    private String businessId;
    private String title;
    private String description;
    private String dueDate;
    private boolean completed;
    private boolean notified;
    private String photo;

    public Task() {}

    public Task(String id, String businessId, String title,
                String description, String dueDate,
                boolean completed, boolean notified) {
        this.id = id;
        this.businessId = businessId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.completed = completed;
        this.notified = notified;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
}
