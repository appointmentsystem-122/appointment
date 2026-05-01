package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.notifiers.CalendarNotifier;
import com.appointmentscheduler.domain.notifiers.Observer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    @Test
    void attachDetachAndNotify() {
        NotificationService svc = new NotificationService();
        AtomicInteger calls = new AtomicInteger();
        Observer o = (u, m) -> calls.incrementAndGet();
        svc.attach(o);
        svc.attach(o);
        User u = new User("1", "N", "e@x.com", "x");
        svc.notifyAllObservers(u, "hello");
        assertThat(calls.get()).isEqualTo(1);
        svc.sendAppointmentReminder(u, "soon");
        assertThat(calls.get()).isEqualTo(2);
        svc.detach(o);
        svc.notifyAllObservers(u, "x");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void concreteNotifiers_coverBranches() {
        User u = new User("1", "N", "e@x.com", "x");
        new CalendarNotifier().notify(u, "cal");
        new CalendarNotifier().notify(null, "x");
    }

    @Test
    void calendarNotifier_nullUser_isNoOp() {
        assertThatCode(() -> new CalendarNotifier().notify(null, "x")).doesNotThrowAnyException();
    }

    @Test
    void sendAppointmentReminder_withPort_delegatesToPortWithPrefixedMessage() {
        AppointmentReminderPort port = mock(AppointmentReminderPort.class);
        NotificationService svc = new NotificationService(port);
        User u = new User("1", "N", "e@x.com", "x");
        svc.sendAppointmentReminder(u, "soon");
        verify(port).sendReminder(eq(u), eq("REMINDER: soon"));
    }

    @Test
    void notify_nullUser_stillRecordsMessage() {
        NotificationService svc = new NotificationService();
        svc.notify(null, "broadcast");
        assertThat(svc.getSentMessages()).hasSize(1);
        assertThat(svc.getSentMessages().get(0).getContent()).isEqualTo("broadcast");
    }

}
