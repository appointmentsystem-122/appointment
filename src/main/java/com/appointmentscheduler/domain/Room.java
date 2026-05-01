package com.appointmentscheduler.domain;

import java.util.Objects;

/**
 * Represents a room (resource) for scheduling.
 */
public class Room {
    private final String id;
    private final String name;
    private final String clinicId;

    public Room(String id, String name) {
        this(id, name, null);
    }

    public Room(String id, String name, String clinicId) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID cannot be null or empty");
        this.id = id;
        this.name = name != null ? name : "";
        this.clinicId = clinicId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getClinicId() { return clinicId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
