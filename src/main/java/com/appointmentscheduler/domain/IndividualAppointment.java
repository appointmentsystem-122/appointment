package com.appointmentscheduler.domain;

/**
 * Represents an individual appointment.
 */
public class IndividualAppointment extends Appointment {
    public IndividualAppointment(User patient, TimeSlot timeSlot) {
        super(patient, timeSlot);
    }

    public IndividualAppointment(String id, User patient, TimeSlot timeSlot) {
        super(id, patient, timeSlot);
    }
}
