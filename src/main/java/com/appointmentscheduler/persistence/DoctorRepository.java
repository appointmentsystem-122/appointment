package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Doctor;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Doctor entities.
 */
public interface DoctorRepository {
    void save(Doctor doctor);
    Optional<Doctor> findById(String id);
    List<Doctor> findAll();
}
