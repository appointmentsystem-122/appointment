package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    public static boolean exportAppointmentsToCSV(List<Appointment> appointments, String filePath) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(filePath), StandardCharsets.UTF_8))) {
            writer.println("Appointment ID,Patient Name,Date,Time,Status,Type");

            for (Appointment appt : appointments) {
                String id = appt.getId();
                String patientName = appt.getPatient().getName().replace(",", " ");
                String date = appt.getTimeSlot().getStartTime().toLocalDate().toString();
                String time = appt.getTimeSlot().getStartTime().toLocalTime().toString();
                String status = appt.getStatus();
                String type = appt.getClass().getSimpleName();

                writer.printf("%s,%s,%s,%s,%s,%s%n", id, patientName, date, time, status, type);
            }
            return true;
        } catch (IOException e) {
            log.warn("Could not export appointments report to '{}': {}", filePath, e.getMessage());
            return false;
        }
    }
}
