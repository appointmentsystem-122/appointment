package com.appointmentscheduler.application.email;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.VirtualAppointment;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Builds human-readable appointment metadata for outbound email bodies.
 * Keeps presentation text in one place so {@link JakartaMailEmailNotificationService} stays focused on transport.
 */
public final class AppointmentEmailContent {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private AppointmentEmailContent() {
    }

    /**
     * @return a short label for the concrete appointment type (English, email-friendly).
     */
    public static String describeAppointmentType(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        if (appointment instanceof VirtualAppointment) {
            return "Virtual / online";
        }
        if (appointment instanceof InPersonAppointment) {
            return "In person";
        }
        if (appointment instanceof UrgentAppointment) {
            return "Urgent";
        }
        if (appointment instanceof GroupAppointment) {
            return "Group";
        }
        if (appointment instanceof FollowUpAppointment) {
            return "Follow-up";
        }
        if (appointment instanceof AssessmentAppointment) {
            return "Assessment / consultation";
        }
        if (appointment instanceof RecurringAppointment) {
            return "Recurring";
        }
        if (appointment instanceof IndividualAppointment) {
            return "Individual";
        }
        String simple = appointment.getClass().getSimpleName();
        if (simple.contains("Assessment") || simple.contains("Consult")) {
            return "Consultation";
        }
        if (simple.contains("Follow")) {
            return "Follow-up";
        }
        return "Standard";
    }

    /**
     * @return formatted local date for the slot start.
     */
    public static String formatAppointmentDate(TimeSlot slot) {
        Objects.requireNonNull(slot, "slot");
        return slot.getStartTime().format(DATE_FMT);
    }

    /**
     * @return formatted local start–end time range.
     */
    public static String formatAppointmentTimeRange(TimeSlot slot) {
        Objects.requireNonNull(slot, "slot");
        return slot.getStartTime().format(TIME_FMT) + " – " + slot.getEndTime().format(TIME_FMT);
    }

    /**
     * Builds the plain-text body for a booking confirmation.
     *
     * @param companyName optional branding line (e.g. from {@code app.brand.name})
     */
    public static String buildBookingConfirmationBody(Appointment appointment, String companyName) {
        Objects.requireNonNull(appointment, "appointment");
        TimeSlot slot = appointment.getTimeSlot();
        String patientName = appointment.getPatient() != null ? appointment.getPatient().getName() : "Customer";
        String brand = companyName != null && !companyName.isBlank() ? companyName.trim() : "Appointment Booking System";

        return "Dear " + patientName + ",\r\n\r\n"
                + "Thank you for using " + brand + ". Your appointment has been successfully booked and confirmed.\r\n\r\n"
                + "Appointment details\r\n"
                + "-------------------\r\n"
                + "Date:     " + formatAppointmentDate(slot) + "\r\n"
                + "Time:     " + formatAppointmentTimeRange(slot) + "\r\n"
                + "Type:     " + describeAppointmentType(appointment) + "\r\n"
                + "Status:   " + appointment.getStatus() + "\r\n"
                + "Reference: " + appointment.getId() + "\r\n\r\n"
                + "If you need to change or cancel this appointment, please use the application or contact us as soon as possible.\r\n\r\n"
                + "Kind regards,\r\n"
                + brand + "\r\n";
    }

    /**
     * Plain-text body when an appointment time was changed.
     */
    public static String buildModifiedBody(Appointment appointment, String companyName) {
        Objects.requireNonNull(appointment, "appointment");
        TimeSlot slot = appointment.getTimeSlot();
        String patientName = appointment.getPatient() != null ? appointment.getPatient().getName() : "Customer";
        String brand = companyName != null && !companyName.isBlank() ? companyName.trim() : "Appointment Booking System";

        return "Dear " + patientName + ",\r\n\r\n"
                + "Your appointment with " + brand + " has been rescheduled.\r\n\r\n"
                + "New schedule\r\n"
                + "------------\r\n"
                + "Date:   " + formatAppointmentDate(slot) + "\r\n"
                + "Time:   " + formatAppointmentTimeRange(slot) + "\r\n"
                + "Type:   " + describeAppointmentType(appointment) + "\r\n"
                + "Status: " + appointment.getStatus() + "\r\n\r\n"
                + "Please update your calendar. If this change was not expected, contact us immediately.\r\n\r\n"
                + "Kind regards,\r\n"
                + brand + "\r\n";
    }

    /**
     * Plain-text body when an appointment was cancelled.
     */
    public static String buildCancelledBody(Appointment appointment, String companyName) {
        Objects.requireNonNull(appointment, "appointment");
        TimeSlot slot = appointment.getTimeSlot();
        String patientName = appointment.getPatient() != null ? appointment.getPatient().getName() : "Customer";
        String brand = companyName != null && !companyName.isBlank() ? companyName.trim() : "Appointment Booking System";

        return "Dear " + patientName + ",\r\n\r\n"
                + "This message confirms that your appointment with " + brand + " has been cancelled.\r\n\r\n"
                + "Previous schedule\r\n"
                + "-----------------\r\n"
                + "Date: " + formatAppointmentDate(slot) + "\r\n"
                + "Time: " + formatAppointmentTimeRange(slot) + "\r\n"
                + "Type: " + describeAppointmentType(appointment) + "\r\n\r\n"
                + "You may book a new appointment at any time through the application.\r\n\r\n"
                + "Kind regards,\r\n"
                + brand + "\r\n";
    }

    /**
     * Plain-text body for a reminder (future scheduled job).
     */
    public static String buildReminderBody(Appointment appointment, String detailsText, String companyName) {
        Objects.requireNonNull(appointment, "appointment");
        TimeSlot slot = appointment.getTimeSlot();
        String patientName = appointment.getPatient() != null ? appointment.getPatient().getName() : "Customer";
        String brand = companyName != null && !companyName.isBlank() ? companyName.trim() : "Appointment Booking System";
        String extra = detailsText != null && !detailsText.isBlank() ? "\r\nNote: " + detailsText.trim() + "\r\n" : "";

        return "Dear " + patientName + ",\r\n\r\n"
                + "This is a friendly reminder from " + brand + ".\r\n\r\n"
                + "You have an upcoming appointment:\r\n"
                + "Date: " + formatAppointmentDate(slot) + "\r\n"
                + "Time: " + formatAppointmentTimeRange(slot) + "\r\n"
                + "Type: " + describeAppointmentType(appointment) + "\r\n"
                + extra + "\r\n"
                + "Kind regards,\r\n"
                + brand + "\r\n";
    }
}
