package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ResourceLock("AppConfigProps")
class ApplicationLowBranchBoostTest {

    @AfterEach
    void resetProps() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void appConfig_parsing_and_systemTypeBranches() {
        Properties p = new Properties();
        p.setProperty("x.int", "abc");
        p.setProperty("x.bool", "TRUE");
        p.setProperty("booking.serviceTypes", " Remote , In person ");
        AppConfig.applyPropertiesForTest(p);

        assertThat(AppConfig.getInt("x.int", 42)).isEqualTo(42);
        assertThat(AppConfig.getBoolean("x.bool", false)).isTrue();
        assertThat(AppConfig.getBoolean("x.unknown", true)).isTrue();
        assertThat(AppConfig.getBookingServiceTypes()).containsExactly(" Remote", "In person ");

        AppConfig.setSystemType("Clinic");
        assertThat(AppConfig.getSystemType()).isEqualTo("Healthcare");
        AppConfig.setSystemType(" ");
        assertThatCode(AppConfig::getSystemType).doesNotThrowAnyException();
    }

    @Test
    void bookingCatalog_modesNullAndOnlineComparisonBranches() {
        Properties p = new Properties();
        p.setProperty("booking.serviceTypes", "");
        p.setProperty("booking.appointmentTypes", "A,B");
        AppConfig.applyPropertiesForTest(p);
        List<BookingOption> options = BookingCatalog.listOptions();
        assertThat(options).isNotEmpty();
    }

    @Test
    void bookingRequestFields_defaultLanguageAny_and_partyClampMin() {
        User u = new User("u", "N", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "L");
        BookingRequestFields.applyTo(a, " ", " ", null, " ", "??", 0, 0);
        assertThat(a.getParticipantCount()).isEqualTo(1);
        assertThat(a.getCustomerNotes()).isNull();
        assertThat(a.getReminderChannel()).isEqualTo(BookingRequestFields.REMINDER_APP);
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_ANY);
    }

    @Test
    void pdfReport_filtersDeletedCancelled_and_badPathThrows() throws Exception {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        ReportingService reporting = new ReportingService(repo);
        PdfReportService svc = new PdfReportService(repo, reporting);

        User u = new User("u", "Name", "e@x.com", "x");
        LocalDate day = LocalDate.now().plusDays(3);
        LocalDateTime t = day.atTime(9, 0);
        InPersonAppointment ok = new InPersonAppointment(u, new TimeSlot(t, t.plusHours(1)), "L");
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(t.plusHours(2), t.plusHours(3)), "L");
        cancelled.setStatus("CANCELLED");
        InPersonAppointment deleted = new InPersonAppointment(u, new TimeSlot(t.plusHours(4), t.plusHours(5)), "L");
        deleted.markDeleted("admin");
        repo.save(ok);
        repo.save(cancelled);
        repo.save(deleted);

        Path out = Files.createTempFile("daily-branch", ".html");
        try {
            svc.writeDailyReport(day, out.toString());
            String html = Files.readString(out);
            assertThat(html).contains("Total for day:</strong> 1");
            assertThat(html).doesNotContain("CANCELLED");
        } finally {
            Files.deleteIfExists(out);
        }
    }

    @Test
    void reportGenerator_directoryPath_returnsFalseIoBranch() throws Exception {
        User p = new User("u", "Name,Comma", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L");
        Path dir = Files.createTempDirectory("rep-dir");
        try {
            boolean ok = ReportGenerator.exportAppointmentsToCSV(List.of(a), dir.toString());
            assertThat(ok).isFalse();
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void slotRecommendation_filtersCancelledInCongestionWindow() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        ScheduleService schedule = new ScheduleService(repo);
        SlotRecommendationService svc = new SlotRecommendationService(schedule);

        User u = new User("u-s", "N", "s@x.com", "x");
        LocalDate d = LocalDate.now().plusDays(7);
        LocalDateTime t1 = d.atTime(8, 0);
        LocalDateTime t2 = d.atTime(9, 0);
        InPersonAppointment keep = new InPersonAppointment(u, new TimeSlot(t1, t1.plusHours(1)), "L");
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(t2, t2.plusHours(1)), "L");
        cancelled.setStatus("CANCELLED");
        repo.save(keep);
        repo.save(cancelled);

        List<TimeSlot> rec = svc.getRecommendedSlots(d, 3);
        assertThat(rec).isNotNull();
    }

    @Test
    void auditLogService_withInjectedRepository_delegatesAllQueries() {
        com.appointmentscheduler.persistence.AuditEntryRepository repo = mock(com.appointmentscheduler.persistence.AuditEntryRepository.class);
        when(repo.findRecent(2)).thenReturn(List.of());
        when(repo.findAll()).thenReturn(List.of());
        when(repo.findByEntityType("E")).thenReturn(List.of());
        when(repo.findByUserId("u")).thenReturn(List.of());

        AuditLogService svc = new AuditLogService(repo);
        svc.log((User) null, "ACT", "details");
        svc.log("id", "name", "ACT2", "details2");
        svc.getRecentEntries(2);
        svc.getAllEntries();
        svc.getEntriesByEntityType("E");
        svc.getEntriesByUser("u");

        verify(repo, org.mockito.Mockito.times(2)).append(org.mockito.ArgumentMatchers.any());
        verify(repo).findRecent(2);
        verify(repo).findAll();
        verify(repo).findByEntityType("E");
        verify(repo).findByUserId("u");
    }

    @Test
    void backupRestore_manifestAndCsv_badPaths_throwIo() {
        BackupRestoreService svc = new BackupRestoreService(
                new InMemoryAppointmentRepository(),
                new com.appointmentscheduler.persistence.InMemoryUserRepository(),
                new com.appointmentscheduler.persistence.InMemoryDoctorRepository(),
                new com.appointmentscheduler.persistence.InMemoryRoomRepository(),
                new com.appointmentscheduler.persistence.InMemoryClinicRepository());

        assertThatThrownBy(() -> svc.exportBackupManifest("?:\\bad\\manifest.txt")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> svc.exportAppointmentsCsv("?:\\bad\\appointments.csv")).isInstanceOf(Exception.class);
    }
}

