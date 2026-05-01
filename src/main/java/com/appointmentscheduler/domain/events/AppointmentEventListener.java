package com.appointmentscheduler.domain.events;

/**
 * Observer interface for appointment lifecycle events.
 */
@FunctionalInterface
public interface AppointmentEventListener {
    void onAppointmentEvent(AppointmentEvent event);
}
