package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentBookingFieldsTest {

    @Test
    void bookingExtras_roundTrip() {
        User patient = new User("p1", "P", "p@t.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        InPersonAppointment a = new InPersonAppointment(patient, slot, "Room A");

        a.setCustomerNotes("Allergies: none");
        a.setContactPhone("+962700000000");
        a.setReminderChannel("SMS");
        a.setAccessibilityNeeds("Wheelchair");
        a.setPreferredLanguage("AR");

        assertThat(a.getCustomerNotes()).isEqualTo("Allergies: none");
        assertThat(a.getContactPhone()).isEqualTo("+962700000000");
        assertThat(a.getReminderChannel()).isEqualTo("SMS");
        assertThat(a.getAccessibilityNeeds()).isEqualTo("Wheelchair");
        assertThat(a.getPreferredLanguage()).isEqualTo("AR");
    }
}
