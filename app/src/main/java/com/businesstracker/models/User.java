package com.businesstracker.models;

public class User {
    private String id;
    private String email;
    private String name;
    private String avatar;

    public User() {}

    public User(String id, String email, String name, String avatar) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.avatar = avatar;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getInitial() {
        if (name != null && !name.isEmpty()) {
            return String.valueOf(Character.toUpperCase(name.charAt(0)));
        }
        return "U";
    }
}
