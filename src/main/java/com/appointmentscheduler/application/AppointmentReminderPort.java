package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.notifiers.Observer;

/**
 * Application port (hexagonal): outbound contract for appointment reminders.
 * Implementations dispatch to one or more {@link Observer} channels (email, SMS, push, etc.).
 */
public interface AppointmentReminderPort {

    void registerObserver(Observer observer);

    void unregisterObserver(Observer observer);

    void sendReminder(User user, String message);
}
