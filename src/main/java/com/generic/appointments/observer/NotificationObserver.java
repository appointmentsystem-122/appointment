package com.generic.appointments.observer;

/**
 * Observer for appointment-related notifications.
 */
public interface NotificationObserver {

    void onNotification(NotificationEvent event);
}

