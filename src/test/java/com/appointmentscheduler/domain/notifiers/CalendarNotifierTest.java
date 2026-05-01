package com.appointmentscheduler.domain.notifiers;

import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class CalendarNotifierTest {

    @Test
    void notify_nullUserNoOp() {
        CalendarNotifier n = new CalendarNotifier();
        assertThatCode(() -> n.notify(null, "msg")).doesNotThrowAnyException();
    }

    @Test
    void notify_withUserRuns() {
        CalendarNotifier n = new CalendarNotifier();
        User u = new User("id", "Name", "e@x.com", "p");
        assertThatCode(() -> n.notify(u, "hello")).doesNotThrowAnyException();
    }
}
