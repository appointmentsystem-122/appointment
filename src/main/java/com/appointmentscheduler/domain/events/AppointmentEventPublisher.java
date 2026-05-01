package com.appointmentscheduler.domain.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Event-driven publisher for appointment lifecycle events.
 * Observers subscribe to receive events (booking, modify, cancel, reminder).
 */
public class AppointmentEventPublisher {

    private final List<AppointmentEventListener> listeners = new ArrayList<>();

    public void addListener(AppointmentEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(AppointmentEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AppointmentEvent event) {
        for (AppointmentEventListener listener : new ArrayList<>(listeners)) {
            listener.onAppointmentEvent(event);
        }
    }
}
