package com.appointmentscheduler.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing a recurrence pattern for appointments.
 * Supports weekly and monthly recurrence.
 */
public final class RecurrencePattern {

    public enum Frequency {
        WEEKLY,
        MONTHLY
    }

    private final Frequency frequency;
    private final LocalDateTime seriesStart;
    private final LocalDateTime seriesEnd;
    private final int interval; // e.g. every 2 weeks

    public RecurrencePattern(Frequency frequency, LocalDateTime seriesStart, LocalDateTime seriesEnd, int interval) {
        if (frequency == null) throw new IllegalArgumentException("Frequency cannot be null");
        if (seriesStart == null) throw new IllegalArgumentException("Series start cannot be null");
        if (seriesEnd == null) throw new IllegalArgumentException("Series end cannot be null");
        if (!seriesEnd.isAfter(seriesStart)) throw new IllegalArgumentException("Series end must be after start");
        if (interval < 1) throw new IllegalArgumentException("Interval must be positive");
        this.frequency = frequency;
        this.seriesStart = seriesStart;
        this.seriesEnd = seriesEnd;
        this.interval = interval;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public LocalDateTime getSeriesStart() {
        return seriesStart;
    }

    public LocalDateTime getSeriesEnd() {
        return seriesEnd;
    }

    public int getInterval() {
        return interval;
    }

    /**
     * Generates occurrence start times within the series range.
     * Reusable and testable logic.
     */
    public List<LocalDateTime> generateOccurrenceStarts() {
        List<LocalDateTime> starts = new ArrayList<>();
        LocalDateTime current = seriesStart;
        while (!current.isAfter(seriesEnd)) {
            starts.add(current);
            current = nextOccurrence(current);
        }
        return starts;
    }

    private LocalDateTime nextOccurrence(LocalDateTime from) {
        switch (frequency) {
            case WEEKLY:
                return from.plusWeeks(interval);
            case MONTHLY:
                return from.plusMonths(interval);
            default:
                throw new IllegalStateException("Unknown frequency: " + frequency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurrencePattern that = (RecurrencePattern) o;
        return interval == that.interval
                && frequency == that.frequency
                && Objects.equals(seriesStart, that.seriesStart)
                && Objects.equals(seriesEnd, that.seriesEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, seriesStart, seriesEnd, interval);
    }
}
