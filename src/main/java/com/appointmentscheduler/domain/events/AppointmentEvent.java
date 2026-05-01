package com.appointmentscheduler.domain.events;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.User;

import java.time.LocalDateTime;

/**
 * Event-driven notification trigger.
 * Observer-based appointment lifecycle events.
 */
public final class AppointmentEvent {

    public enum Type {
        CREATED,
        MODIFIED,
        CANCELLED,
        COMPLETED,
        REMINDER
    }

    private final Type type;
    private final Appointment appointment;
    private final User actor;
    private final LocalDateTime occurredAt;
    private final String details;

    public AppointmentEvent(Type type, Appointment appointment, User actor, String details) {
        this.type = type;
        this.appointment = appointment;
        this.actor = actor;
        this.occurredAt = LocalDateTime.now();
        this.details = details != null ? details : "";
    }

    public Type getType() {
        return type;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public User getActor() {
        return actor;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getDetails() {
        return details;
    }
}
