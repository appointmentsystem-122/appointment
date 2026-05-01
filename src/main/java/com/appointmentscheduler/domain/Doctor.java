package com.appointmentscheduler.domain;

import java.util.Objects;

/**
 * Represents a doctor (resource) for scheduling.
 */
public class Doctor {
    private final String id;
    private final String name;
    private final String email;
    private final String specialty;
    private final int maxAppointmentsPerDay;
    private final String clinicId;

    public Doctor(String id, String name, String email, String specialty, int maxAppointmentsPerDay) {
        this(id, name, email, specialty, maxAppointmentsPerDay, null);
    }

    public Doctor(String id, String name, String email, String specialty, int maxAppointmentsPerDay, String clinicId) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("ID cannot be null or empty");
        this.id = id;
        this.name = name != null ? name : "";
        this.email = email != null ? email : "";
        this.specialty = specialty != null ? specialty : "";
        this.maxAppointmentsPerDay = Math.max(1, maxAppointmentsPerDay);
        this.clinicId = clinicId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSpecialty() { return specialty; }
    public int getMaxAppointmentsPerDay() { return maxAppointmentsPerDay; }
    public String getClinicId() { return clinicId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doctor doctor = (Doctor) o;
        return Objects.equals(id, doctor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
