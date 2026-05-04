package com.appointmentscheduler.application;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Pure decision logic for session timeout behavior.
 */
public final class SessionTimeoutPolicy {

    public enum Action {
        NONE,
        WARN,
        LOGOUT
    }

    public Action evaluate(LocalDateTime lastActivity,
                           LocalDateTime now,
                           boolean warningShown,
                           long timeoutMinutes,
                           long warningMinutes,
                           boolean hasAuthenticatedUser) {
        if (!hasAuthenticatedUser || lastActivity == null || now == null) {
            return Action.NONE;
        }
        if (timeoutMinutes <= 0) {
            return Action.LOGOUT;
        }

        long inactiveMinutes = ChronoUnit.MINUTES.between(lastActivity, now);
        if (inactiveMinutes >= timeoutMinutes) {
            return Action.LOGOUT;
        }
        if (warningMinutes > 0 && inactiveMinutes >= warningMinutes && !warningShown) {
            return Action.WARN;
        }
        return Action.NONE;
    }
}
