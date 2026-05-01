package com.appointmentscheduler.application;

import com.appointmentscheduler.application.email.JakartaMailEmailNotificationService;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Covers transport guards and (with mocked {@link Transport}) the send path for JaCoCo.
 */
class JakartaMailEmailNotificationServiceTest {

    @AfterEach
    void restoreConfig() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void sendBookingConfirmation_noOpWhenEmailDisabled() {
        Properties p = new Properties();
        p.setProperty("email.enabled", "false");
        AppConfig.applyPropertiesForTest(p);

        JakartaMailEmailNotificationService svc = new JakartaMailEmailNotificationService();
        InPersonAppointment appt = sampleAppt("recipient@test.com");

        assertThatCode(() -> svc.sendBookingConfirmation(appt)).doesNotThrowAnyException();
    }

    @Test
    void sendMethods_skipWhenNoRecipientEmail() {
        Properties p = new Properties();
        p.setProperty("email.enabled", "true");
        p.setProperty("email.auth.username", "sender@test.com");
        p.setProperty("email.auth.password", "secret");
        AppConfig.applyPropertiesForTest(p);

        JakartaMailEmailNotificationService svc = new JakartaMailEmailNotificationService();

        InPersonAppointment blankEmail = new InPersonAppointment(
                new BlankEmailUser(),
                slot(),
                "L"
        );

        assertThatCode(() -> {
            svc.sendBookingConfirmation(blankEmail);
            svc.sendAppointmentModified(blankEmail);
            svc.sendAppointmentCancelled(blankEmail);
            svc.sendAppointmentReminder(blankEmail, "note");
        }).doesNotThrowAnyException();

        NullPatientAppt nullPat = new NullPatientAppt(new User("1", "N", "n@n.com", "p"), slot(), "L");
        assertThatCode(() -> {
            svc.sendBookingConfirmation(nullPat);
            svc.sendAppointmentModified(nullPat);
            svc.sendAppointmentCancelled(nullPat);
            svc.sendAppointmentReminder(nullPat, null);
        }).doesNotThrowAnyException();
    }

    @Test
    void send_skipsWhenEnabledButCredentialsMissing() {
        Properties p = new Properties();
        p.setProperty("email.enabled", "true");
        p.setProperty("email.auth.username", "");
        p.setProperty("email.auth.password", "");
        AppConfig.applyPropertiesForTest(p);

        JakartaMailEmailNotificationService svc = new JakartaMailEmailNotificationService();
        InPersonAppointment appt = sampleAppt("recipient@test.com");

        assertThatCode(() -> {
            svc.sendBookingConfirmation(appt);
            svc.sendAppointmentModified(appt);
            svc.sendAppointmentCancelled(appt);
            svc.sendAppointmentReminder(appt, "x");
        }).doesNotThrowAnyException();
    }

    @Test
    void send_allPublicMethods_withMockedTransport() {
        Properties p = new Properties();
        p.setProperty("email.enabled", "true");
        p.setProperty("email.auth.username", "sender@test.com");
        p.setProperty("email.auth.password", "secret");
        p.setProperty("email.smtp.host", "localhost");
        p.setProperty("email.smtp.port", "587");
        p.setProperty("email.smtp.starttls", "false");
        AppConfig.applyPropertiesForTest(p);

        JakartaMailEmailNotificationService svc = new JakartaMailEmailNotificationService();
        InPersonAppointment appt = sampleAppt("recipient@test.com");

        try (MockedStatic<Transport> tr = mockStatic(Transport.class)) {
            tr.when(() -> Transport.send(any(Message.class))).thenAnswer(inv -> null);

            assertThatCode(() -> {
                svc.sendBookingConfirmation(appt);
                svc.sendAppointmentModified(appt);
                svc.sendAppointmentCancelled(appt);
                svc.sendAppointmentReminder(appt, null);
                svc.sendAppointmentReminder(appt, "  Reminder note  ");
            }).doesNotThrowAnyException();
        }
    }

    @Test
    void sendMime_wrapsException() {
        Properties p = new Properties();
        p.setProperty("email.enabled", "true");
        p.setProperty("email.auth.username", "sender@test.com");
        p.setProperty("email.auth.password", "secret");
        p.setProperty("email.smtp.starttls", "true");
        AppConfig.applyPropertiesForTest(p);

        JakartaMailEmailNotificationService svc = new JakartaMailEmailNotificationService();
        InPersonAppointment appt = sampleAppt("recipient@test.com");

        try (MockedStatic<Transport> tr = mockStatic(Transport.class)) {
            // Match the actual overload: Transport.send(Message) — not send(MimeMessage).
            tr.when(() -> Transport.send(any(Message.class))).thenThrow(new jakarta.mail.MessagingException("boom"));

            assertThatThrownBy(() -> svc.sendBookingConfirmation(appt))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Email send failed");
        }
    }

    private static TimeSlot slot() {
        LocalDateTime s = LocalDateTime.now().plusDays(1).withNano(0);
        return new TimeSlot(s, s.plusHours(1));
    }

    private static InPersonAppointment sampleAppt(String email) {
        User u = new User("1", "T", email, "p");
        InPersonAppointment appt = new InPersonAppointment(u, slot(), "L");
        appt.setStatus("CONFIRMED");
        return appt;
    }

    private static final class NullPatientAppt extends InPersonAppointment {
        NullPatientAppt(User patient, TimeSlot timeSlot, String location) {
            super(patient, timeSlot, location);
        }

        @Override
        public User getPatient() {
            return null;
        }
    }

    /** Valid {@link User} construction but blank email for skip-warn paths. */
    private static final class BlankEmailUser extends User {
        BlankEmailUser() {
            super("1", "N", "n@n.com", "p");
        }

        @Override
        public String getEmail() {
            return "   ";
        }
    }
}
