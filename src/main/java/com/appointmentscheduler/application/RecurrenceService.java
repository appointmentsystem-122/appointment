package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.RecurrencePattern;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages recurring appointments: creation, single-occurrence cancellation, series cancellation.
 * Reusable and testable recurrence logic.
 */
public class RecurrenceService {

    private final AppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    public RecurrenceService(AppointmentRepository appointmentRepository,
                             AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Creates a series of recurring appointments.
     */
    public List<RecurringAppointment> createRecurringSeries(User patient, RecurrencePattern pattern,
                                                            Duration slotDuration,
                                                            Appointment prototype) {
        List<LocalDateTime> starts = pattern.generateOccurrenceStarts();
        String seriesId = UUID.randomUUID().toString();
        List<RecurringAppointment> created = new ArrayList<>();
        for (LocalDateTime start : starts) {
            LocalDateTime end = start.plus(slotDuration);
            TimeSlot slot = new TimeSlot(start, end);
            RecurringAppointment appt = createRecurringInstance(patient, slot, seriesId, pattern, prototype);
            appointmentRepository.save(appt);
            created.add(appt);
        }
        return created;
    }

    private RecurringAppointment createRecurringInstance(User patient, TimeSlot slot, String seriesId,
                                                         RecurrencePattern pattern, Appointment prototype) {
        String occurrenceId = seriesId + "_" + slot.getStartTime().toEpochSecond(java.time.ZoneOffset.UTC);
        return new RecurringAppointment(patient, slot, seriesId, pattern, occurrenceId);
    }

    /**
     * Cancels a single occurrence of a recurring series.
     */
    public boolean cancelSingleOccurrence(String occurrenceId, User requester) {
        Optional<Appointment> opt = appointmentRepository.findById(occurrenceId);
        if (!opt.isPresent() || !(opt.get() instanceof RecurringAppointment)) return false;
        RecurringAppointment appt = (RecurringAppointment) opt.get();
        if (!appt.getPatient().equals(requester)) return false;
        if (appt.getTimeSlot().getStartTime().isBefore(LocalDateTime.now())) return false;
        appt.setStatus("CANCELLED");
        appointmentRepository.save(appt);
        auditLogService.log(requester, "CANCEL_RECURRENCE_SINGLE", "Occurrence " + occurrenceId, "RecurringAppointment", occurrenceId, appt.getStatus(), "CANCELLED");
        return true;
    }

    /**
     * Cancels the entire recurring series.
     */
    public int cancelEntireSeries(String seriesId, User requester) {
        List<Appointment> all = appointmentRepository.findAll();
        List<RecurringAppointment> series = all.stream()
                .filter(a -> a instanceof RecurringAppointment)
                .map(a -> (RecurringAppointment) a)
                .filter(r -> seriesId.equals(r.getSeriesId()))
                .filter(r -> r.getPatient().equals(requester))
                .filter(r -> r.getTimeSlot().getStartTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        for (RecurringAppointment r : series) {
            r.setStatus("CANCELLED");
            appointmentRepository.save(r);
            auditLogService.log(requester, "CANCEL_RECURRENCE_SERIES", "Series " + seriesId + " occurrence " + r.getId(), "RecurringAppointment", r.getId(), r.getStatus(), "CANCELLED");
        }
        return series.size();
    }

    /**
     * Finds all appointments in a series.
     */
    public List<RecurringAppointment> getSeriesOccurrences(String seriesId) {
        return appointmentRepository.findAll().stream()
                .filter(a -> a instanceof RecurringAppointment)
                .map(a -> (RecurringAppointment) a)
                .filter(r -> seriesId.equals(r.getSeriesId()))
                .collect(Collectors.toList());
    }
}
