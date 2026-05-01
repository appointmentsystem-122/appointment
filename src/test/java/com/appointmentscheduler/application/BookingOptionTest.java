package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingOptionTest {

    @Test
    void displayLabel_andEquality() {
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Express", 30, 1);
        BookingOption o1 = BookingOption.of(t, true);
        BookingOption o2 = BookingOption.of(t, true);
        assertThat(o1.getDisplayLabel()).contains("Express").contains("min");
        assertThat(o1).isEqualTo(o2);
        assertThat(o1.getId()).isNotBlank();
    }
}
