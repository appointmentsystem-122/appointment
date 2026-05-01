package com.generic.appointments.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericModelTest {

    @Test
    void timeSlot_rejectsEndBeforeOrEqualStart() {
        LocalDateTime s = LocalDateTime.of(2026, 1, 1, 10, 0);
        assertThatThrownBy(() -> new TimeSlot(null, s, s, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TimeSlot(null, s, s.minusMinutes(1), true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timeSlot_setsAvailability() {
        LocalDateTime s = LocalDateTime.of(2026, 1, 1, 9, 0);
        TimeSlot t = new TimeSlot("id1", s, s.plusHours(1), true);
        t.setAvailable(false);
        assertThat(t.isAvailable()).isFalse();
    }

    @Test
    void appointment_setStatus() {
        Customer c = new Customer(null, "N", "e@x.com");
        LocalDateTime a = LocalDateTime.of(2026, 2, 1, 11, 0);
        TimeSlot slot = new TimeSlot(null, a, a.plusHours(1), true);
        Appointment ap = new Appointment(null, c, slot, AppointmentStatus.PENDING);
        ap.setStatus(AppointmentStatus.CONFIRMED);
        assertThat(ap.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void companyAndSchedule() {
        Schedule sch = new Schedule();
        LocalDateTime st = LocalDateTime.of(2026, 3, 1, 8, 0);
        TimeSlot sl = new TimeSlot(null, st, st.plusMinutes(30), true);
        sch.addTimeSlot(sl);
        assertThat(sch.getTimeSlots()).hasSize(1);
        Company co = new Company(null, "Acme", sch);
        Administrator adm = new Administrator(null, "A", "a@x.com", "admin", "hash");
        co.addAdministrator(adm);
        assertThat(co.getAdministrators()).containsExactly(adm);
        assertThat(co.getCompanyName()).isEqualTo("Acme");
    }

    @Test
    void appointmentStatus_values() {
        assertThat(AppointmentStatus.values()).containsExactly(
                AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED);
    }

    @Test
    void companyAndAdministrator_requireNonNullArguments() {
        Schedule sch = new Schedule();
        assertThatThrownBy(() -> new Company(null, null, sch)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Company(null, "Acme", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Administrator(null, "A", "a@x.com", null, "h"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Administrator(null, "A", "a@x.com", "u", null))
                .isInstanceOf(NullPointerException.class);
    }
}
