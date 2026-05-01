package com.generic.appointments.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a business using the platform (clinic, salon, training center, etc.).
 */
public final class Company {

    private final String id;
    private final String companyName;
    private final Schedule schedule;
    private final List<Administrator> administrators = new ArrayList<>();

    public Company(String id, String companyName, Schedule schedule) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.companyName = Objects.requireNonNull(companyName, "companyName");
        this.schedule = Objects.requireNonNull(schedule, "schedule");
    }

    public String getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void addAdministrator(Administrator admin) {
        administrators.add(Objects.requireNonNull(admin, "admin"));
    }

    public List<Administrator> getAdministrators() {
        return Collections.unmodifiableList(administrators);
    }
}

