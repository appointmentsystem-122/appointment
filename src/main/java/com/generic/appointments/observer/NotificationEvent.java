package com.generic.appointments.observer;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;

/**
 * Event fired when an appointment changes state.
 */
public final class NotificationEvent {

    private final Appointment appointment;
    private final AppointmentStatus previousStatus;

    public NotificationEvent(Appointment appointment, AppointmentStatus previousStatus) {
        this.appointment = appointment;
        this.previousStatus = previousStatus;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public AppointmentStatus getPreviousStatus() {
        return previousStatus;
    }
}

