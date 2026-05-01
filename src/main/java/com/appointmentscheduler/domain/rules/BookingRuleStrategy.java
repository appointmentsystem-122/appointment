package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;

/**
 * Strategy interface for enforcing booking business rules.
 */
public interface BookingRuleStrategy {
    
    /**
     * Evaluates if the given appointment is valid according to the rule.
     * @param appointment the appointment to check
     * @return true if valid, false otherwise
     */
    boolean isValid(Appointment appointment);
}
