package com.generic.appointments.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of time slots for a company/location.
 */
public final class Schedule {

    private final List<TimeSlot> timeSlots = new ArrayList<>();

    public void addTimeSlot(TimeSlot slot) {
        timeSlots.add(slot);
    }

    public List<TimeSlot> getTimeSlots() {
        return Collections.unmodifiableList(timeSlots);
    }
}

