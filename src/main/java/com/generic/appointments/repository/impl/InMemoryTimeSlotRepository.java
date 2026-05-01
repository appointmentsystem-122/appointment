package com.generic.appointments.repository.impl;

import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.TimeSlotRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation for time slots.
 */
public class InMemoryTimeSlotRepository implements TimeSlotRepository {

    private final Map<String, TimeSlot> store = new ConcurrentHashMap<>();

    @Override
    public void save(TimeSlot timeSlot) {
        store.put(timeSlot.getId(), timeSlot);
    }

    @Override
    public Optional<TimeSlot> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<TimeSlot> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<TimeSlot> findAvailableBetween(LocalDateTime from, LocalDateTime to) {
        return store.values().stream()
            .filter(TimeSlot::isAvailable)
            .filter(slot -> !slot.getStartTime().isBefore(from)
                         && !slot.getEndTime().isAfter(to))
            .collect(Collectors.toList());
    }
}

