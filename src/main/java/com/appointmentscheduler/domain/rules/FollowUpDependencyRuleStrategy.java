package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.persistence.AppointmentRepository;

import java.util.Optional;

/**
 * Enforces that follow-up appointments require a prior completed appointment.
 */
public class FollowUpDependencyRuleStrategy implements BookingRuleStrategy {

    private final AppointmentRepository appointmentRepository;

    public FollowUpDependencyRuleStrategy(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public boolean isValid(Appointment appointment) {
        if (!(appointment instanceof FollowUpAppointment)) return true;
        FollowUpAppointment fu = (FollowUpAppointment) appointment;
        if (!fu.hasPriorAppointment()) return true;
        Optional<Appointment> prior = appointmentRepository.findById(fu.getPriorAppointmentId());
        if (prior.isEmpty()) return false;
        Appointment p = prior.get();
        if (!p.getPatient().equals(appointment.getPatient())) return false;
        boolean priorCompleted = "COMPLETED".equals(p.getStatus()) || "CONFIRMED".equals(p.getStatus());
        boolean priorBeforeFollowUp = p.getTimeSlot().getEndTime().isBefore(appointment.getTimeSlot().getStartTime());
        return priorCompleted && priorBeforeFollowUp;
    }
}
