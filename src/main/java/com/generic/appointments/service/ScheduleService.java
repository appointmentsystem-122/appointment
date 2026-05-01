package com.generic.appointments.service;

import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.TimeSlotRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Commands and queries related to company schedule.
 */
public class ScheduleService {

    private final TimeSlotRepository timeSlotRepository;

    public ScheduleService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public TimeSlot createTimeSlot(LocalDateTime start, LocalDateTime end) {
        TimeSlot slot = new TimeSlot(null, start, end, true);
        timeSlotRepository.save(slot);
        return slot;
    }

    public List<TimeSlot> findAvailableBetween(LocalDateTime from, LocalDateTime to) {
        return timeSlotRepository.findAvailableBetween(from, to);
    }
}

