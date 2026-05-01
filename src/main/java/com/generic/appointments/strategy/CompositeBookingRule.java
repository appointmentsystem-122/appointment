package com.generic.appointments.strategy;

import com.generic.appointments.model.Appointment;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite of multiple booking rules.
 */
public class CompositeBookingRule implements BookingRuleStrategy {

    private final List<BookingRuleStrategy> rules = new ArrayList<>();

    public CompositeBookingRule addRule(BookingRuleStrategy rule) {
        rules.add(rule);
        return this;
    }

    @Override
    public void validate(Appointment appointment) {
        for (BookingRuleStrategy rule : rules) {
            rule.validate(appointment);
        }
    }
}

