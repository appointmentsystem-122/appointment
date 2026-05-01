package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.TimeSlot;

import java.time.Duration;

/**
 * Ensures that the appointment duration does not exceed a maximum configured time.
 */
public class DurationRuleStrategy implements BookingRuleStrategy {
    private final long maxDurationMinutes;

    public DurationRuleStrategy(long maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null || appointment.getTimeSlot() == null) return false;
        
        TimeSlot slot = appointment.getTimeSlot();
        Duration duration = Duration.between(slot.getStartTime(), slot.getEndTime());
        
        return duration.toMinutes() <= maxDurationMinutes;
    }
}
