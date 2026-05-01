package com.appointmentscheduler.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the full matrix of bookable options from admin-defined types and configured delivery modes.
 */
public final class BookingCatalog {

    private BookingCatalog() {}

    /**
     * Every {@link AppointmentTypeConfig.Type} × each entry in {@link AppConfig#getBookingServiceTypes()}
     * (first entry = online reference label, others = on-site variants).
     */
    public static List<BookingOption> listOptions() {
        List<BookingOption> out = new ArrayList<>();
        List<AppointmentTypeConfig.Type> types = AppointmentTypeConfig.getAll();
        String[] modes = AppConfig.getBookingServiceTypes();
        if (modes == null || modes.length == 0) {
            for (AppointmentTypeConfig.Type t : types) {
                out.add(BookingOption.of(t, false));
            }
            return out;
        }
        String onlineRef = modes[0].trim();
        for (AppointmentTypeConfig.Type t : types) {
            for (String mode : modes) {
                boolean online = mode.trim().equalsIgnoreCase(onlineRef);
                out.add(BookingOption.of(t, online));
            }
        }
        return out;
    }
}
