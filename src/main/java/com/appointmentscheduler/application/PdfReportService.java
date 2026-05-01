package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.persistence.AppointmentRepository;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates daily and summary reports (HTML/text) for export.
 * For full PDF support, add openpdf dependency and use a PDF library.
 */
public class PdfReportService {

    private final AppointmentRepository appointmentRepository;
    private final ReportingService reportingService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PdfReportService(AppointmentRepository appointmentRepository, ReportingService reportingService) {
        this.appointmentRepository = appointmentRepository;
        this.reportingService = reportingService;
    }

    /**
     * Writes a daily report (HTML) to the given path.
     */
    public void writeDailyReport(LocalDate date, String outputPath) throws Exception {
        List<Appointment> dayAppointments = appointmentRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(a -> !a.isDeleted())
                .filter(a -> a.getTimeSlot().getStartTime().toLocalDate().equals(date))
                .filter(a -> !"CANCELLED".equals(a.getStatus()))
                .collect(Collectors.toList());

        Path path = Path.of(outputPath);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path))) {
            w.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Daily Report</title></head><body>");
            w.println("<h1>Daily Appointment Report – " + date.format(DATE_FMT) + "</h1>");
            w.println("<p><strong>Total for day:</strong> " + dayAppointments.size() + " | <strong>System total:</strong> " + reportingService.getTotalAppointmentsCount() + "</p>");
            w.println("<table border=\"1\"><tr><th>Time</th><th>Patient</th><th>Status</th></tr>");
            for (Appointment a : dayAppointments) {
                w.println("<tr><td>" + a.getTimeSlot().toString() + "</td><td>" + a.getPatient().getName() + "</td><td>" + a.getStatus() + "</td></tr>");
            }
            w.println("</table></body></html>");
        }
    }
}
