package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link BookingOption} branches: duration/participant clamps, display label modes, and equality.
 */
class BookingOptionCoverageTest {

    @Test
    void of_clampsDurationBelow15AndBuildsStableId() {
        AppointmentTypeConfig.Type shortDur = new AppointmentTypeConfig.Type("Trim", 10, 3);
        BookingOption o = BookingOption.of(shortDur, false);
        assertThat(o.getDurationMinutes()).isEqualTo(15);
        assertThat(o.getMaxParticipants()).isEqualTo(3);
        assertThat(o.getId()).contains("trim");
        assertThat(o.getId()).endsWith("|0|15|3");
    }

    @Test
    void getDisplayLabel_remoteVsInPerson() {
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Consult", 60, 4);
        BookingOption remote = BookingOption.of(t, true);
        BookingOption onsite = BookingOption.of(t, false);
        assertThat(remote.getDisplayLabel()).contains("Remote").contains("عن بُعد");
        assertThat(onsite.getDisplayLabel()).contains("In person").contains("حضوري");
    }

    @Test
    void equalsAndHashCode_sameId() {
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("X", 30, 2);
        BookingOption a = BookingOption.of(t, true);
        BookingOption b = BookingOption.of(t, true);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equals_falseForNullAndDifferentClass() {
        BookingOption o = BookingOption.of(new AppointmentTypeConfig.Type("Y", 45, 2), false);
        assertThat(o).isNotEqualTo(null);
        assertThat(o).isNotEqualTo("not-an-option");
    }

    @Test
    void equals_falseWhenOnlineModeDiffers() {
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Z", 60, 5);
        BookingOption on = BookingOption.of(t, true);
        BookingOption off = BookingOption.of(t, false);
        assertThat(on).isNotEqualTo(off);
    }

    @Test
    void toString_usesDisplayLabel() {
        BookingOption o = BookingOption.of(new AppointmentTypeConfig.Type("Svc", 60, 4), true);
        assertThat(o.toString()).isEqualTo(o.getDisplayLabel());
    }
}
