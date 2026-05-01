package com.appointmentscheduler.domain;

/**
 * Represents an administrator variant of the User in the system.
 * An administrator has special capabilities such as managing reservations
 * of lower-privileged users.
 */
public class Administrator extends User {

    /**
     * Constructs a new Administrator.
     *
     * @param id       the unique identifier
     * @param name     the full name
     * @param email    the email address
     * @param password the password
     */
    public Administrator(String id, String name, String email, String password) {
        super(id, name, email, password);
    }

    /**
     * Indicates that this user is an administrator.
     *
     * @return always true for an Administrator.
     */
    @Override
    public boolean isAdmin() {
        return true;
    }
}
