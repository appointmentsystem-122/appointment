package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @Test
    void coversLifecycleHelpers() {
        assertThat(AppointmentStatus.PENDING.isActive()).isTrue();
        assertThat(AppointmentStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(AppointmentStatus.CONFIRMED.getAllowedTransitions()).contains(AppointmentStatus.COMPLETED);
        assertThat(AppointmentStatus.CANCELLED.getAllowedTransitions()).isEmpty();
    }
}
