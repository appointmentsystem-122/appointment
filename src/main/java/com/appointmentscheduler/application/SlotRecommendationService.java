package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.Schedule;
import com.appointmentscheduler.domain.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Recommends optimal available slots based on earliest availability and schedule congestion.
 */
public class SlotRecommendationService {

    private final ScheduleService scheduleService;

    public SlotRecommendationService(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Returns recommended slots for a date, ordered by preference:
     * 1. Earliest available
     * 2. Lowest congestion (fewest appointments in surrounding hours)
     */
    public List<TimeSlot> getRecommendedSlots(LocalDate date, int maxSuggestions) {
        List<TimeSlot> available = scheduleService.getAvailableSlots(date);
        if (available.isEmpty()) return new ArrayList<>();
        if (available.size() <= maxSuggestions) return available;

        Schedule schedule = scheduleService.getMasterSchedule();
        Map<TimeSlot, Long> congestionBySlot = available.stream()
                .collect(Collectors.toMap(s -> s, s -> countNearbyAppointments(s, schedule)));

        return available.stream()
                .sorted(Comparator.comparing((TimeSlot s) -> s.getStartTime())
                        .thenComparingLong(congestionBySlot::get))
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }

    private long countNearbyAppointments(TimeSlot slot, Schedule schedule) {
        LocalDateTime windowStart = slot.getStartTime().minusHours(2);
        LocalDateTime windowEnd = slot.getEndTime().plusHours(2);
        TimeSlot window = new TimeSlot(windowStart, windowEnd);
        return schedule.getOverlappingAppointments(window).stream()
                .filter(a -> !a.getStatus().equals("CANCELLED"))
                .count();
    }
}
