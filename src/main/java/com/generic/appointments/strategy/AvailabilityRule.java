package com.generic.appointments.strategy;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.AppointmentRepository;

/**
 * Ensures a time slot is available and not already booked.
 */
public class AvailabilityRule implements BookingRuleStrategy {

    private final AppointmentRepository appointmentRepository;

    public AvailabilityRule(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void validate(Appointment appointment) {
        TimeSlot slot = appointment.getTimeSlot();
        if (!slot.isAvailable()) {
            throw new IllegalArgumentException("Time slot is not available");
        }

        boolean alreadyBooked = appointmentRepository.findByTimeSlot(slot).stream()
            .anyMatch(a -> a.getStatus() != AppointmentStatus.CANCELLED);

        if (alreadyBooked) {
            throw new IllegalArgumentException("Time slot is already booked");
        }
    }
}

