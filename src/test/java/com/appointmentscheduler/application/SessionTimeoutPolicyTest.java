package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTimeoutPolicyTest {

    private final SessionTimeoutPolicy policy = new SessionTimeoutPolicy();

    @Test
    void evaluate_returnsNone_whenNoAuthenticatedUser() {
        SessionTimeoutPolicy.Action action = policy.evaluate(
                LocalDateTime.now().minusMinutes(120),
                LocalDateTime.now(),
                false,
                30,
                25,
                false
        );

        assertThat(action).isEqualTo(SessionTimeoutPolicy.Action.NONE);
    }

    @Test
    void evaluate_returnsWarn_whenBetweenWarningAndTimeout_andWarningNotShown() {
        LocalDateTime now = LocalDateTime.now();
        SessionTimeoutPolicy.Action action = policy.evaluate(
                now.minusMinutes(26),
                now,
                false,
                30,
                25,
                true
        );

        assertThat(action).isEqualTo(SessionTimeoutPolicy.Action.WARN);
    }

    @Test
    void evaluate_returnsNone_whenWarningAlreadyShown() {
        LocalDateTime now = LocalDateTime.now();
        SessionTimeoutPolicy.Action action = policy.evaluate(
                now.minusMinutes(26),
                now,
                true,
                30,
                25,
                true
        );

        assertThat(action).isEqualTo(SessionTimeoutPolicy.Action.NONE);
    }

    @Test
    void evaluate_returnsLogout_whenTimeoutExceeded() {
        LocalDateTime now = LocalDateTime.now();
        SessionTimeoutPolicy.Action action = policy.evaluate(
                now.minusMinutes(31),
                now,
                false,
                30,
                25,
                true
        );

        assertThat(action).isEqualTo(SessionTimeoutPolicy.Action.LOGOUT);
    }

    @Test
    void evaluate_returnsLogout_whenTimeoutIsNonPositive() {
        SessionTimeoutPolicy.Action action = policy.evaluate(
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                0,
                1,
                true
        );

        assertThat(action).isEqualTo(SessionTimeoutPolicy.Action.LOGOUT);
    }
}
