package com.appointmentscheduler.domain;

/**
 * Represents an assessment appointment.
 */
public class AssessmentAppointment extends Appointment {
    public AssessmentAppointment(User patient, TimeSlot timeSlot) {
        super(patient, timeSlot);
    }

    public AssessmentAppointment(String id, User patient, TimeSlot timeSlot) {
        super(id, patient, timeSlot);
    }
}
