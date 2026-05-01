package com.appointmentscheduler.domain;

/**
 * Represents a group appointment.
 */
public class GroupAppointment extends Appointment {
    private int maxCapacity;

    public GroupAppointment(User patient, TimeSlot timeSlot, int maxCapacity) {
        super(patient, timeSlot);
        this.maxCapacity = maxCapacity;
    }

    public GroupAppointment(String id, User patient, TimeSlot timeSlot, int maxCapacity) {
        super(id, patient, timeSlot);
        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
}
