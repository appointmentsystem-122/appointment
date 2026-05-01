package com.generic.appointments.service;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.observer.NotificationEvent;
import com.generic.appointments.observer.NotificationObserver;
import com.generic.appointments.repository.impl.InMemoryAppointmentRepository;
import com.generic.appointments.repository.impl.InMemoryTimeSlotRepository;
import com.generic.appointments.strategy.AvailabilityRule;
import com.generic.appointments.strategy.CompositeBookingRule;
import com.generic.appointments.strategy.DurationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for the generic AppointmentService.
 */
class AppointmentServiceTest {

    private InMemoryAppointmentRepository apptRepo;
    private InMemoryTimeSlotRepository slotRepo;
    private AppointmentService appointmentService;
    private Customer customer;

    @BeforeEach
    void setUp() {
        apptRepo = new InMemoryAppointmentRepository();
        slotRepo = new InMemoryTimeSlotRepository();

        NotificationService notificationService = new NotificationService();
        notificationService.register(new NotificationObserver() {
            @Override
            public void onNotification(NotificationEvent event) {
                // no-op for tests, just ensure notifications don't throw
            }
        });

        CompositeBookingRule bookingRule = new CompositeBookingRule()
            .addRule(new DurationRule(Duration.ofHours(2)))
            .addRule(new AvailabilityRule(apptRepo));

        appointmentService = new AppointmentService(apptRepo, bookingRule, notificationService);
        customer = new Customer(null, "Test Customer", "test@example.com");
    }

    @Test
    void bookAppointment_success() {
        TimeSlot slot = new TimeSlot(null,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(2),
            true);
        slotRepo.save(slot);

        Appointment appt = appointmentService.bookAppointment(customer, slot);

        assertNotNull(appt.getId());
        assertEquals(AppointmentStatus.CONFIRMED, appt.getStatus());
        assertFalse(slot.isAvailable());
    }

    @Test
    void cancelAppointment_freesSlot() {
        TimeSlot slot = new TimeSlot(null,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(2),
            true);
        slotRepo.save(slot);

        Appointment appt = appointmentService.bookAppointment(customer, slot);
        appointmentService.cancelAppointment(appt.getId());

        Appointment persisted = apptRepo.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, persisted.getStatus());
        assertTrue(persisted.getTimeSlot().isAvailable());
    }

    @Test
    void preventDoubleBooking_sameSlot() {
        TimeSlot slot = new TimeSlot(null,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(2),
            true);
        slotRepo.save(slot);

        appointmentService.bookAppointment(customer, slot);

        Customer other = new Customer(null, "Other", "other@example.com");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> appointmentService.bookAppointment(other, slot));

        String msg = ex.getMessage().toLowerCase();
        // After first booking the slot is marked unavailable; validation may fail before "already booked"
        assertTrue(msg.contains("already booked") || msg.contains("not available"),
                () -> "unexpected message: " + ex.getMessage());
    }

    @Test
    void cancelAppointment_notFound_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> appointmentService.cancelAppointment("no-such-id"));
    }

    @Test
    void findAll_and_findByCustomer() {
        assertTrue(appointmentService.findAll().isEmpty());
        TimeSlot slot = new TimeSlot(null,
                LocalDateTime.now().plusHours(3),
                LocalDateTime.now().plusHours(4),
                true);
        slotRepo.save(slot);
        Appointment booked = appointmentService.bookAppointment(customer, slot);
        assertFalse(appointmentService.findAll().isEmpty());
        assertFalse(appointmentService.findByCustomer(customer).isEmpty());
        assertEquals(booked.getId(), appointmentService.findByCustomer(customer).get(0).getId());
    }
}

