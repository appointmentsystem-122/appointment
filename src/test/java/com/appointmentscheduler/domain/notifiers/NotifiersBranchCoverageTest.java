package com.appointmentscheduler.domain.notifiers;

import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

/**
 * Covers null vs non-null branches in {@link SMSNotification} and {@link CalendarNotifier}.
 */
class NotifiersBranchCoverageTest {

    @Test
    void smsNotification_nullUser_logsEarly() {
        new SMSNotification().notify(null, "hello");
    }

    @Test
    void smsNotification_withUser() {
        new SMSNotification().notify(new User("1", "N", "e@e.com", "h"), "msg");
        new SMSNotification().notify(new User("2", "N2", "e2@e.com", "h"), null);
    }

    @Test
    void calendarNotifier_nullVsNonNull() {
        new CalendarNotifier().notify(null, "x");
        new CalendarNotifier().notify(new User("1", "N", "e@e.com", "h"), "y");
    }
}
