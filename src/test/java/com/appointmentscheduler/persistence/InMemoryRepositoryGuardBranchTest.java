package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers null-guard branches and list-slicing branches in in-memory repositories
 * (cheap wins for package branch coverage).
 */
class InMemoryRepositoryGuardBranchTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 4, 11, 10, 0);

    @Test
    void user_saveNull_isIgnored() {
        InMemoryUserRepository r = new InMemoryUserRepository();
        r.save(null);
        assertThat(r.findAll()).isEmpty();
    }

    @Test
    void appointment_saveNull_isIgnored() {
        InMemoryAppointmentRepository r = new InMemoryAppointmentRepository();
        r.save(null);
        assertThat(r.findAll()).isEmpty();
    }

    @Test
    void clinic_doctor_room_saveNull_isIgnored() {
        new InMemoryClinicRepository().save(null);
        new InMemoryDoctorRepository().save(null);
        new InMemoryRoomRepository().save(null);
    }

    @Test
    void audit_appendNull_isIgnored() {
        InMemoryAuditEntryRepository r = new InMemoryAuditEntryRepository();
        r.append(null);
        assertThat(r.findAll()).isEmpty();
    }

    @Test
    void audit_findRecent_whenLargerThanMax_returnsTail() {
        InMemoryAuditEntryRepository r = new InMemoryAuditEntryRepository();
        for (int i = 0; i < 12; i++) {
            r.append(new AuditEntry(T0.plusMinutes(i), "u", "n", "A", "d" + i));
        }
        List<AuditEntry> tail = r.findRecent(5);
        assertThat(tail).hasSize(5);
        assertThat(tail.get(0).getDetails()).isEqualTo("d7");
        assertThat(r.findRecent(20)).hasSize(12);
    }

    @Test
    void audit_findByUserId_nullOrEmpty_returnsAll() {
        InMemoryAuditEntryRepository r = new InMemoryAuditEntryRepository();
        r.append(new AuditEntry(T0, "a", "n", "x", "d"));
        r.append(new AuditEntry(T0, "b", "n", "y", "d"));
        assertThat(r.findByUserId(null)).hasSize(2);
        assertThat(r.findByUserId("")).hasSize(2);
        assertThat(r.findByUserId("a")).hasSize(1);
    }

    @Test
    void audit_findByEntityType_nullOrEmpty_returnsAll() {
        InMemoryAuditEntryRepository r = new InMemoryAuditEntryRepository();
        r.append(new AuditEntry(T0, "u", "n", "x", "d", "TYPE_A", "e1", null, null));
        r.append(new AuditEntry(T0, "u", "n", "y", "d", "TYPE_B", "e2", null, null));
        assertThat(r.findByEntityType(null)).hasSize(2);
        assertThat(r.findByEntityType("")).hasSize(2);
        assertThat(r.findByEntityType("TYPE_A")).hasSize(1);
    }

    @Test
    void audit_trimWhileOverMax_capacity() {
        InMemoryAuditEntryRepository r = new InMemoryAuditEntryRepository();
        for (int i = 0; i < 2002; i++) {
            r.append(new AuditEntry(T0.plusNanos(i), "u", "n", "A", "x" + i));
        }
        assertThat(r.findAll().size()).isEqualTo(2000);
    }

    @Test
    void appointmentRepository_defaultMethod_blockingFilters() {
        User p = new User("p-guard", "G", "g@g.com", "h");
        TimeSlot slot = new TimeSlot(T0, T0.plusHours(1));
        InMemoryAppointmentRepository r = new InMemoryAppointmentRepository();
        Appointment ok = new InPersonAppointment("a1", p, slot, "L");
        ok.setStatus("CONFIRMED");
        r.save(ok);
        assertThat(r.findBlockingBookingsForPatient("p-guard")).hasSize(1);
        assertThat(r.findBlockingBookingsForPatient("other")).isEmpty();
    }
}
