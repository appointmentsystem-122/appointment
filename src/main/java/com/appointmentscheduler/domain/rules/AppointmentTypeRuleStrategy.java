package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;

/**
 * Applies different specific business rules based on the appointment type.
 */
public class AppointmentTypeRuleStrategy implements BookingRuleStrategy {

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null) return false;

        if (appointment instanceof VirtualAppointment) {
            VirtualAppointment va = (VirtualAppointment) appointment;
            // Virtual appointments must have a meeting link (can be generated later, but shouldn't be empty if confirmed)
            return va.getMeetingLink() != null && !va.getMeetingLink().isEmpty();
        }

        if (appointment instanceof InPersonAppointment) {
            InPersonAppointment ipa = (InPersonAppointment) appointment;
            // In-person appointments must have a location
            return ipa.getLocation() != null && !ipa.getLocation().isEmpty();
        }

        return true; // Default fallback for other types
    }
}
