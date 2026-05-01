package com.appointmentscheduler.application;

/**
 * Stable codes returned from {@link BookingService#tryBookWithReason} for UI localization.
 */
public final class BookingFailureCodes {

    /** Patient already has PENDING/CONFIRMED booking; must be completed or cancelled before another self-service book. */
    public static final String OPEN_APPOINTMENT_NOT_COMPLETED = "OPEN_APPOINTMENT_NOT_COMPLETED";

    private BookingFailureCodes() {}
}
