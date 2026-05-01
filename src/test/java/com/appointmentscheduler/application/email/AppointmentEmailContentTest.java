package com.appointmentscheduler.application.email;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentEmailContentTest {

    @Test
    void bookingBodyIncludesPatientDateTimeTypeStatusAndBrand() {
        User u = new User("1", "Jane Doe", "jane@test.com", "p");
        LocalDateTime start = LocalDateTime.of(2026, 6, 15, 14, 30);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        InPersonAppointment appt = new InPersonAppointment(u, slot, "Main office");
        appt.setStatus("CONFIRMED");

        String body = AppointmentEmailContent.buildBookingConfirmationBody(appt, "Acme Scheduling");

        assertThat(body)
                .contains("Dear Jane Doe")
                .contains("Acme Scheduling")
                .contains("Monday, June 15, 2026")
                .contains("14:30")
                .contains("In person")
                .contains("CONFIRMED")
                .contains(appt.getId());
    }

    @Test
    void describeType_inPerson() {
        User u = new User("1", "A", "a@b.com", "p");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        InPersonAppointment appt = new InPersonAppointment(u, slot, "x");
        assertThat(AppointmentEmailContent.describeAppointmentType(appt)).isEqualTo("In person");
    }
}
