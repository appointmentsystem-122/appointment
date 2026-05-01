package com.appointmentscheduler.domain;

import java.util.Objects;

/**
 * Represents a standard user in the appointment scheduling system.
 * This class serves as the base class for other user types like Administrator.
 */
public class User {
    private final String id;
    private final String name;
    private final String email;
    private final String password; // In a real system, this would be hashed.

    /**
     * Constructs a new User.
     *
     * @param id       the unique identifier for the user
     * @param name     the full name of the user
     * @param email    the email address of the user
     * @param password the login password
     */
    public User(String id, String name, String email, String password) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID cannot be null or empty");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name cannot be null or empty");
        if (email == null || email.isEmpty()) throw new IllegalArgumentException("Email cannot be null or empty");
        if (password == null || password.isEmpty()) throw new IllegalArgumentException("Password cannot be null or empty");
        
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    /**
     * Retrieves the user's ID.
     * @return the unique ID of the user.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the user's name.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the user's email.
     * @return the email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Retrieves the user's password.
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Checks if the user has administrative privileges.
     * @return true if admin, false otherwise.
     */
    public boolean isAdmin() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
