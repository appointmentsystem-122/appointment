package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Doctor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of DoctorRepository.
 */
public class InMemoryDoctorRepository implements DoctorRepository {

    private final Map<String, Doctor> store = new ConcurrentHashMap<>();

    @Override
    public void save(Doctor doctor) {
        if (doctor != null) store.put(doctor.getId(), doctor);
    }

    @Override
    public Optional<Doctor> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Doctor> findAll() {
        return new ArrayList<>(store.values());
    }
}
