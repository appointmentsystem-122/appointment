package com.generic.appointments.repository.impl;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.AppointmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation for appointments.
 */
public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> store = new ConcurrentHashMap<>();

    @Override
    public void save(Appointment appointment) {
        store.put(appointment.getId(), appointment);
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Appointment> findByCustomer(Customer customer) {
        return store.values().stream()
            .filter(a -> a.getCustomer().getId().equals(customer.getId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findByTimeSlot(TimeSlot timeSlot) {
        return store.values().stream()
            .filter(a -> a.getTimeSlot().getId().equals(timeSlot.getId()))
            .collect(Collectors.toList());
    }
}

