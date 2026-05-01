package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.domain.Appointment;

import java.time.LocalDateTime;

/**
 * Enforces minimum time before appointment for booking.
 * No booking past allowed cut-off time.
 */
public class BookingCutoffRuleStrategy implements BookingRuleStrategy {

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null || appointment.getTimeSlot() == null) return false;
        int cutoffHours = AppConfig.getInt("booking.cutoffHoursBefore", 2);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = appointment.getTimeSlot().getStartTime().minusHours(cutoffHours);
        return now.isBefore(cutoff) || now.equals(cutoff);
    }
}
