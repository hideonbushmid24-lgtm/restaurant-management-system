package com.restaurant.model;

public class User {

    private final long id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final boolean active;

    public User(
            long id,
            String username,
            String passwordHash,
            String fullName,
            Role role,
            boolean active
    ) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}