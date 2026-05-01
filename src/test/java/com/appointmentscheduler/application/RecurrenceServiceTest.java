package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.*;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceServiceTest {

    @Test
    void createSeries_savesOccurrences() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        AuditLogService audit = new AuditLogService();
        RecurrenceService svc = new RecurrenceService(repo, audit);
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 15, 10, 0);
        RecurrencePattern pattern = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, start, end, 1);
        InPersonAppointment proto = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "R");
        List<RecurringAppointment> series = svc.createRecurringSeries(p, pattern, Duration.ofHours(1), proto);
        assertThat(series).isNotEmpty();
        assertThat(repo.findAll()).hasSize(series.size());
    }

    @Test
    void cancelSingleOccurrence_successWhenFutureAndOwner() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        AuditLogService audit = new AuditLogService();
        RecurrenceService svc = new RecurrenceService(repo, audit);
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0).withSecond(0).withNano(0);
        RecurrencePattern pat = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, s.plusWeeks(2), 1);
        String occId = "sid_" + s.toEpochSecond(ZoneOffset.UTC);
        RecurringAppointment ra = new RecurringAppointment(occId, p, new TimeSlot(s, s.plusHours(1)), "sid", pat, occId);
        repo.save(ra);
        assertThat(svc.cancelSingleOccurrence(occId, p)).isTrue();
    }

    @Test
    void cancelSeriesAndListOccurrences() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        AuditLogService audit = new AuditLogService();
        RecurrenceService svc = new RecurrenceService(repo, audit);
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
        RecurrencePattern pat = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, s.plusWeeks(3), 1);
        String series = "SER123";
        RecurringAppointment r1 = new RecurringAppointment("a1", p, new TimeSlot(s, s.plusHours(1)), series, pat, "o1");
        RecurringAppointment r2 = new RecurringAppointment("a2", p, new TimeSlot(s.plusWeeks(1), s.plusWeeks(1).plusHours(1)), series, pat, "o2");
        repo.save(r1);
        repo.save(r2);
        assertThat(svc.getSeriesOccurrences(series)).hasSize(2);
        int n = svc.cancelEntireSeries(series, p);
        assertThat(n).isEqualTo(2);
    }

    @Test
    void cancelSingle_wrongPatientOrPast() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        RecurrenceService svc = new RecurrenceService(repo, new AuditLogService());
        User p = new User("u", "N", "e@x.com", "x");
        User other = new User("u2", "O", "o@x.com", "x");
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        RecurrencePattern pat = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, past, past.plusWeeks(1), 1);
        RecurringAppointment ra = new RecurringAppointment("x1", p, new TimeSlot(past, past.plusHours(1)), "s", pat, "x1");
        repo.save(ra);
        assertThat(svc.cancelSingleOccurrence("x1", other)).isFalse();
        assertThat(svc.cancelSingleOccurrence("missing", p)).isFalse();
    }
}
