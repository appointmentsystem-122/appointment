package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.Schedule;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.persistence.AppointmentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing the aggregated schedule and slot availability.
 */
public class ScheduleService {

    private final AppointmentRepository appointmentRepository;
    private final Schedule masterSchedule;
    private ClosedDayService closedDayService;

    public ScheduleService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.masterSchedule = new Schedule();
        loadSchedule();
    }

    /** Optional: set to respect admin-closed days. */
    public void setClosedDayService(ClosedDayService closedDayService) {
        this.closedDayService = closedDayService;
    }

    /**
     * Re-loads from the repository (synchronization). Clears in-memory schedule first to avoid duplicates.
     */
    public void loadSchedule() {
        masterSchedule.clear();
        List<Appointment> all = appointmentRepository.findAll();
        all.stream()
                .filter(Objects::nonNull)
                .filter(a -> !a.isDeleted())
                .forEach(masterSchedule::addAppointment);
    }

    /**
     * Finds available 1-hour slots on a given day (business hours).
     */
    public List<TimeSlot> getAvailableSlots(java.time.LocalDate date) {
        return getAvailableSlots(date, 60);
    }

    /**
     * Finds available slots of given duration (e.g. 30 min) on a given day.
     * Used for enterprise booking: "next available" and slot grid.
     */
    public List<TimeSlot> getAvailableSlots(java.time.LocalDate date, int durationMinutes) {
        if (closedDayService != null && closedDayService.isDayClosed(date)) return new ArrayList<>();
        List<TimeSlot> availableSlots = new ArrayList<>();
        int startHour = AppConfig.getBusinessHourStart();
        int endHour = AppConfig.getBusinessHourEnd();
        LocalDateTime dayStart = date.atTime(startHour, 0);
        LocalDateTime dayEnd = date.atTime(endHour, 0);
        LocalDateTime current = dayStart;
        while (!current.plusMinutes(durationMinutes).isAfter(dayEnd)) {
            TimeSlot proposedSlot = new TimeSlot(current, current.plusMinutes(durationMinutes));
            List<Appointment> overlapping = masterSchedule.getOverlappingAppointments(proposedSlot);
            boolean isBooked = overlapping.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(a -> a.getStatus() != null && !"CANCELLED".equals(a.getStatus()));
            if (!isBooked) availableSlots.add(proposedSlot);
            current = current.plusMinutes(30);
        }
        return availableSlots;
    }

    /**
     * True if there is at least one bookable slot on this calendar day: not in the past as a day,
     * not an admin-closed day, not fully booked, and (for today) at least one slot starting at or after now.
     *
     * @param durationMinutes same granularity as {@link #getAvailableSlots(LocalDate, int)} (e.g. 60 for the simple book screen, 30 for patient chips)
     */
    public boolean isDateBookable(java.time.LocalDate date, int durationMinutes) {
        if (date == null) return false;
        java.time.LocalDate today = java.time.LocalDate.now();
        if (date.isBefore(today)) return false;
        List<TimeSlot> slots = getAvailableSlots(date, durationMinutes);
        if (slots.isEmpty()) return false;
        if (date.equals(today)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            return slots.stream().anyMatch(s -> !s.getStartTime().isBefore(now));
        }
        return true;
    }

    /** Same duration as {@link #getAvailableSlots(LocalDate)} (60 minutes). */
    public boolean isDateBookable(java.time.LocalDate date) {
        return isDateBookable(date, 60);
    }

    /**
     * Finds the next available slot from now (today or future days).
     * Returns empty if no slot found within a reasonable window (e.g. 30 days).
     */
    public Optional<TimeSlot> getNextAvailableSlot(int durationMinutes) {
        java.time.LocalDate date = java.time.LocalDate.now();
        for (int i = 0; i < 30; i++) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            for (TimeSlot slot : getAvailableSlots(date, durationMinutes)) {
                if (!slot.getEndTime().isBefore(now) && !slot.getStartTime().isBefore(now))
                    return Optional.of(slot);
            }
            date = date.plusDays(1);
        }
        return Optional.empty();
    }

    public Schedule getMasterSchedule() {
        return masterSchedule;
    }
}
