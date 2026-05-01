package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.persistence.AppointmentRepository;

import java.util.List;

/**
 * Ensures no double-booking for the same room (overlapping slot).
 */
public class RoomConflictRuleStrategy implements BookingRuleStrategy {

    private final AppointmentRepository appointmentRepository;

    public RoomConflictRuleStrategy(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null || appointment.getTimeSlot() == null) return true;
        String roomId = appointment.getRoomId();
        if (roomId == null || roomId.isEmpty()) return true;
        List<Appointment> all = appointmentRepository.findAll();
        for (Appointment a : all) {
            if (a == null || a.isDeleted() || "CANCELLED".equals(a.getStatus()) || "EXPIRED".equals(a.getStatus())) continue;
            if (roomId.equals(a.getRoomId()) && a.getTimeSlot().overlapsWith(appointment.getTimeSlot())) {
                if (!a.getId().equals(appointment.getId())) return false;
            }
        }
        return true;
    }
}
