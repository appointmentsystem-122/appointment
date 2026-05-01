package com.appointmentscheduler.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents the overall schedule, holding a collection of appointments.
 */
public class Schedule {
    private final List<Appointment> appointments;

    public Schedule() {
        this.appointments = new ArrayList<>();
    }

    /**
     * Clears all appointments from the schedule (used when reloading from repository).
     */
    public void clear() {
        this.appointments.clear();
    }

    /**
     * Adds an appointment to the schedule.
     * @param appointment to be added
     */
    public void addAppointment(Appointment appointment) {
        if (appointment == null) return;
        this.appointments.add(appointment);
    }

    /**
     * Retrieves all appointments.
     * @return a list of appointments
     */
    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(this.appointments);
    }

    /**
     * Returns appointments that overlap with a given timeslot (ignoring cancelled ones).
     * @param timeSlot the timeslot to check
     * @return list of overlapping appointments
     */
    public List<Appointment> getOverlappingAppointments(TimeSlot timeSlot) {
        // Snapshot: listeners may mutate `appointments` during iteration (e.g. UI reload).
        return new ArrayList<>(appointments).stream()
                .filter(Objects::nonNull)
                .filter(a -> !a.isDeleted())
                .filter(a -> !a.getStatus().equals("CANCELLED"))
                .filter(a -> a.getTimeSlot().overlapsWith(timeSlot))
                .collect(Collectors.toList());
    }
}
