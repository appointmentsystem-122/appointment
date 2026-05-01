package com.appointmentscheduler.domain.notifiers;

import com.appointmentscheduler.domain.User;

/**
 * Concrete observer that creates calendar events.
 */
public class CalendarNotifier implements Observer {
    
    @Override
    public void notify(User user, String message) {
        if (user != null) {
            System.out.println("[CalendarNotifier] Adding calendar event for " + user.getName() + ": " + message);
            // In a real system, invoke a calendar API representation.
        }
    }
}
