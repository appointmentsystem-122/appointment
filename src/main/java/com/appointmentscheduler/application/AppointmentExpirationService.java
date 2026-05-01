package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AppointmentStatus;
import com.appointmentscheduler.persistence.AppointmentRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically expires appointments when their time passes.
 * Marks CONFIRMED/PENDING appointments as EXPIRED after end time.
 */
public class AppointmentExpirationService {

    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    public AppointmentExpirationService(AppointmentRepository appointmentRepository,
                                        AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Expires all past appointments that are still active.
     * Should be called periodically or before critical operations.
     */
    public int expirePastAppointments() {
        List<Appointment> all = appointmentRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int expired = 0;
        for (Appointment a : all) {
            if (a.getTimeSlot().getEndTime().isBefore(now)) {
                String status = a.getStatus();
                if ("CONFIRMED".equals(status) || "PENDING".equals(status)) {
                    a.setStatus(AppointmentStatus.EXPIRED.name());
                    appointmentRepository.save(a);
                    auditLogService.log("System", "System", "EXPIRE", "Appointment " + a.getId() + " auto-expired");
                    expired++;
                }
            }
        }
        return expired;
    }
}
