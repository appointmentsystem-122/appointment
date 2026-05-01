package com.appointmentscheduler.domain;

import java.util.Objects;

/**
 * Configurable appointment type with duration and max participants.
 */
public class AppointmentType {
    private final String id;
    private final String name;
    private final int durationMinutes;
    private final int maxParticipants;

    public AppointmentType(String id, String name, int durationMinutes, int maxParticipants) {
        this.id = id != null && !id.isEmpty() ? id : (name != null ? name.replaceAll("\\s+", "_") : "type");
        this.name = name != null ? name : "";
        this.durationMinutes = Math.max(15, Math.min(480, durationMinutes));
        this.maxParticipants = Math.max(1, Math.min(100, maxParticipants));
    }

    public AppointmentType(String name, int durationMinutes, int maxParticipants) {
        this(null, name, durationMinutes, maxParticipants);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getMaxParticipants() { return maxParticipants; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppointmentType that = (AppointmentType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
