package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.notifiers.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of {@link AppointmentReminderPort}: Observer pattern subject with
 * thread-safe registration and per-channel fault isolation (one failing channel does not block others).
 */
public final class AppointmentReminderService implements AppointmentReminderPort {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderService.class);

    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    @Override
    public void registerObserver(Observer observer) {
        if (observer == null) {
            return;
        }
        if (!observers.contains(observer)) {
            observers.add(observer);
            log.debug("Registered reminder channel: {}", observer.getClass().getSimpleName());
        }
    }

    @Override
    public void unregisterObserver(Observer observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void sendReminder(User user, String message) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(message, "message");
        for (Observer observer : observers) {
            dispatchSafely(observer, user, message);
        }
    }

    private void dispatchSafely(Observer observer, User user, String message) {
        try {
            observer.notify(user, message);
        } catch (RuntimeException ex) {
            log.error("Reminder channel failed: " + observer.getClass().getName(), ex);
        }
    }
}
