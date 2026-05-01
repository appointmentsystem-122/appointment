package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.persistence.AppointmentRepository;
import com.appointmentscheduler.persistence.DoctorRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ensures a doctor does not exceed their max appointments per day.
 */
public class MaxAppointmentsPerDoctorRuleStrategy implements BookingRuleStrategy {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public MaxAppointmentsPerDoctorRuleStrategy(AppointmentRepository appointmentRepository,
                                               DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public boolean isValid(Appointment appointment) {
        if (appointment == null || appointment.getTimeSlot() == null) return true;
        String doctorId = appointment.getDoctorId();
        if (doctorId == null || doctorId.isEmpty()) return true;
        Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
        if (!docOpt.isPresent()) return true;
        Doctor doc = docOpt.get();
        LocalDate day = appointment.getTimeSlot().getStartTime().toLocalDate();
        long count = appointmentRepository.findAll().stream()
            .filter(Objects::nonNull)
            .filter(a -> !a.isDeleted())
            .filter(a -> !"CANCELLED".equals(a.getStatus()) && !"EXPIRED".equals(a.getStatus()))
            .filter(a -> doctorId.equals(a.getDoctorId()))
            .filter(a -> a.getTimeSlot().getStartTime().toLocalDate().equals(day))
            .count();
        return count < doc.getMaxAppointmentsPerDay();
    }
}
