package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.domain.Appointment;

/**
 * Enforces booking within business working hours.
 * Slot must start at or after startHour and end by endHour (inclusive).
 */
public class WorkingHoursRuleStrategy implements BookingRuleStrategy {

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null || appointment.getTimeSlot() == null) return false;
        int startHour = AppConfig.getBusinessHourStart();
        int endHour = AppConfig.getBusinessHourEnd();
        int slotStartHour = appointment.getTimeSlot().getStartTime().getHour();
        int slotEndHour = appointment.getTimeSlot().getEndTime().getHour();
        int slotEndMinute = appointment.getTimeSlot().getEndTime().getMinute();
        // Start must be within working hours (>= startHour)
        boolean startOk = slotStartHour >= startHour;
        // End must be by end of working hours: before (endHour+1):00, so endHour:59 is allowed
        boolean endOk = slotEndHour < endHour || (slotEndHour == endHour && slotEndMinute <= 59);
        return startOk && endOk;
    }
}
