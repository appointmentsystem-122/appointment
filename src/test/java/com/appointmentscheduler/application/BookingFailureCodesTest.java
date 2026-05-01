package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingFailureCodesTest {

    @Test
    void stableCode() {
        assertThat(BookingFailureCodes.OPEN_APPOINTMENT_NOT_COMPLETED).isEqualTo("OPEN_APPOINTMENT_NOT_COMPLETED");
    }
}
