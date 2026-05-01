package com.generic.appointments.strategy;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.impl.InMemoryAppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericStrategyRulesTest {

    @Test
    void durationRule_invalidAndValid() {
        DurationRule rule = new DurationRule(Duration.ofHours(1));
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 1, 10, 9, 0);
        TimeSlot tooLong = new TimeSlot(null, s, s.plusHours(2), true);
        Appointment a1 = new Appointment(null, c, tooLong, AppointmentStatus.PENDING);
        assertThatThrownBy(() -> rule.validate(a1)).isInstanceOf(IllegalArgumentException.class);
        TimeSlot ok = new TimeSlot(null, s, s.plusMinutes(30), true);
        Appointment a2 = new Appointment(null, c, ok, AppointmentStatus.PENDING);
        assertThatCode(() -> rule.validate(a2)).doesNotThrowAnyException();
    }

    @Test
    void durationRule_exactlyMaxDuration_isValid() {
        DurationRule rule = new DurationRule(Duration.ofHours(1));
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 1, 11, 9, 0);
        TimeSlot exact = new TimeSlot(null, s, s.plusHours(1), true);
        Appointment a = new Appointment(null, c, exact, AppointmentStatus.PENDING);
        assertThatCode(() -> rule.validate(a)).doesNotThrowAnyException();
    }

    @Test
    void availabilityRule_slotUnavailable() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        AvailabilityRule rule = new AvailabilityRule(repo);
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 2, 10, 10, 0);
        TimeSlot slot = new TimeSlot(null, s, s.plusHours(1), false);
        Appointment a = new Appointment(null, c, slot, AppointmentStatus.PENDING);
        assertThatThrownBy(() -> rule.validate(a)).hasMessageContaining("not available");
    }

    @Test
    void availabilityRule_alreadyBooked() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 2, 11, 11, 0);
        TimeSlot slot = new TimeSlot("slot-x", s, s.plusHours(1), true);
        Appointment existing = new Appointment(null, c, slot, AppointmentStatus.CONFIRMED);
        repo.save(existing);
        AvailabilityRule rule = new AvailabilityRule(repo);
        Appointment candidate = new Appointment(null, c, slot, AppointmentStatus.PENDING);
        assertThatThrownBy(() -> rule.validate(candidate)).hasMessageContaining("already booked");
    }

    @Test
    void availabilityRule_cancelledDoesNotBlock() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 2, 12, 14, 0);
        TimeSlot slot = new TimeSlot("slot-y", s, s.plusHours(1), true);
        Appointment existing = new Appointment(null, c, slot, AppointmentStatus.CANCELLED);
        repo.save(existing);
        AvailabilityRule rule = new AvailabilityRule(repo);
        Appointment candidate = new Appointment(null, c, slot, AppointmentStatus.PENDING);
        assertThatCode(() -> rule.validate(candidate)).doesNotThrowAnyException();
    }

    @Test
    void compositeRule_iteratesAll() {
        CompositeBookingRule composite = new CompositeBookingRule()
                .addRule(new DurationRule(Duration.ofHours(2)));
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 3, 1, 9, 0);
        TimeSlot slot = new TimeSlot(null, s, s.plusMinutes(45), true);
        Appointment a = new Appointment(null, c, slot, AppointmentStatus.PENDING);
        assertThatCode(() -> composite.validate(a)).doesNotThrowAnyException();
    }

    @Test
    void compositeRule_emptyNoOp() {
        CompositeBookingRule composite = new CompositeBookingRule();
        Customer c = new Customer(null, "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 3, 2, 9, 0);
        TimeSlot slot = new TimeSlot(null, s, s.plusHours(1), true);
        Appointment a = new Appointment(null, c, slot, AppointmentStatus.PENDING);
        assertThatCode(() -> composite.validate(a)).doesNotThrowAnyException();
    }
}
