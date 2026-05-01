package com.appointmentscheduler.domain;

/**
 * Represents a follow-up appointment.
 * Requires a prior appointment (dependency).
 */
public class FollowUpAppointment extends Appointment {
    private final String priorAppointmentId;

    public FollowUpAppointment(User patient, TimeSlot timeSlot) {
        this(patient, timeSlot, null);
    }

    public FollowUpAppointment(User patient, TimeSlot timeSlot, String priorAppointmentId) {
        super(patient, timeSlot);
        this.priorAppointmentId = priorAppointmentId;
    }

    public FollowUpAppointment(String id, User patient, TimeSlot timeSlot, String priorAppointmentId) {
        super(id, patient, timeSlot);
        this.priorAppointmentId = priorAppointmentId;
    }

    public String getPriorAppointmentId() {
        return priorAppointmentId;
    }

    public boolean hasPriorAppointment() {
        return priorAppointmentId != null && !priorAppointmentId.isEmpty();
    }
}
