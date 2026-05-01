package com.appointmentscheduler.presentation.fixtures;

import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;

/**
 * Concrete appointment whose simple class name is {@code "Appointment"} so
 * {@code getSimpleName().replace("Appointment", "")} yields an empty type label (subtitle branch).
 */
public final class Appointment extends IndividualAppointment {
    public Appointment(String id, User patient, TimeSlot timeSlot) {
        super(id, patient, timeSlot);
    }
}
