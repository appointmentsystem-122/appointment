package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupRestoreServiceTest {

    @Test
    void exportManifestAndCsv() throws Exception {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        InMemoryDoctorRepository dr = new InMemoryDoctorRepository();
        InMemoryRoomRepository rr = new InMemoryRoomRepository();
        InMemoryClinicRepository cr = new InMemoryClinicRepository();
        BackupRestoreService svc = new BackupRestoreService(ar, ur, dr, rr, cr);
        User u = new User("u", "N", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "L");
        ar.save(a);
        Path m = Files.createTempFile("manifest", ".txt");
        Path c = Files.createTempFile("bk", ".csv");
        try {
            svc.exportBackupManifest(m.toString());
            assertThat(Files.readString(m)).contains("appointments=1");
            svc.exportAppointmentsCsv(c.toString());
            assertThat(Files.readString(c)).contains(a.getId());
        } finally {
            Files.deleteIfExists(m);
            Files.deleteIfExists(c);
        }
    }

    @Test
    void exportAppointmentsCsv_nullPatientFields_useEmptyStrings() throws Exception {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        InMemoryDoctorRepository dr = new InMemoryDoctorRepository();
        InMemoryRoomRepository rr = new InMemoryRoomRepository();
        InMemoryClinicRepository cr = new InMemoryClinicRepository();
        BackupRestoreService svc = new BackupRestoreService(ar, ur, dr, rr, cr);
        LocalDateTime s = LocalDateTime.now().plusDays(2);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        Appointment a = mock(Appointment.class);
        when(a.isDeleted()).thenReturn(false);
        when(a.getId()).thenReturn("csv-pat-null");
        when(a.getPatient()).thenReturn(null);
        when(a.getTimeSlot()).thenReturn(slot);
        when(a.getStatus()).thenReturn("CONFIRMED");
        when(a.getDoctorId()).thenReturn(null);
        when(a.getRoomId()).thenReturn(null);
        when(a.getClinicId()).thenReturn(null);
        ar.save(a);
        Path c = Files.createTempFile("bk2", ".csv");
        try {
            svc.exportAppointmentsCsv(c.toString());
            assertThat(Files.readString(c)).contains("csv-pat-null");
        } finally {
            Files.deleteIfExists(c);
        }
    }

    @Test
    void exportAppointmentsCsv_skipsDeleted_and_handlesNullTimeSlot() throws Exception {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        BackupRestoreService svc = new BackupRestoreService(
                ar,
                new InMemoryUserRepository(),
                new InMemoryDoctorRepository(),
                new InMemoryRoomRepository(),
                new InMemoryClinicRepository());

        Appointment deleted = mock(Appointment.class);
        when(deleted.isDeleted()).thenReturn(true);
        when(deleted.getId()).thenReturn("deleted-row");
        ar.save(deleted);

        Appointment nullSlot = mock(Appointment.class);
        when(nullSlot.isDeleted()).thenReturn(false);
        when(nullSlot.getId()).thenReturn("no-slot");
        when(nullSlot.getPatient()).thenReturn(null);
        when(nullSlot.getTimeSlot()).thenReturn(null);
        when(nullSlot.getStatus()).thenReturn("PENDING");
        ar.save(nullSlot);

        Path c = Files.createTempFile("bk3", ".csv");
        try {
            svc.exportAppointmentsCsv(c.toString());
            var lines = Files.readAllLines(c).stream().collect(Collectors.toList());
            assertThat(lines.stream().anyMatch(l -> l.contains("deleted-row"))).isFalse();
            assertThat(lines.stream().anyMatch(l -> l.contains("no-slot"))).isTrue();
        } finally {
            Files.deleteIfExists(c);
        }
    }
}
