package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequestFieldsTest {

    @Test
    void applyTo_nullAppointment_noOp() {
        BookingRequestFields.applyTo(null, "x", "1", "SMS", "a", "en", 2, 5);
    }

    @Test
    void applyTo_clampsPartyAndTrimsFields() {
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L");

        String longNotes = "x".repeat(3000);
        BookingRequestFields.applyTo(a, longNotes, "  +123  ", "email", "wheel", "ar", 99, 3);

        assertThat(a.getParticipantCount()).isEqualTo(3);
        assertThat(a.getCustomerNotes()).hasSize(2000);
        assertThat(a.getContactPhone()).isEqualTo("+123");
        assertThat(a.getReminderChannel()).isEqualTo("EMAIL");
        assertThat(a.getAccessibilityNeeds()).isEqualTo("wheel");
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_AR);
    }

    @Test
    void normalizeReminder_unknownDefaultsToApp() {
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L");
        BookingRequestFields.applyTo(a, null, null, "weird", null, null, 1, 1);
        assertThat(a.getReminderChannel()).isEqualTo(BookingRequestFields.REMINDER_APP);
    }

    @Test
    void normalizeLanguage_detectsEnglishAndArabicHints() {
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L");
        BookingRequestFields.applyTo(a, null, null, "NONE", null, "ENGLISH", 1, 1);
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_EN);
        BookingRequestFields.applyTo(a, null, null, "NONE", null, "العربية", 1, 1);
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_AR);
    }

    @Test
    void applyTo_blankStringsBecomeNullAndClampsPartyWithLowCap() {
        User p = new User("u2", "N", "e2@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L");
        BookingRequestFields.applyTo(a, "   ", "  \t  ", "  sms  ", " ", null, 0, 0);
        assertThat(a.getCustomerNotes()).isNull();
        assertThat(a.getContactPhone()).isNull();
        assertThat(a.getReminderChannel()).isEqualTo(BookingRequestFields.REMINDER_SMS);
        assertThat(a.getAccessibilityNeeds()).isNull();
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_ANY);
        assertThat(a.getParticipantCount()).isEqualTo(1);
    }

    @Test
    void applyTo_normalizeReminderKnownCodes() {
        User p = new User("u3", "N", "e3@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L");
        BookingRequestFields.applyTo(a, null, null, "none", null, "ANY", 1, 4);
        assertThat(a.getReminderChannel()).isEqualTo(BookingRequestFields.REMINDER_NONE);
        BookingRequestFields.applyTo(a, null, null, "APP", null, "ANY", 1, 4);
        assertThat(a.getReminderChannel()).isEqualTo(BookingRequestFields.REMINDER_APP);
    }

    @Test
    void applyTo_normalizeLanguageArPrefixAndEnSubstring() {
        User p = new User("u4", "N", "e4@x.com", "x");
        LocalDateTime start = LocalDateTime.now().plusDays(4);
        InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L");
        BookingRequestFields.applyTo(a, null, null, "NONE", null, "ar-SA", 1, 1);
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_AR);
        BookingRequestFields.applyTo(a, null, null, "NONE", null, "x ENGL x", 1, 1);
        assertThat(a.getPreferredLanguage()).isEqualTo(BookingRequestFields.LANG_EN);
    }
}
