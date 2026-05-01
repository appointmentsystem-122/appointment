package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrencePatternTest {

    @Test
    void constructor_rejectsInvalidArguments() {
        LocalDateTime s = LocalDateTime.of(2028, 1, 1, 9, 0);
        LocalDateTime e = s.plusWeeks(2);
        assertThatThrownBy(() -> new RecurrencePattern(null, s, e, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Frequency");
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, null, e, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("start");
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, null, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("end");
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, e, s, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, s, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("after");
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, s.minusDays(1), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.MONTHLY, s, e, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Interval");
        assertThatThrownBy(() -> new RecurrencePattern(RecurrencePattern.Frequency.MONTHLY, s, e, -3))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Interval");
    }

    @Test
    void generateOccurrenceStarts_weeklyAndMonthly() {
        LocalDateTime start = LocalDateTime.of(2027, 1, 4, 9, 0);
        LocalDateTime end = start.plusWeeks(3);
        RecurrencePattern weekly = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, start, end, 1);
        List<LocalDateTime> w = weekly.generateOccurrenceStarts();
        assertThat(w).isNotEmpty().contains(start);

        LocalDateTime mStart = LocalDateTime.of(2027, 2, 1, 10, 0);
        LocalDateTime mEnd = mStart.plusMonths(4);
        RecurrencePattern monthly = new RecurrencePattern(RecurrencePattern.Frequency.MONTHLY, mStart, mEnd, 1);
        assertThat(monthly.generateOccurrenceStarts()).isNotEmpty();
    }

    @Test
    void equalsAndHashCode_valueBased() {
        LocalDateTime s = LocalDateTime.now().plusDays(1);
        LocalDateTime e = s.plusMonths(2);
        RecurrencePattern a = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, e, 2);
        RecurrencePattern b = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, s, e, 2);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        RecurrencePattern c = new RecurrencePattern(RecurrencePattern.Frequency.MONTHLY, s, e, 2);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.equals(null)).isFalse();
        assertThat(a.equals("x")).isFalse();
    }
}
