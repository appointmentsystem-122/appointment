package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptServiceTest {

    @Test
    void nullEmailIgnored() {
        InMemoryLoginAttemptService s = new InMemoryLoginAttemptService();
        s.recordFailure(null);
        s.recordFailure("");
        s.clearFailures(null);
        assertThat(s.getFailureCount(null)).isZero();
    }

    @Test
    void lockAfterFiveFailuresThenClear() {
        InMemoryLoginAttemptService s = new InMemoryLoginAttemptService();
        String email = "lock-test@example.com";
        for (int i = 0; i < 5; i++) {
            s.recordFailure(email);
        }
        assertThat(s.isLocked(email)).isTrue();
        s.clearFailures(email);
        assertThat(s.isLocked(email)).isFalse();
        assertThat(s.getFailureCount(email)).isZero();
    }

    @Test
    void treatsEmailCaseInsensitively() {
        InMemoryLoginAttemptService s = new InMemoryLoginAttemptService();
        s.recordFailure("User@Example.com");
        s.recordFailure("user@example.com");
        assertThat(s.getFailureCount("USER@example.com")).isEqualTo(2);
    }

    @Test
    void remainingLockMinutes_positiveWhenLocked() {
        InMemoryLoginAttemptService s = new InMemoryLoginAttemptService();
        String email = "remaining@example.com";
        for (int i = 0; i < 5; i++) {
            s.recordFailure(email);
        }
        assertThat(s.isLocked(email)).isTrue();
        assertThat(s.getRemainingLockMinutes(email)).isGreaterThan(0);
    }

    @Test
    void emptyEmailQueriesReturnSafeDefaults() {
        InMemoryLoginAttemptService s = new InMemoryLoginAttemptService();
        assertThat(s.isLocked("")).isFalse();
        assertThat(s.getRemainingLockMinutes("")).isZero();
        assertThat(s.getFailureCount("")).isZero();
    }
}
