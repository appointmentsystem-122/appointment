package com.appointmentscheduler.domain;

/**
 * Represents a doctor user. Can view and manage appointments within their scope.
 */
public class DoctorUser extends User {

    public DoctorUser(String id, String name, String email, String password) {
        super(id, name, email, password);
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}
