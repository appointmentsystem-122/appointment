package com.generic.appointments.strategy;

import com.generic.appointments.model.Appointment;

/**
 * Strategy for appointment validation rules.
 */
public interface BookingRuleStrategy {

    void validate(Appointment appointment) throws IllegalArgumentException;
}

