package com.appointmentscheduler.domain;

import com.appointmentscheduler.domain.notifiers.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentSendReminderTest {

    @Mock
    private Observer mockObserver;

    @Test
    void sendReminder_futureAppointment_notifiesObserver() {
        User patient = new User("p1", "Pat", "p@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        IndividualAppointment appointment = new IndividualAppointment(patient, slot);

        appointment.sendReminder(mockObserver);

        String expected = "Reminder: You have an appointment at " + start;
        verify(mockObserver).notify(eq(patient), eq(expected));
    }

    @Test
    void sendReminder_pastAppointment_doesNotNotify() {
        User patient = new User("p1", "Pat", "p@x.com", "x");
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        IndividualAppointment appointment = new IndividualAppointment(patient, slot);

        appointment.sendReminder(mockObserver);

        verify(mockObserver, never()).notify(any(), any());
    }
}
