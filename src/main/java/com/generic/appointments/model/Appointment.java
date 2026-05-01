package com.generic.appointments.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A booking between a customer and a time slot.
 */
public final class Appointment {

    private final String id;
    private final Customer customer;
    private final TimeSlot timeSlot;
    private AppointmentStatus status;

    public Appointment(String id, Customer customer, TimeSlot timeSlot, AppointmentStatus status) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.customer = Objects.requireNonNull(customer, "customer");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }
}

