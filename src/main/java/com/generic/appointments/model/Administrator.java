package com.generic.appointments.model;

import java.util.Objects;

/**
 * Administrator (company / staff user with elevated permissions).
 */
public final class Administrator extends User {

    private final String username;
    private final String passwordHash; // In production this should be a secure hash

    public Administrator(String id, String name, String email,
                         String username, String passwordHash) {
        super(id, name, email);
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}

