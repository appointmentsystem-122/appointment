package com.generic.appointments.service;

import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.AppointmentStatus;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.observer.NotificationEvent;
import com.generic.appointments.repository.AppointmentRepository;
import com.generic.appointments.strategy.BookingRuleStrategy;

import java.util.List;

/**
 * Core appointment workflows: book, cancel, list.
 */
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BookingRuleStrategy bookingRule;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              BookingRuleStrategy bookingRule,
                              NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.bookingRule = bookingRule;
        this.notificationService = notificationService;
    }

    public Appointment bookAppointment(Customer customer, TimeSlot timeSlot) {
        Appointment appointment = new Appointment(null, customer, timeSlot, AppointmentStatus.PENDING);
        bookingRule.validate(appointment);

        timeSlot.setAvailable(false);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        notificationService.notifyAll(new NotificationEvent(appointment, AppointmentStatus.PENDING));
        return appointment;
    }

    public void cancelAppointment(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        AppointmentStatus previous = appointment.getStatus();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.getTimeSlot().setAvailable(true);
        appointmentRepository.save(appointment);

        notificationService.notifyAll(new NotificationEvent(appointment, previous));
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> findByCustomer(Customer customer) {
        return appointmentRepository.findByCustomer(customer);
    }
}

