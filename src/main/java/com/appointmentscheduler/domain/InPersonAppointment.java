package com.appointmentscheduler.domain;

/**
 * Represents an in-person appointment.
 */
public class InPersonAppointment extends Appointment {
    private String location;

    public InPersonAppointment(User patient, TimeSlot timeSlot, String location) {
        super(patient, timeSlot);
        this.location = location;
    }

    public InPersonAppointment(String id, User patient, TimeSlot timeSlot, String location) {
        super(id, patient, timeSlot);
        this.location = location != null ? location : "";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
