package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurringAppointmentTest {

    private static RecurrencePattern pattern() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        return new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, start, start.plusMonths(3), 1);
    }

    @Test
    void constructor_validatesSeriesPatternAndOccurrence() {
        User p = new User("1", "N", "e@e.com", "h");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1));
        RecurrencePattern rp = pattern();

        assertThatThrownBy(() -> new RecurringAppointment(p, slot, null, rp, "o1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecurringAppointment(p, slot, "sid", null, "o1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecurringAppointment(p, slot, "sid", rp, ""))
                .isInstanceOf(IllegalArgumentException.class);

        RecurringAppointment a = new RecurringAppointment("rid", p, slot, "sid", rp, "o1");
        assertThat(a.getSeriesId()).isEqualTo("sid");
        assertThat(a.getOccurrenceId()).isEqualTo("o1");
        assertThat(a.getRecurrencePattern()).isSameAs(rp);

        RecurringAppointment b = new RecurringAppointment("rid", p, slot, "sid", rp, "o1");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        RecurringAppointment otherId = new RecurringAppointment("other", p, slot, "sid", rp, "o1");
        assertThat(a).isNotEqualTo(otherId);
        assertThat(a.equals(new InPersonAppointment(p, slot, "L"))).isFalse();
        assertThat(a.equals(null)).isFalse();
    }
}
