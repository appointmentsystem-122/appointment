package com.appointmentscheduler.domain;

import java.util.Objects;

/**
 * Represents a clinic/branch in a multi-clinic enterprise setup.
 */
public class Clinic {
    private final String id;
    private final String name;
    private final String address;
    private final String timeZone;

    public Clinic(String id, String name, String address, String timeZone) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Clinic ID cannot be null or empty");
        this.id = id;
        this.name = name != null ? name : "";
        this.address = address != null ? address : "";
        this.timeZone = timeZone != null ? timeZone : "UTC";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getTimeZone() { return timeZone; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clinic clinic = (Clinic) o;
        return Objects.equals(id, clinic.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
