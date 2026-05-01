package com.generic.appointments.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Base user in the generic appointment system.
 * Extended by Administrator and Customer.
 */
public abstract class User {

    private final String id;
    private final String name;
    private final String email;

    protected User(String id, String name, String email) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = Objects.requireNonNull(name, "name");
        this.email = Objects.requireNonNull(email, "email");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}

