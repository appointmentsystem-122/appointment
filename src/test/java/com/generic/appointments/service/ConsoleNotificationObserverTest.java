package com.generic.appointments.service;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.observer.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleNotificationObserverTest {

    @Test
    void printsLine() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream old = System.out;
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            Customer c = new Customer(null, "Pat", "p@x.com");
            LocalDateTime s = LocalDateTime.of(2026, 4, 1, 12, 0);
            TimeSlot slot = new TimeSlot(null, s, s.plusHours(1), true);
            Appointment a = new Appointment("aid", c, slot, AppointmentStatus.CONFIRMED);
            new ConsoleNotificationObserver().onNotification(new NotificationEvent(a, AppointmentStatus.PENDING));
        } finally {
            System.setOut(old);
        }
        assertThat(buf.toString(StandardCharsets.UTF_8)).contains("NOTIFY").contains("Pat");
    }
}
