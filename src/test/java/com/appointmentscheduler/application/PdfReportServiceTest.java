package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportServiceTest {

    @Test
    void writeDailyReport_htmlFile() throws Exception {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        ReportingService reporting = new ReportingService(repo);
        PdfReportService svc = new PdfReportService(repo, reporting);
        User u = new User("u", "N", "e@x.com", "x");
        LocalDate day = LocalDate.now().plusDays(2);
        LocalDateTime s = day.atTime(10, 0);
        InPersonAppointment a = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "L");
        repo.save(a);
        Path f = Files.createTempFile("daily", ".html");
        try {
            svc.writeDailyReport(day, f.toString());
            String html = Files.readString(f);
            assertThat(html).contains("Daily Appointment Report");
        } finally {
            Files.deleteIfExists(f);
        }
    }
}
