package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;

import java.time.LocalDateTime;

/** Virtual subtype whose simple name is long enough for daily-mode subtitle truncation in CalendarViewComponent. */
final class LongNameVirtualAppointmentForDailySubtitle extends VirtualAppointment {

    LongNameVirtualAppointmentForDailySubtitle(User u, LocalDateTime start) {
        super("lvn-daily-sub", u, new TimeSlot(start, start.plusHours(1)), "https://meet.example/x");
        setStatus("CONFIRMED");
    }
}
