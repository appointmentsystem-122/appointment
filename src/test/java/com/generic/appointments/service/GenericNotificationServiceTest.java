package com.generic.appointments.service;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.observer.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GenericNotificationServiceTest {

    @Test
    void notifyAllInvokesObservers() {
        NotificationService svc = new NotificationService();
        AtomicInteger n = new AtomicInteger();
        svc.register(event -> n.incrementAndGet());
        Customer c = new Customer(null, "N", "e@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 6, 1, 10, 0);
        TimeSlot slot = new TimeSlot(null, s, s.plusHours(1), true);
        Appointment a = new Appointment(null, c, slot, AppointmentStatus.CONFIRMED);
        svc.notifyAll(new NotificationEvent(a, AppointmentStatus.PENDING));
        assertThat(n.get()).isEqualTo(1);
    }
}
