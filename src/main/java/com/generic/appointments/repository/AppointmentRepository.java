package com.generic.appointments.repository;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;

import java.util.List;
import java.util.Optional;

/**
 * Repository for appointments.
 */
public interface AppointmentRepository {

    void save(Appointment appointment);

    Optional<Appointment> findById(String id);

    List<Appointment> findAll();

    List<Appointment> findByCustomer(Customer customer);

    List<Appointment> findByTimeSlot(TimeSlot timeSlot);
}

