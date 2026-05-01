package com.appointmentscheduler.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Appointment lifecycle status.
 * Defines valid state transitions.
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    EXPIRED;

    private static final Set<AppointmentStatus> ACTIVE = EnumSet.of(PENDING, CONFIRMED);
    private static final Set<AppointmentStatus> TERMINAL = EnumSet.of(CANCELLED, COMPLETED, EXPIRED);

    /**
     * Whether this status allows modification or cancellation.
     */
    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    /**
     * Whether this status is terminal (no further transitions).
     */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * Valid transitions from this status.
     */
    public Set<AppointmentStatus> getAllowedTransitions() {
        switch (this) {
            case PENDING:
                return EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED:
                return EnumSet.of(CANCELLED, COMPLETED, EXPIRED);
            case CANCELLED:
            case COMPLETED:
            case EXPIRED:
                return EnumSet.noneOf(AppointmentStatus.class);
            default:
                return EnumSet.noneOf(AppointmentStatus.class);
        }
    }
}
