package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchSummaryTest {

    @Test
    void forbidden() {
        DispatchSummary d = DispatchSummary.forbidden();
        assertThat(d.isForbidden()).isTrue();
        assertThat(d.getSuccessCount()).isZero();
        assertThat(d.getFailureCount()).isZero();
        assertThat(d.getSkipped()).isZero();
        assertThat(d.getMessage()).contains("authorized");
    }

    @Test
    void empty() {
        DispatchSummary d = DispatchSummary.empty("reason");
        assertThat(d.isForbidden()).isFalse();
        assertThat(d.getMessage()).isEqualTo("reason");
    }

    @Test
    void of() {
        DispatchSummary d = DispatchSummary.of(2, 1, 3, "done");
        assertThat(d.getSuccessCount()).isEqualTo(2);
        assertThat(d.getFailureCount()).isEqualTo(1);
        assertThat(d.getSkipped()).isEqualTo(3);
        assertThat(d.getMessage()).isEqualTo("done");
        assertThat(d.isForbidden()).isFalse();
    }
}
