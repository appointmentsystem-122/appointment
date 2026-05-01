package com.generic.appointments.repository;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.impl.InMemoryAppointmentRepository;
import com.generic.appointments.repository.impl.InMemoryTimeSlotRepository;
import com.generic.appointments.repository.impl.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericInMemoryRepositoryTest {

    @Test
    void appointmentRepository_findByCustomerAndTimeSlot() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        Customer c = new Customer("c1", "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 7, 1, 10, 0);
        TimeSlot slot = new TimeSlot("ts1", s, s.plusHours(1), false);
        Appointment a = new Appointment("a1", c, slot, AppointmentStatus.CONFIRMED);
        repo.save(a);
        assertThat(repo.findById("a1")).isPresent();
        assertThat(repo.findByCustomer(c)).hasSize(1);
        assertThat(repo.findByTimeSlot(slot)).hasSize(1);
        assertThat(repo.findAll()).hasSize(1);
    }

    @Test
    void timeSlotRepository_findAvailableBetween() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        LocalDateTime s = LocalDateTime.of(2026, 8, 1, 9, 0);
        TimeSlot open = new TimeSlot(null, s, s.plusHours(1), true);
        repo.save(open);
        List<TimeSlot> list = repo.findAvailableBetween(s.minusHours(1), s.plusHours(3));
        assertThat(list).contains(open);
    }

    @Test
    void userRepository_findByEmail() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        Customer u = new Customer(null, "U", "find@x.com");
        repo.save(u);
        assertThat(repo.findByEmail("FIND@x.com")).contains(u);
        assertThat(repo.findById(u.getId())).contains(u);
    }

    @Test
    void userRepository_emailNotFound_returnsEmpty() {
        InMemoryUserRepository repo = new InMemoryUserRepository();
        Customer u = new Customer(null, "U", "find@x.com");
        repo.save(u);
        assertThat(repo.findByEmail("missing@x.com")).isEmpty();
    }

    @Test
    void appointmentRepository_findByCustomer_emptyWhenNoMatch() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        Customer c1 = new Customer("c1", "C", "c@x.com");
        Customer c2 = new Customer("c2", "D", "d@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 7, 2, 10, 0);
        TimeSlot slot = new TimeSlot("ts1", s, s.plusHours(1), false);
        Appointment a = new Appointment("a1", c1, slot, AppointmentStatus.CONFIRMED);
        repo.save(a);
        assertThat(repo.findByCustomer(c2)).isEmpty();
    }

    @Test
    void appointmentRepository_findByTimeSlot_emptyWhenSlotIdMismatch() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        Customer c = new Customer("c1", "C", "c@x.com");
        LocalDateTime s = LocalDateTime.of(2026, 7, 3, 11, 0);
        TimeSlot slotUsed = new TimeSlot("ts1", s, s.plusHours(1), false);
        Appointment a = new Appointment("a1", c, slotUsed, AppointmentStatus.CONFIRMED);
        repo.save(a);
        TimeSlot otherSlot = new TimeSlot("ts-other", s, s.plusHours(1), false);
        assertThat(repo.findByTimeSlot(otherSlot)).isEmpty();
    }

    @Test
    void appointmentRepository_findById_missing_returnsEmpty() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        assertThat(repo.findById("nope")).isEmpty();
    }

    @Test
    void timeSlotRepository_filtersAvailabilityAndWindow() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        LocalDateTime day = LocalDateTime.of(2026, 9, 1, 8, 0);
        TimeSlot booked = new TimeSlot(null, day.plusHours(1), day.plusHours(2), false);
        repo.save(booked);
        TimeSlot open = new TimeSlot(null, day.plusHours(3), day.plusHours(4), true);
        repo.save(open);
        TimeSlot outside = new TimeSlot(null, day.plusDays(1), day.plusDays(1).plusHours(1), true);
        repo.save(outside);
        assertThat(repo.findAvailableBetween(day, day.plusHours(23))).containsExactly(open);
        assertThat(repo.findById(open.getId())).contains(open);
        assertThat(repo.findAll()).hasSize(3);
    }

    @Test
    void timeSlotRepository_excludesWhenEndAfterWindow() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 1, 12, 0);
        TimeSlot spills = new TimeSlot(null, from.plusHours(1), to.plusHours(1), true);
        repo.save(spills);
        assertThat(repo.findAvailableBetween(from, to)).isEmpty();
    }

    @Test
    void timeSlotRepository_findAvailableBetween_inclusiveStartAndEnd() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 1, 11, 0);
        TimeSlot exactWindow = new TimeSlot("tw", from, to, true);
        repo.save(exactWindow);
        assertThat(repo.findAvailableBetween(from, to)).containsExactly(exactWindow);
    }

    @Test
    void timeSlotRepository_findById_missing_returnsEmpty() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        assertThat(repo.findById("missing")).isEmpty();
    }

    @Test
    void timeSlotRepository_excludesWhenStartsBeforeWindow() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        LocalDateTime from = LocalDateTime.of(2026, 12, 1, 10, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 1, 12, 0);
        TimeSlot earlyStart = new TimeSlot(null, from.minusMinutes(30), from.plusHours(1), true);
        repo.save(earlyStart);
        assertThat(repo.findAvailableBetween(from, to)).isEmpty();
    }
}
