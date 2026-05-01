package com.appointmentscheduler.application.email;

import com.appointmentscheduler.domain.Appointment;

/**
 * Outbound port for transactional email notifications (hexagonal / clean architecture).
 * <p>
 * Implementations send real email (e.g. SMTP) or no-op when disabled in configuration.
 * Extend this contract for reminders, cancellations, and reschedule notices without changing
 * core booking logic beyond calling the appropriate method after a successful state change.
 * </p>
 */
public interface EmailNotificationPort {

    /**
     * Sends a booking confirmation to the patient's email address after a successful reservation.
     *
     * @param appointment the saved appointment (must include patient and time slot)
     */
    void sendBookingConfirmation(Appointment appointment);

    /**
     * Notifies the patient that an existing appointment was rescheduled.
     *
     * @param appointment the appointment after modification
     */
    void sendAppointmentModified(Appointment appointment);

    /**
     * Notifies the patient that an appointment was cancelled.
     *
     * @param appointment the persisted appointment after status was set to cancelled
     */
    void sendAppointmentCancelled(Appointment appointment);

    /**
     * Reserved for scheduled reminder jobs (e.g. 24h before). Body text may include slot details.
     *
     * @param appointment the appointment to remind about
     * @param detailsText contextual line (e.g. reminder policy or notes)
     */
    void sendAppointmentReminder(Appointment appointment, String detailsText);
}
