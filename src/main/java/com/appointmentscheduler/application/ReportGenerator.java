package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ReportGenerator {

    public static boolean exportAppointmentsToCSV(List<Appointment> appointments, String filePath) {
        try {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                // Write CSV Header
                writer.println("Appointment ID,Patient Name,Date,Time,Status,Type");

                // Write Data Rows
                for (Appointment appt : appointments) {
                    String id = appt.getId();
                    String patientName = appt.getPatient().getName().replace(",", " "); // sanitize commas
                    String date = appt.getTimeSlot().getStartTime().toLocalDate().toString();
                    String time = appt.getTimeSlot().getStartTime().toLocalTime().toString();
                    String status = appt.getStatus();
                    String type = appt.getClass().getSimpleName();

                    writer.printf("%s,%s,%s,%s,%s,%s%n", id, patientName, date, time, status, type);
                }
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
