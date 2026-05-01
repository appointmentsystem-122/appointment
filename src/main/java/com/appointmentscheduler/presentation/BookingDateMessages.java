package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ClosedDayService;

import java.time.LocalDate;

/**
 * User-facing reasons when a calendar day cannot be used for booking.
 */
public final class BookingDateMessages {

    private BookingDateMessages() {}

    public static String unavailable(LocalDate date) {
        ClosedDayService cds = ApplicationContext.getClosedDayService();
        if (date != null && cds != null && cds.isDayClosed(date)) {
            return I18n.get("booking.day_closed");
        }
        return I18n.get("booking.day_no_slots");
    }
}
