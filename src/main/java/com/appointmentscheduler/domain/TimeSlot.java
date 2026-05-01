package com.appointmentscheduler.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A Value Object representing a specific duration of time.
 * Characterized by a start time and an end time.
 */
public class TimeSlot {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    /**
     * Constructs a TimeSlot.
     *
     * @param startTime the start time
     * @param endTime   the end time
     * @throws IllegalArgumentException if startTime is after or equal to endTime
     */
    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start and End times must not be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be strictly before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Retrieves the start time.
     * @return the start time.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Retrieves the end time.
     * @return the end time.
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Checks if this timeslot overlaps with another timeslot.
     *
     * @param other the other timeslot
     * @return true if there is an overlap, false otherwise
     */
    public boolean overlapsWith(TimeSlot other) {
        if (other == null) return false;
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(startTime, timeSlot.startTime) && Objects.equals(endTime, timeSlot.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }

    @Override
    public String toString() {
        return startTime.toString() + " to " + endTime.toString();
    }
}
