package com.appointmentscheduler.domain.notifiers;

import com.appointmentscheduler.domain.User;

/**
 * Observer interface for the notification system.
 */
public interface Observer {
    
    /**
     * Notify the user with a message.
     * @param user the user to notify
     * @param message the notification message content
     */
    void notify(User user, String message);
}
