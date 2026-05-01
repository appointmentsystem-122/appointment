package com.appointmentscheduler.application.email;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.User;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;

/**
 * Sends email via Gmail-compatible SMTP using Jakarta Mail (TLS on port 587).
 * <p>
 * <strong>Configuration:</strong> set {@code email.enabled=true} in {@code application.properties}
 * and provide credentials via environment variables {@code SENDER_EMAIL} and {@code APP_PASSWORD}
 * (Gmail App Password), or set {@code email.auth.username} / {@code email.auth.password} in the
 * properties file for local development (avoid committing real secrets).
 * </p>
 */
public final class JakartaMailEmailNotificationService implements EmailNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(JakartaMailEmailNotificationService.class);

    private static final String SUBJECT_CONFIRM = "Appointment Booking Confirmation";
    private static final String SUBJECT_MODIFIED = "Appointment Rescheduled";
    private static final String SUBJECT_CANCELLED = "Appointment Cancelled";
    private static final String SUBJECT_REMINDER = "Appointment Reminder";

    @Override
    public void sendBookingConfirmation(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        if (!isActive()) {
            return;
        }
        User patient = appointment.getPatient();
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            log.warn("Skipping booking confirmation email: no recipient email on appointment {}", appointment.getId());
            return;
        }
        String body = AppointmentEmailContent.buildBookingConfirmationBody(appointment, AppConfig.getBrandName());
        sendMime(patient.getEmail(), SUBJECT_CONFIRM, body);
    }

    @Override
    public void sendAppointmentModified(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        if (!isActive()) {
            return;
        }
        User patient = appointment.getPatient();
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            log.warn("Skipping modification email: no recipient email on appointment {}", appointment.getId());
            return;
        }
        String body = AppointmentEmailContent.buildModifiedBody(appointment, AppConfig.getBrandName());
        sendMime(patient.getEmail(), SUBJECT_MODIFIED, body);
    }

    @Override
    public void sendAppointmentCancelled(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        if (!isActive()) {
            return;
        }
        User patient = appointment.getPatient();
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            log.warn("Skipping cancellation email: no recipient email on appointment {}", appointment.getId());
            return;
        }
        String body = AppointmentEmailContent.buildCancelledBody(appointment, AppConfig.getBrandName());
        sendMime(patient.getEmail(), SUBJECT_CANCELLED, body);
    }

    @Override
    public void sendAppointmentReminder(Appointment appointment, String detailsText) {
        Objects.requireNonNull(appointment, "appointment");
        if (!isActive()) {
            return;
        }
        User patient = appointment.getPatient();
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            log.warn("Skipping reminder email: no recipient email on appointment {}", appointment.getId());
            return;
        }
        String body = AppointmentEmailContent.buildReminderBody(appointment, detailsText, AppConfig.getBrandName());
        sendMime(patient.getEmail(), SUBJECT_REMINDER, body);
    }

    private static boolean isActive() {
        if (!AppConfig.isEmailEnabled()) {
            log.debug("Email notifications are disabled (email.enabled=false).");
            return false;
        }
        if (!credentialsPresent()) {
            log.warn("Email is enabled but sender credentials are missing; set SENDER_EMAIL and APP_PASSWORD or email.auth.* in application.properties.");
            return false;
        }
        return true;
    }

    private static boolean credentialsPresent() {
        return !resolveUsername().isEmpty() && !resolvePassword().isEmpty();
    }

    /**
     * Environment variables override properties (safer for production deployments).
     */
    private static String resolveUsername() {
        String env = System.getenv("SENDER_EMAIL");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return AppConfig.get("email.auth.username", "").trim();
    }

    private static String resolvePassword() {
        String env = System.getenv("APP_PASSWORD");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return AppConfig.get("email.auth.password", "").trim();
    }

    private void sendMime(String toAddress, String subject, String bodyText) {
        try {
            Session session = createSmtpSession();
            MimeMessage msg = new MimeMessage(session);
            String from = resolveUsername();
            InternetAddress fromAddr = new InternetAddress(from);
            try {
                fromAddr.setPersonal(AppConfig.getBrandName(), "UTF-8");
            } catch (java.io.UnsupportedEncodingException ignored) {
                // UTF-8 is always supported; fallback keeps address-only From header
            }
            msg.setFrom(fromAddr);
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress, false));
            msg.setSubject(subject, "UTF-8");
            msg.setText(bodyText, "UTF-8");
            Transport.send(msg);
            log.info("Email sent: subject=\"{}\" to={}", subject, toAddress);
        } catch (Exception e) {
            log.error("Failed to send email (subject=\"{}\")", subject, e);
            throw new IllegalStateException("Email send failed: " + e.getMessage(), e);
        }
    }

    private static Session createSmtpSession() {
        Properties props = new Properties();
        String host = AppConfig.getEmailSmtpHost();
        int port = AppConfig.getEmailSmtpPort();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", AppConfig.isEmailStartTls() ? "true" : "false");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");

        final String user = resolveUsername();
        final String pass = resolvePassword();

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }
}
