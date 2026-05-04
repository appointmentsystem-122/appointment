package com.appointmentscheduler.application;

/**
 * Plain booking-form decision logic used by JavaFX controllers.
 */
public final class BookingFormValidator {

    private BookingFormValidator() {
    }

    public static Result evaluate(boolean dateMissing, boolean typeMissing, boolean slotMissing, boolean blockedByOpenAppointment) {
        return new Result(
                dateMissing,
                typeMissing,
                slotMissing,
                blockedByOpenAppointment,
                !(dateMissing || typeMissing || slotMissing || blockedByOpenAppointment));
    }

    public record Result(
            boolean dateMissing,
            boolean typeMissing,
            boolean slotMissing,
            boolean blockedByOpenAppointment,
            boolean canSubmit) {
    }
}
