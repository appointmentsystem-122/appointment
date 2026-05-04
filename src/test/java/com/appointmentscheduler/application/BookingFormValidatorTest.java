package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingFormValidatorTest {

    @Test
    void evaluate_allInputsPresent_allowsSubmit() {
        BookingFormValidator.Result result = BookingFormValidator.evaluate(false, false, false, false);

        assertThat(result.canSubmit()).isTrue();
        assertThat(result.dateMissing()).isFalse();
        assertThat(result.typeMissing()).isFalse();
        assertThat(result.slotMissing()).isFalse();
        assertThat(result.blockedByOpenAppointment()).isFalse();
    }

    @Test
    void evaluate_missingRequiredFields_blocksSubmitAndReportsFields() {
        BookingFormValidator.Result result = BookingFormValidator.evaluate(true, true, true, false);

        assertThat(result.canSubmit()).isFalse();
        assertThat(result.dateMissing()).isTrue();
        assertThat(result.typeMissing()).isTrue();
        assertThat(result.slotMissing()).isTrue();
        assertThat(result.blockedByOpenAppointment()).isFalse();
    }

    @Test
    void evaluate_blockingOpenAppointment_blocksSubmit() {
        BookingFormValidator.Result result = BookingFormValidator.evaluate(false, false, false, true);

        assertThat(result.canSubmit()).isFalse();
        assertThat(result.blockedByOpenAppointment()).isTrue();
    }
}
