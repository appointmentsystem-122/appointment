package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.notifiers.NotificationMessage;
import com.appointmentscheduler.domain.notifiers.Observer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service managing notifications using the Observer pattern.
 * Also implements {@link Observer} to simulate delivery (console + in-memory log).
 * Optional {@link AppointmentReminderPort}: when set, {@link #sendAppointmentReminder} delegates
 * to that port (dedicated reminder channels); otherwise falls back to the general observer list.
 */
public class NotificationService implements Observer {

    private final List<Observer> observers;
    private final AppointmentReminderPort appointmentReminderPort;
    private final List<NotificationMessage> sentMessages = new CopyOnWriteArrayList<>();

    public NotificationService() {
        this(null);
    }

    /**
     * @param appointmentReminderPort when non-null, reminder traffic uses this port (enterprise split).
     */
    public NotificationService(AppointmentReminderPort appointmentReminderPort) {
        this.observers = new ArrayList<>();
        this.appointmentReminderPort = appointmentReminderPort;
    }

    @Override
    public void notify(User user, String message) {
        LocalDateTime sendTime = LocalDateTime.now();
        sentMessages.add(new NotificationMessage(message, sendTime));
        String who = user != null ? user.getId() : "?";
        System.out.println("[Notification] user=" + who + " @ " + sendTime + " -> " + message);
    }

    /** In-memory log of notifications handled by this service as an {@link Observer}. */
    public List<NotificationMessage> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    /**
     * Register a new observer.
     * @param observer the observer to register
     */
    public void attach(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Remove an existing observer.
     * @param observer the observer to remove
     */
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers.
     * @param user the user receiving the notification
     * @param message the message content
     */
    public void notifyAllObservers(User user, String message) {
        for (Observer observer : observers) {
            observer.notify(user, message);
        }
    }

    /**
     * Specialized method for appointment reminders.
     * @param user the user to remind
     * @param message the specific reminder content
     */
    public void sendAppointmentReminder(User user, String message) {
        String body = "REMINDER: " + message;
        if (appointmentReminderPort != null) {
            appointmentReminderPort.sendReminder(user, body);
        } else {
            notifyAllObservers(user, body);
        }
    }
}
