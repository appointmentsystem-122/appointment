package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportingService.
 */
class ReportingServiceTest {

    private ReportingService reportingService;
    private AppointmentRepository repository;
    private User user;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAppointmentRepository();
        reportingService = new ReportingService(repository);
        user = new User("u1", "Test User", "test@test.com", "hash");
    }

    @Test
    void getAppointmentsPerType_empty() {
        var result = reportingService.getAppointmentsPerType();
        assertTrue(result.isEmpty());
    }

    @Test
    void getAppointmentsPerType_withData() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TimeSlot slot1 = new TimeSlot(start, start.plusHours(1));
        TimeSlot slot2 = new TimeSlot(start.plusHours(2), start.plusHours(3));
        Appointment a1 = new InPersonAppointment(user, slot1, "Room A");
        Appointment a2 = new InPersonAppointment(user, slot2, "Room B");
        repository.save(a1);
        repository.save(a2);

        var result = reportingService.getAppointmentsPerType();
        assertEquals(1, result.size());
        assertEquals(2L, result.get("InPersonAppointment"));
    }

    @Test
    void getCancellationRate_empty() {
        assertEquals(0.0, reportingService.getCancellationRate(), 0.01);
    }

    @Test
    void getCancellationRate_withData() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TimeSlot slot1 = new TimeSlot(start, start.plusHours(1));
        TimeSlot slot2 = new TimeSlot(start.plusHours(2), start.plusHours(3));
        Appointment a1 = new InPersonAppointment(user, slot1, "Room A");
        Appointment a2 = new InPersonAppointment(user, slot2, "Room B");
        a2.setStatus("CANCELLED");
        repository.save(a1);
        repository.save(a2);

        assertEquals(50.0, reportingService.getCancellationRate(), 0.1);
    }

    @Test
    void getPeakBookingHour_empty() {
        assertEquals(9, reportingService.getPeakBookingHour());
    }

    @Test
    void getTotalAppointmentsCount() {
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        repository.save(new InPersonAppointment(user, slot, "Room A"));
        assertEquals(1, reportingService.getTotalAppointmentsCount());
    }

    @Test
    void getTotalAppointmentsCount_withClinicFilter_matchesBranchOrNullClinic() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        InPersonAppointment a = new InPersonAppointment(user, slot, "R");
        a.setClinicId("c1");
        repository.save(a);
        assertEquals(1, reportingService.getTotalAppointmentsCount("c1"));
        assertEquals(1, reportingService.getTotalAppointmentsCount(null));
        assertEquals(0, reportingService.getTotalAppointmentsCount("other"));
    }

    @Test
    void getCancellationRate_withClinicFilter() {
        LocalDateTime t = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment ok = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        ok.setClinicId("br");
        InPersonAppointment cancelled = new InPersonAppointment(user, new TimeSlot(t.plusHours(2), t.plusHours(3)), "R");
        cancelled.setClinicId("br");
        cancelled.setStatus("CANCELLED");
        repository.save(ok);
        repository.save(cancelled);
        assertEquals(50.0, reportingService.getCancellationRate("br"), 0.1);
        assertEquals(0.0, reportingService.getCancellationRate("nomatch"), 0.01);
    }

    @Test
    void getAppointmentsInRange_includesSameDayAndClinicFilter() {
        LocalDate day = LocalDate.now().plusDays(5);
        LocalDateTime start = day.atTime(14, 0);
        InPersonAppointment a = new InPersonAppointment(user, new TimeSlot(start, start.plusHours(1)), "R");
        a.setClinicId("cx");
        repository.save(a);
        List<Appointment> list = reportingService.getAppointmentsInRange(day, day, "cx");
        assertEquals(1, list.size());
        assertTrue(reportingService.getAppointmentsInRange(day, day, "no").isEmpty());
    }

    @Test
    void getTodayThisWeekLastWeekYesterday_counts() {
        assertTrue(reportingService.getTodayAppointmentsCount() >= 0);
        assertTrue(reportingService.getTodayAppointmentsCount("any") >= 0);
        assertTrue(reportingService.getThisWeekAppointmentsCount() >= 0);
        assertTrue(reportingService.getLastWeekAppointmentsCount(null) >= 0);
        assertTrue(reportingService.getYesterdayAppointmentsCount("x") >= 0);
    }

    @Test
    void getAppointmentsByDoctor_skipsCancelledAndEmptyDoctor() {
        LocalDateTime t = LocalDateTime.now().plusDays(4).withHour(11).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment withDoc = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        withDoc.setDoctorId("d1");
        InPersonAppointment cancelled = new InPersonAppointment(user, new TimeSlot(t.plusHours(3), t.plusHours(4)), "R");
        cancelled.setDoctorId("d1");
        cancelled.setStatus("CANCELLED");
        InPersonAppointment noDoc = new InPersonAppointment(user, new TimeSlot(t.plusHours(5), t.plusHours(6)), "R");
        repository.save(withDoc);
        repository.save(cancelled);
        repository.save(noDoc);
        Map<String, Long> byDoc = reportingService.getAppointmentsByDoctor();
        assertEquals(1L, byDoc.getOrDefault("d1", 0L));
    }

    @Test
    void getDoctorStats_countsToday() {
        LocalDateTime t = LocalDate.now().atTime(15, 0);
        InPersonAppointment a = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        a.setDoctorId("docX");
        repository.save(a);
        ReportingService.DoctorStats stats = reportingService.getDoctorStats("docX");
        assertEquals("docX", stats.doctorId);
        assertTrue(stats.totalBookings >= 1);
    }

    @Test
    void getAppointmentsPerType_excludesDeleted() {
        LocalDateTime t = LocalDateTime.now().plusDays(6).withHour(8).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment del = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        del.markDeleted("admin");
        repository.save(del);
        assertTrue(reportingService.getAppointmentsPerType().isEmpty());
    }

    @Test
    void getPeakBookingHour_picksMostFrequentHour() {
        LocalDateTime t1 = LocalDateTime.now().plusDays(7).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime t2 = LocalDateTime.now().plusDays(7).withHour(14).withMinute(30).withSecond(0).withNano(0);
        repository.save(new InPersonAppointment(user, new TimeSlot(t1, t1.plusHours(1)), "R"));
        repository.save(new InPersonAppointment(user, new TimeSlot(t2, t2.plusHours(1)), "R"));
        assertEquals(14, reportingService.getPeakBookingHour());
    }

    @Test
    void getDoctorStats_unknownDoctor_returnsZeros() {
        ReportingService.DoctorStats stats = reportingService.getDoctorStats("no-such-doctor-id");
        assertEquals("no-such-doctor-id", stats.doctorId);
        assertEquals(0, stats.totalBookings);
        assertEquals(0, stats.todayBookings);
    }

    @Test
    void getYesterdayAppointmentsCount_withClinicFilter() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atTime(16, 0);
        InPersonAppointment a = new InPersonAppointment(user, new TimeSlot(start, start.plusHours(1)), "R");
        a.setClinicId("c-y");
        repository.save(a);
        assertTrue(reportingService.getYesterdayAppointmentsCount("c-y") >= 1);
        assertEquals(0, reportingService.getYesterdayAppointmentsCount("other-clinic"));
    }

    @Test
    void branchFilter_nullOrEmptyAppointmentClinic_matchesRequestedClinic() {
        LocalDateTime t = LocalDateTime.now().plusDays(8).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment nullClinic = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        nullClinic.setClinicId(null);
        InPersonAppointment emptyClinic = new InPersonAppointment(user, new TimeSlot(t.plusHours(2), t.plusHours(3)), "R");
        emptyClinic.setClinicId("");
        repository.save(nullClinic);
        repository.save(emptyClinic);
        assertEquals(2, reportingService.getTotalAppointmentsCount("requested-branch"));
    }

    @Test
    void appointmentsInRange_respectsInclusiveFrom_andExclusiveNextDayEnd() {
        LocalDate d = LocalDate.now().plusDays(9);
        LocalDateTime atStart = d.atStartOfDay();
        LocalDateTime atBoundaryExcluded = d.plusDays(1).atStartOfDay();
        InPersonAppointment in = new InPersonAppointment(user, new TimeSlot(atStart, atStart.plusHours(1)), "R");
        InPersonAppointment out = new InPersonAppointment(user, new TimeSlot(atBoundaryExcluded, atBoundaryExcluded.plusHours(1)), "R");
        repository.save(in);
        repository.save(out);
        assertEquals(1, reportingService.getAppointmentsInRange(d, d).size());
    }

    @Test
    void cancellationRate_caseSensitiveOnlyUpperCancelledCounted() {
        LocalDateTime t = LocalDateTime.now().plusDays(10).withHour(12).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment upper = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        upper.setStatus("CANCELLED");
        InPersonAppointment lower = new InPersonAppointment(user, new TimeSlot(t.plusHours(2), t.plusHours(3)), "R");
        lower.setStatus("cancelled");
        repository.save(upper);
        repository.save(lower);
        assertEquals(50.0, reportingService.getCancellationRate(), 0.1);
    }

    @Test
    void appointmentsByDoctor_excludesExpiredAndDeletedAndEmptyDoctor() {
        LocalDateTime t = LocalDateTime.now().plusDays(11).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment ok = new InPersonAppointment(user, new TimeSlot(t, t.plusHours(1)), "R");
        ok.setDoctorId("dx");
        ok.setStatus("CONFIRMED");

        InPersonAppointment expired = new InPersonAppointment(user, new TimeSlot(t.plusHours(2), t.plusHours(3)), "R");
        expired.setDoctorId("dx");
        expired.setStatus("EXPIRED");

        InPersonAppointment deleted = new InPersonAppointment(user, new TimeSlot(t.plusHours(4), t.plusHours(5)), "R");
        deleted.setDoctorId("dx");
        deleted.markDeleted("admin");

        InPersonAppointment emptyDoc = new InPersonAppointment(user, new TimeSlot(t.plusHours(6), t.plusHours(7)), "R");
        emptyDoc.setDoctorId("");
        emptyDoc.setStatus("CONFIRMED");

        repository.save(ok);
        repository.save(expired);
        repository.save(deleted);
        repository.save(emptyDoc);

        Map<String, Long> byDoc = reportingService.getAppointmentsByDoctor();
        assertEquals(1L, byDoc.getOrDefault("dx", 0L));
    }
}
