package com.generic.appointments.strategy;

import com.generic.appointments.model.Appointment;

import java.time.Duration;

/**
 * Ensures that appointment duration is positive and does not exceed a maximum.
 */
public class DurationRule implements BookingRuleStrategy {

    private final Duration maxDuration;

    public DurationRule(Duration maxDuration) {
        this.maxDuration = maxDuration;
    }

    @Override
    public void validate(Appointment appointment) {
        Duration d = Duration.between(
            appointment.getTimeSlot().getStartTime(),
            appointment.getTimeSlot().getEndTime()
        );
        if (d.isNegative() || d.isZero() || d.compareTo(maxDuration) > 0) {
            throw new IllegalArgumentException("Invalid appointment duration");
        }
    }
}

