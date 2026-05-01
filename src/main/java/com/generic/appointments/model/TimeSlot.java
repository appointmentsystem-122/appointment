package com.generic.appointments.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A bookable time slot in a schedule.
 */
public final class TimeSlot {

    private final String id;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private boolean available;

    public TimeSlot(String id, LocalDateTime startTime, LocalDateTime endTime, boolean available) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}

