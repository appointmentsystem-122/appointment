package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.notifiers.Observer;
import com.appointmentscheduler.domain.notifiers.SMSNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

    @Test
    void sendReminder_notifiesSingleMockObserverWithCorrectArguments() {
        Observer observer = mock(Observer.class);
        AppointmentReminderService service = new AppointmentReminderService();
        User user = new User("u1", "Test User", "test@example.com", "secret");
        String message = "Your appointment is tomorrow at 10:00.";

        service.registerObserver(observer);
        service.sendReminder(user, message);

        verify(observer, times(1)).notify(eq(user), eq(message));
    }

    @Test
    void sendReminder_notifiesMultipleObserversTogether() {
        SMSNotification sms1 = spy(new SMSNotification());
        SMSNotification sms2 = spy(new SMSNotification());
        AppointmentReminderService service = new AppointmentReminderService();
        User user = new User("u2", "Jane Doe", "jane@example.com", "pwd");
        String message = "Reminder: check-up in one hour.";

        service.registerObserver(sms1);
        service.registerObserver(sms2);
        service.sendReminder(user, message);

        verify(sms1, times(1)).notify(eq(user), eq(message));
        verify(sms2, times(1)).notify(eq(user), eq(message));
    }

    @Test
    void sendReminder_oneChannelThrows_otherStillNotified() {
        Observer good = mock(Observer.class);
        Observer bad = mock(Observer.class);
        doThrow(new RuntimeException("smtp down")).when(bad).notify(any(), any());

        AppointmentReminderService service = new AppointmentReminderService();
        User user = new User("u3", "Bob", "bob@example.com", "p");
        service.registerObserver(bad);
        service.registerObserver(good);
        service.sendReminder(user, "msg");

        verify(good, times(1)).notify(eq(user), eq("msg"));
    }

    @Test
    void registerObserver_null_isIgnored() {
        AppointmentReminderService service = new AppointmentReminderService();
        service.registerObserver(null);
        Observer o = mock(Observer.class);
        User user = new User("u-null", "N", "n@x.com", "p");
        service.registerObserver(o);
        service.sendReminder(user, "m");
        verify(o, times(1)).notify(eq(user), eq("m"));
    }

    @Test
    void registerObserver_sameInstanceTwice_notifiesOnce() {
        Observer o = mock(Observer.class);
        AppointmentReminderService service = new AppointmentReminderService();
        User user = new User("u-dup", "D", "d@x.com", "p");
        service.registerObserver(o);
        service.registerObserver(o);
        service.sendReminder(user, "once");
        verify(o, times(1)).notify(eq(user), eq("once"));
    }

    @Test
    void unregisterObserver_null_isSafe() {
        AppointmentReminderService service = new AppointmentReminderService();
        service.unregisterObserver(null);
    }

    @Test
    void unregisterObserver_removesChannel() {
        Observer o = mock(Observer.class);
        AppointmentReminderService service = new AppointmentReminderService();
        User user = new User("u-unreg", "U", "u@x.com", "p");
        service.registerObserver(o);
        service.unregisterObserver(o);
        service.sendReminder(user, "m");
        verifyNoInteractions(o);
    }

    @Test
    void sendReminder_nullUser_throws() {
        AppointmentReminderService service = new AppointmentReminderService();
        assertThatThrownBy(() -> service.sendReminder(null, "m"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("user");
    }

    @Test
    void sendReminder_nullMessage_throws() {
        AppointmentReminderService service = new AppointmentReminderService();
        User user = new User("u-nomsg", "N", "nomsg@x.com", "p");
        assertThatThrownBy(() -> service.sendReminder(user, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("message");
    }
}
