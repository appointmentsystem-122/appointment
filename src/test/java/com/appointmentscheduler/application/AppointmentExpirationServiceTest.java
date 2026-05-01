package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Auto-expiry of past CONFIRMED/PENDING appointments.
 */
@DisplayName("AppointmentExpirationService")
class AppointmentExpirationServiceTest {

    private AppointmentRepository repo;
    private AppointmentExpirationService service;

    @BeforeEach
    void setUp() {
        repo = mock(AppointmentRepository.class);
        service = new AppointmentExpirationService(repo, new AuditLogService());
    }

    @Nested
    @DisplayName("Expiry transitions")
    class Transitions {

        @Test
        @DisplayName("Past CONFIRMED appointment becomes EXPIRED")
        void confirmedPastExpires() {
            User patient = new User("p1", "P", "p@t.com", "x");
            LocalDateTime past = LocalDateTime.now().minusDays(2);
            InPersonAppointment a = new InPersonAppointment("a1", patient, new TimeSlot(past, past.plusHours(1)), "L");
            a.setStatus("CONFIRMED");
            when(repo.findAll()).thenReturn(List.of(a));

            assertThat(service.expirePastAppointments()).isEqualTo(1);
            assertThat(a.getStatus()).isEqualTo("EXPIRED");
            verify(repo).save(a);
        }

        @Test
        @DisplayName("Past PENDING appointment becomes EXPIRED")
        void pendingPastExpires() {
            User patient = new User("p1", "P", "p@t.com", "x");
            LocalDateTime past = LocalDateTime.now().minusHours(3);
            InPersonAppointment a = new InPersonAppointment("pnd", patient, new TimeSlot(past, past.plusHours(1)), "L");
            a.setStatus("PENDING");
            when(repo.findAll()).thenReturn(List.of(a));

            assertThat(service.expirePastAppointments()).isEqualTo(1);
            assertThat(a.getStatus()).isEqualTo("EXPIRED");
            verify(repo).save(a);
        }

        @Test
        @DisplayName("Future appointments are untouched")
        void futureUnchanged() {
            User patient = new User("p1", "P", "p@t.com", "x");
            LocalDateTime future = LocalDateTime.now().plusDays(1);
            InPersonAppointment a = new InPersonAppointment("a1", patient, new TimeSlot(future, future.plusHours(1)), "L");
            a.setStatus("CONFIRMED");
            when(repo.findAll()).thenReturn(List.of(a));

            assertThat(service.expirePastAppointments()).isZero();
            assertThat(a.getStatus()).isEqualTo("CONFIRMED");
            verify(repo, never()).save(any(Appointment.class));
        }
    }

    @Nested
    @DisplayName("Non-expiring statuses")
    class SkippedStatuses {

        @Test
        @DisplayName("CANCELLED past appointments stay CANCELLED")
        void cancelledPast() {
            User patient = new User("p1", "P", "p@t.com", "x");
            LocalDateTime past = LocalDateTime.now().minusDays(1);
            InPersonAppointment a = new InPersonAppointment("a1", patient, new TimeSlot(past, past.plusHours(1)), "L");
            a.setStatus("CANCELLED");
            when(repo.findAll()).thenReturn(List.of(a));

            assertThat(service.expirePastAppointments()).isZero();
            assertThat(a.getStatus()).isEqualTo("CANCELLED");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("COMPLETED past appointments are not re-opened or expired")
        void completedPast() {
            User patient = new User("p1", "P", "p@t.com", "x");
            LocalDateTime past = LocalDateTime.now().minusDays(1);
            InPersonAppointment a = new InPersonAppointment("a1", patient, new TimeSlot(past, past.plusHours(1)), "L");
            a.setStatus("COMPLETED");
            when(repo.findAll()).thenReturn(List.of(a));

            assertThat(service.expirePastAppointments()).isZero();
            assertThat(a.getStatus()).isEqualTo("COMPLETED");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("Empty repository returns zero count")
        void emptyRepo() {
            when(repo.findAll()).thenReturn(Collections.emptyList());
            assertThat(service.expirePastAppointments()).isZero();
            verify(repo, never()).save(any());
        }
    }
}
