package com.generic.appointments.repository;

import com.generic.appointments.model.TimeSlot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for time slots.
 */
public interface TimeSlotRepository {

    void save(TimeSlot timeSlot);

    Optional<TimeSlot> findById(String id);

    List<TimeSlot> findAll();

    List<TimeSlot> findAvailableBetween(LocalDateTime from, LocalDateTime to);
}

