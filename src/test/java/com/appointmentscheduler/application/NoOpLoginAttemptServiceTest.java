package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpLoginAttemptServiceTest {

    @Test
    void neverLocks() {
        LoginAttemptService s = new NoOpLoginAttemptService();
        s.recordFailure("a@b.com");
        s.clearFailures("a@b.com");
        assertThat(s.isLocked("a@b.com")).isFalse();
        assertThat(s.getRemainingLockMinutes("a@b.com")).isZero();
        assertThat(s.getFailureCount("a@b.com")).isZero();
    }
}
