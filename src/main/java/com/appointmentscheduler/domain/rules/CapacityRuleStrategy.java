package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.GroupAppointment;

/**
 * Ensures that group appointments do not exceed their maximum capacity,
 * and individual/other appointments do not exceed 1 participant.
 */
public class CapacityRuleStrategy implements BookingRuleStrategy {

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null) return false;

        if (appointment instanceof GroupAppointment) {
            GroupAppointment groupAppointment = (GroupAppointment) appointment;
            return groupAppointment.getParticipantCount() <= groupAppointment.getMaxCapacity();
        } else {
            // Non-group appointments must have exactly 1 participant mapped to the patient,
            // or maybe strict up limits. Assuming 1 for individual forms.
            return appointment.getParticipantCount() == 1;
        }
    }
}
