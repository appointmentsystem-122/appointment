package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link AppointmentRepository#findBlockingBookingsForPatient(String)} (default method on the interface).
 * High leverage: used by booking rules for one-open-appointment enforcement.
 */
@DisplayName("AppointmentRepository default methods")
class AppointmentRepositoryDefaultMethodsTest {

    private InMemoryAppointmentRepository repo;
    private User patient;

    @BeforeEach
    void setUp() {
        repo = new InMemoryAppointmentRepository();
        patient = new User("p1", "Pat", "p@t.com", "x");
    }

    @Test
    @DisplayName("null patient id yields empty list (guard)")
    void nullPatientId_empty() {
        assertThat(repo.findBlockingBookingsForPatient(null)).isEmpty();
    }

    @Test
    @DisplayName("CONFIRMED and PENDING non-deleted appointments for that patient are blocking")
    void pendingAndConfirmedBlock() {
        LocalDateTime s = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a1 = new InPersonAppointment(patient, new TimeSlot(s, s.plusHours(1)), "L");
        a1.setStatus("CONFIRMED");
        InPersonAppointment a2 = new InPersonAppointment(patient, new TimeSlot(s.plusDays(1), s.plusDays(1).plusHours(1)), "L");
        a2.setStatus("PENDING");
        repo.save(a1);
        repo.save(a2);

        List<com.appointmentscheduler.domain.Appointment> blockers = repo.findBlockingBookingsForPatient("p1");
        assertThat(blockers).hasSize(2);
    }

    @Test
    @DisplayName("CANCELLED, COMPLETED, EXPIRED are not blocking")
    void terminalStatusesNotBlocking() {
        LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
        for (String st : List.of("CANCELLED", "COMPLETED", "EXPIRED")) {
            repo = new InMemoryAppointmentRepository();
            InPersonAppointment a = new InPersonAppointment(patient, new TimeSlot(s, s.plusHours(1)), "L");
            a.setStatus(st);
            repo.save(a);
            assertThat(repo.findBlockingBookingsForPatient("p1")).as("status %s", st).isEmpty();
        }
    }

    @Test
    @DisplayName("Soft-deleted rows are excluded")
    void deletedExcluded() {
        LocalDateTime s = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a = new InPersonAppointment(patient, new TimeSlot(s, s.plusHours(1)), "L");
        a.setStatus("CONFIRMED");
        a.markDeleted("admin");
        repo.save(a);
        assertThat(repo.findBlockingBookingsForPatient("p1")).isEmpty();
    }

    @Test
    @DisplayName("Other patients' appointments do not appear in the blockers list")
    void otherPatientsIgnored() {
        User other = new User("p2", "O", "o@t.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a = new InPersonAppointment(other, new TimeSlot(s, s.plusHours(1)), "L");
        a.setStatus("CONFIRMED");
        repo.save(a);
        assertThat(repo.findBlockingBookingsForPatient("p1")).isEmpty();
    }

    @Test
    @DisplayName("Null elements from findAll are ignored (Objects::nonNull filter)")
    void nullAppointmentInFindAll_ignored() {
        LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment ok = new InPersonAppointment(patient, new TimeSlot(s, s.plusHours(1)), "L");
        ok.setStatus("PENDING");
        AppointmentRepository withNull = new AppointmentRepository() {
            @Override
            public void save(Appointment appointment) { }

            @Override
            public Optional<Appointment> findById(String id) {
                return Optional.empty();
            }

            @Override
            public List<Appointment> findAll() {
                return Arrays.asList(null, ok);
            }

            @Override
            public void deleteById(String id) { }
        };
        assertThat(withNull.findBlockingBookingsForPatient("p1")).hasSize(1);
    }

    @Test
    @DisplayName("Null or non-blocking status values are excluded from blockers")
    void nonBlockingStatuses_excluded() {
        LocalDateTime s = LocalDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0);
        for (String st : Arrays.asList(null, "", "PROCESSING", "HOLD")) {
            repo = new InMemoryAppointmentRepository();
            InPersonAppointment a = new InPersonAppointment(patient, new TimeSlot(s, s.plusHours(1)), "L");
            a.setStatus(st);
            repo.save(a);
            assertThat(repo.findBlockingBookingsForPatient("p1")).as("status %s", String.valueOf(st)).isEmpty();
        }
    }

    @Test
    @DisplayName("Appointments with null patient are excluded")
    void nullPatientOnAppointment_excluded() {
        Appointment orphan = mock(Appointment.class);
        when(orphan.getPatient()).thenReturn(null);
        when(orphan.getStatus()).thenReturn("CONFIRMED");
        when(orphan.isDeleted()).thenReturn(false);
        AppointmentRepository repo2 = new AppointmentRepository() {
            @Override
            public void save(Appointment appointment) { }

            @Override
            public Optional<Appointment> findById(String id) {
                return Optional.empty();
            }

            @Override
            public List<Appointment> findAll() {
                return List.of(orphan);
            }

            @Override
            public void deleteById(String id) { }
        };
        assertThat(repo2.findBlockingBookingsForPatient("p1")).isEmpty();
    }
}
