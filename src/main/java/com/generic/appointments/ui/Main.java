package com.generic.appointments.ui;

import com.generic.appointments.model.Administrator;
import com.generic.appointments.model.Appointment;
import com.generic.appointments.model.Customer;
import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.impl.InMemoryAppointmentRepository;
import com.generic.appointments.repository.impl.InMemoryTimeSlotRepository;
import com.generic.appointments.repository.impl.InMemoryUserRepository;
import com.generic.appointments.service.*;
import com.generic.appointments.strategy.AvailabilityRule;
import com.generic.appointments.strategy.CompositeBookingRule;
import com.generic.appointments.strategy.DurationRule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Console entry point demonstrating the generic appointment system without UI dependencies.
 */
public class Main {

    public static void main(String[] args) {
        // Infrastructure
        var userRepo = new InMemoryUserRepository();
        var apptRepo = new InMemoryAppointmentRepository();
        var slotRepo = new InMemoryTimeSlotRepository();

        // Services & rules
        var notificationService = new NotificationService();
        notificationService.register(new ConsoleNotificationObserver());

        var bookingRule = new CompositeBookingRule()
            .addRule(new DurationRule(Duration.ofHours(2)))
            .addRule(new AvailabilityRule(apptRepo));

        var userService = new UserService(userRepo);
        var scheduleService = new ScheduleService(slotRepo);
        var appointmentService = new AppointmentService(apptRepo, bookingRule, notificationService);

        // Demo data
        Administrator admin = new Administrator(null, "Admin User", "admin@company.com",
                "admin", "admin123");
        Customer customer = new Customer(null, "Alice Client", "alice@client.com");
        userService.registerUser(admin);
        userService.registerUser(customer);

        TimeSlot slot = scheduleService.createTimeSlot(
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(2)
        );

        System.out.println("=== Generic Appointment Scheduling Demo ===");
        System.out.println("Available slot: " + slot.getStartTime() + " - " + slot.getEndTime());
        System.out.println("Customer " + customer.getName() + " booking this slot...");

        Appointment appointment = appointmentService.bookAppointment(customer, slot);

        System.out.println("Booked appointment id: " + appointment.getId());
        System.out.println("Status: " + appointment.getStatus());

        // Simple cancel flow
        Scanner scanner = new Scanner(System.in);
        System.out.print("Cancel appointment? (y/n): ");
        String input = scanner.nextLine();
        if ("y".equalsIgnoreCase(input)) {
            appointmentService.cancelAppointment(appointment.getId());
            System.out.println("Appointment cancelled.");
        }

        System.out.println("All appointments:");
        for (Appointment a : appointmentService.findAll()) {
            System.out.printf("  %s | %s | %s%n",
                a.getId(),
                a.getCustomer().getName(),
                a.getStatus());
        }
    }
}

