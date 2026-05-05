package com.appointmentscheduler.application;

/**
 * Stateless booking-form decision helper used by JavaFX controllers.
 * The validator converts raw UI state into a compact result object that can be asserted in unit tests
 * without constructing JavaFX scenes.
 */
public final class BookingFormValidator {

    private BookingFormValidator() {
    }

    /**
     * Evaluates the current booking form state and decides whether submission should be enabled.
     *
     * @param dateMissing whether no booking date has been selected
     * @param typeMissing whether no booking option has been selected
     * @param slotMissing whether no time slot has been selected
     * @param blockedByOpenAppointment whether the user already has an appointment that prevents booking
     * @return immutable validation result exposing the individual flags and final submit state
     */
    public static Result evaluate(boolean dateMissing, boolean typeMissing, boolean slotMissing, boolean blockedByOpenAppointment) {
        return new Result(
                dateMissing,
                typeMissing,
                slotMissing,
                blockedByOpenAppointment,
                !(dateMissing || typeMissing || slotMissing || blockedByOpenAppointment));
    }

    /**
     * Immutable booking-form validation snapshot consumed by controllers and tests.
     *
     * @param dateMissing whether the date requirement is not yet satisfied
     * @param typeMissing whether an appointment type is still missing
     * @param slotMissing whether a time slot has not been selected
     * @param blockedByOpenAppointment whether another appointment blocks submission
     * @param canSubmit overall decision indicating whether booking can proceed
     */
    public record Result(
            boolean dateMissing,
            boolean typeMissing,
            boolean slotMissing,
            boolean blockedByOpenAppointment,
            boolean canSubmit) {
    }
}
