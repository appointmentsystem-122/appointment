package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.events.AppointmentEvent;
import com.appointmentscheduler.domain.events.AppointmentEventListener;

/**
 * Bridges appointment events to notification triggers.
 * Event-driven approach: booking creation, modification, cancellation, reminder.
 */
public class NotificationEventBridge implements AppointmentEventListener {

    private final NotificationService notificationService;

    public NotificationEventBridge(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onAppointmentEvent(AppointmentEvent event) {
        if (event == null || event.getAppointment() == null) return;
        String msg = buildMessage(event);
        notificationService.notifyAllObservers(event.getAppointment().getPatient(), msg);
    }

    private String buildMessage(AppointmentEvent event) {
        switch (event.getType()) {
            case CREATED:
                return "Your appointment has been CONFIRMED for " + event.getAppointment().getTimeSlot();
            case MODIFIED:
                return "Your appointment has been MODIFIED to " + event.getDetails();
            case CANCELLED:
                return "Your appointment has been CANCELLED.";
            case COMPLETED:
                return "Your appointment has been marked COMPLETED.";
            case REMINDER:
                return "REMINDER: " + event.getDetails();
            default:
                return "Appointment update: " + event.getDetails();
        }
    }
}
