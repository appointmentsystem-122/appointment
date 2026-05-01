package com.generic.appointments.service;

import com.generic.appointments.observer.NotificationEvent;
import com.generic.appointments.observer.NotificationObserver;

/**
 * Simple console-based observer implementation for demo and tests.
 */
public class ConsoleNotificationObserver implements NotificationObserver {

    @Override
    public void onNotification(NotificationEvent event) {
        System.out.printf(
            "[NOTIFY] Customer %s → appointment %s changed from %s to %s%n",
            event.getAppointment().getCustomer().getName(),
            event.getAppointment().getId(),
            event.getPreviousStatus(),
            event.getAppointment().getStatus()
        );
    }
}

