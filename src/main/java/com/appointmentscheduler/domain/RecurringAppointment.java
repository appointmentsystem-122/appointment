package com.appointmentscheduler.domain;

import java.util.Objects;

/**
 * Appointment that repeats on a schedule.
 * Supports cancellation of single occurrence or entire series.
 */
public class RecurringAppointment extends Appointment {

    private final String seriesId;
    private final RecurrencePattern recurrencePattern;
    private final String occurrenceId; // unique for each occurrence

    public RecurringAppointment(User patient, TimeSlot timeSlot, String seriesId,
                                RecurrencePattern recurrencePattern, String occurrenceId) {
        super(patient, timeSlot);
        if (seriesId == null || seriesId.isEmpty()) throw new IllegalArgumentException("Series ID required");
        if (recurrencePattern == null) throw new IllegalArgumentException("Recurrence pattern required");
        if (occurrenceId == null || occurrenceId.isEmpty()) throw new IllegalArgumentException("Occurrence ID required");
        this.seriesId = seriesId;
        this.recurrencePattern = recurrencePattern;
        this.occurrenceId = occurrenceId;
    }

    public RecurringAppointment(String id, User patient, TimeSlot timeSlot, String seriesId,
                               RecurrencePattern recurrencePattern, String occurrenceId) {
        super(id, patient, timeSlot);
        if (seriesId == null || seriesId.isEmpty()) throw new IllegalArgumentException("Series ID required");
        if (recurrencePattern == null) throw new IllegalArgumentException("Recurrence pattern required");
        if (occurrenceId == null || occurrenceId.isEmpty()) throw new IllegalArgumentException("Occurrence ID required");
        this.seriesId = seriesId;
        this.recurrencePattern = recurrencePattern;
        this.occurrenceId = occurrenceId;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public RecurrencePattern getRecurrencePattern() {
        return recurrencePattern;
    }

    public String getOccurrenceId() {
        return occurrenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RecurringAppointment that = (RecurringAppointment) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
