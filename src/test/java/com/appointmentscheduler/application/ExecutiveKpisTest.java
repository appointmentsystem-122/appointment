package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Uses real {@link ReportingService} + mocked {@link AppointmentRepository} (interface only)
 * so KPI math is exercised without mocking concrete service classes.
 */
class ExecutiveKpisTest {

    private static InPersonAppointment appt(String id, User p, LocalDateTime start, String clinicId, String status) {
        InPersonAppointment a = new InPersonAppointment(id, p, new TimeSlot(start, start.plusHours(1)), "L");
        a.setClinicId(clinicId);
        a.setStatus(status);
        return a;
    }

    @Test
    void build_returnsFourRows_andCancellationStatus() {
        User p = new User("p1", "P", "p@t.com", "x");
        LocalDate today = LocalDate.now();
        LocalDateTime tPeak = today.atTime(14, 0);

        List<com.appointmentscheduler.domain.Appointment> data = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            data.add(appt("ok" + i, p, tPeak.plusMinutes(i), "clinic-1", "CONFIRMED"));
        }
        data.add(appt("c1", p, tPeak.plusMinutes(45), "clinic-1", "CANCELLED"));
        data.add(appt("c2", p, tPeak.plusMinutes(50), "clinic-1", "CANCELLED"));
        // 2 cancelled / 10 total = 20% → WARNING; most starts in hour 14 → peak 14

        AppointmentRepository repo = mock(AppointmentRepository.class);
        when(repo.findAll()).thenReturn(data);

        ReportingService rs = new ReportingService(repo);
        List<ExecutiveKpis.KpiRow> rows = ExecutiveKpis.build(rs, "clinic-1");

        assertThat(rows).hasSize(4);
        assertThat(rows.get(0).value).isEqualTo("10");
        assertThat(rows.get(1).value).isEqualTo("10");
        assertThat(rows.get(2).status).isEqualTo(ExecutiveKpis.Status.WARNING);
        assertThat(rows.get(3).value).contains("14");
    }

    @Test
    void build_cancellationCritical_whenRateHigh() {
        User p = new User("p1", "P", "p@t.com", "x");
        LocalDateTime start = LocalDate.now().atTime(9, 0);
        List<com.appointmentscheduler.domain.Appointment> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            data.add(appt("ok" + i, p, start.plusMinutes(i), null, "CONFIRMED"));
        }
        for (int i = 0; i < 8; i++) {
            data.add(appt("cx" + i, p, start.plusHours(3 + i), null, "CANCELLED"));
        }
        // 8 cancelled / 15 ≈ 53%

        AppointmentRepository repo = mock(AppointmentRepository.class);
        when(repo.findAll()).thenReturn(data);

        ReportingService reporting = new ReportingService(repo);
        List<ExecutiveKpis.KpiRow> rows = ExecutiveKpis.build(reporting, null);

        assertThat(rows.get(2).status).isEqualTo(ExecutiveKpis.Status.CRITICAL);
    }

    @Test
    void build_cancellationOk_whenRateLow() {
        User p = new User("p1", "P", "p@t.com", "x");
        LocalDateTime start = LocalDate.now().atTime(9, 0);
        List<com.appointmentscheduler.domain.Appointment> data = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            data.add(appt("ok" + i, p, start.plusHours(i), "clinic-ok", "CONFIRMED"));
        }
        data.add(appt("c0", p, start.plusHours(10), "clinic-ok", "CANCELLED"));
        // 1/10 = 10% => OK

        AppointmentRepository repo = mock(AppointmentRepository.class);
        when(repo.findAll()).thenReturn(data);

        ReportingService reporting = new ReportingService(repo);
        List<ExecutiveKpis.KpiRow> rows = ExecutiveKpis.build(reporting, "clinic-ok");
        assertThat(rows.get(2).status).isEqualTo(ExecutiveKpis.Status.OK);
    }

    @Test
    void kpiRow_nullThresholdInfo_normalizedToEmptyString() {
        ExecutiveKpis.KpiRow row = new ExecutiveKpis.KpiRow("label", "value", ExecutiveKpis.Status.OK, null);
        assertThat(row.thresholdInfo).isEmpty();
    }

    @Test
    void utilityConstructor_isCovered() {
        assertThat(new ExecutiveKpis()).isNotNull();
    }
}
