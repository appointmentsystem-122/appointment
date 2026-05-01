package com.appointmentscheduler.domain;

/**
 * Represents a receptionist user. Can manage appointments and export data.
 */
public class ReceptionistUser extends User {

    public ReceptionistUser(String id, String name, String email, String password) {
        super(id, name, email, password);
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}
