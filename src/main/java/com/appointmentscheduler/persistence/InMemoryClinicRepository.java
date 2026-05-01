package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Clinic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ClinicRepository.
 */
public class InMemoryClinicRepository implements ClinicRepository {

    private final Map<String, Clinic> store = new ConcurrentHashMap<>();

    @Override
    public void save(Clinic clinic) {
        if (clinic != null) store.put(clinic.getId(), clinic);
    }

    @Override
    public Optional<Clinic> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Clinic> findAll() {
        return new ArrayList<>(store.values());
    }
}
