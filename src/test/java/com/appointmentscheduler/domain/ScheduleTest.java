package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleTest {

    @Test
    void add_clear_overlapFiltersDeletedAndCancelled() {
        Schedule s = new Schedule();
        User p = new User("u", "N", "e@x.com", "x");
        LocalDateTime t0 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot window = new TimeSlot(t0, t0.plusHours(1));

        InPersonAppointment a1 = new InPersonAppointment(p, window, "R1");
        s.addAppointment(a1);
        assertThat(s.getAllAppointments()).hasSize(1);
        assertThat(s.getOverlappingAppointments(window)).hasSize(1);

        s.clear();
        assertThat(s.getAllAppointments()).isEmpty();

        s.addAppointment(null);
        assertThat(s.getAllAppointments()).isEmpty();

        InPersonAppointment a2 = new InPersonAppointment(p, window, "R2");
        a2.markDeleted("test");
        s.addAppointment(a2);
        assertThat(s.getOverlappingAppointments(window)).isEmpty();

        s.clear();
        InPersonAppointment a3 = new InPersonAppointment(p, window, "R3");
        a3.setStatus("CANCELLED");
        s.addAppointment(a3);
        assertThat(s.getOverlappingAppointments(window)).isEmpty();
    }
}
