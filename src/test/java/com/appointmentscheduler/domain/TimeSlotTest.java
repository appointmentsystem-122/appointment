package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TimeSlotTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 6, 1, 9, 0);

    @Test
    void constructor_valid() {
        TimeSlot s = new TimeSlot(T0, T0.plusHours(1));
        assertEquals(T0, s.getStartTime());
        assertEquals(T0.plusHours(1), s.getEndTime());
    }

    @Test
    void constructor_nullStart_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(null, T0.plusHours(1)));
    }

    @Test
    void constructor_startNotBeforeEnd_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(T0, T0));
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(T0.plusHours(1), T0));
    }

    @ParameterizedTest
    @CsvSource({
            "9,0, 10,0, 9,30, 10,30, true",
            "9,0, 10,0, 8,30, 9,30, true",
            "9,0, 10,0, 10,0, 11,0, false",
            "9,0, 10,0, 8,0, 9,0, false"
    })
    void overlapsWith_matrix(int sh, int sm, int eh, int em, int oh, int om, int oeh, int oem, boolean expect) {
        TimeSlot a = new TimeSlot(T0.withHour(sh).withMinute(sm), T0.withHour(eh).withMinute(em));
        TimeSlot b = new TimeSlot(T0.withHour(oh).withMinute(om), T0.withHour(oeh).withMinute(oem));
        assertEquals(expect, a.overlapsWith(b));
    }

    @Test
    void overlapsWith_null_returnsFalse() {
        assertFalse(new TimeSlot(T0, T0.plusHours(1)).overlapsWith(null));
    }

    @Test
    void equalsAndHashCode() {
        TimeSlot a = new TimeSlot(T0, T0.plusHours(1));
        TimeSlot b = new TimeSlot(T0, T0.plusHours(1));
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void toString_containsTimes() {
        TimeSlot s = new TimeSlot(T0, T0.plusHours(1));
        assertThat(s.toString()).contains("T09:00").contains("T10:00");
    }
}
