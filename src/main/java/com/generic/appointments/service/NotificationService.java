package com.generic.appointments.service;

import com.generic.appointments.observer.NotificationEvent;
import com.generic.appointments.observer.NotificationObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Publishes appointment notifications to registered observers.
 */
public class NotificationService {

    private final List<NotificationObserver> observers = new ArrayList<>();

    public void register(NotificationObserver observer) {
        observers.add(observer);
    }

    public void notifyAll(NotificationEvent event) {
        for (NotificationObserver observer : observers) {
            observer.onNotification(event);
        }
    }
}

