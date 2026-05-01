package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Clinic;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Clinic entities (multi-branch support).
 */
public interface ClinicRepository {
    void save(Clinic clinic);
    Optional<Clinic> findById(String id);
    List<Clinic> findAll();
}
