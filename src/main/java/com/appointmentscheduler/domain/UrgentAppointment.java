package com.appointmentscheduler.domain;

/**
 * Represents an urgent appointment.
 */
public class UrgentAppointment extends Appointment {
    public UrgentAppointment(User patient, TimeSlot timeSlot) {
        super(patient, timeSlot);
    }

    public UrgentAppointment(String id, User patient, TimeSlot timeSlot) {
        super(id, patient, timeSlot);
    }
}
