package com.generic.appointments.model;

/**
 * Customer / client of the business using the appointment system.
 */
public final class Customer extends User {

    public Customer(String id, String name, String email) {
        super(id, name, email);
    }
}

