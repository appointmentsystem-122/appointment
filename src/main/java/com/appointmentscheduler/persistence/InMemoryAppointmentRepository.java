package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of AppointmentRepository.
 */
public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> dataStore = new ConcurrentHashMap<>();

    @Override
    public void save(Appointment appointment) {
        if (appointment != null) {
            dataStore.put(appointment.getId(), appointment);
        }
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(dataStore.get(id));
    }

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(dataStore.values());
    }

    @Override
    public void deleteById(String id) {
        dataStore.remove(id);
    }
}
