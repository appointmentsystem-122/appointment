package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Appointment;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository interface for Appointment entities.
 */
public interface AppointmentRepository {

    /**
     * Saves or updates an appointment.
     * @param appointment to save
     */
    void save(Appointment appointment);

    /**
     * Finds an appointment by ID.
     * @param id the appointment ID
     * @return an Optional containing the appointment if found
     */
    Optional<Appointment> findById(String id);

    /**
     * Retrieves all appointments.
     * @return a list of all appointments
     */
    List<Appointment> findAll();

    /**
     * Deletes an appointment by ID.
     * @param id the ID of the appointment to delete
     */
    void deleteById(String id);

    /**
     * Active pipeline bookings for a patient (PENDING or CONFIRMED, not soft-deleted).
     * Used to enforce one open booking until staff marks complete or customer cancels.
     */
    default List<Appointment> findBlockingBookingsForPatient(String patientId) {
        if (patientId == null) return List.of();
        return findAll().stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getPatient() != null && patientId.equals(a.getPatient().getId()))
                .filter(a -> !a.isDeleted())
                .filter(a -> {
                    String s = a.getStatus();
                    return "PENDING".equals(s) || "CONFIRMED".equals(s);
                })
                .collect(Collectors.toList());
    }
}
