package com.appointmentscheduler.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class ClosedDayServiceTest {

    private Preferences prefs;

    @BeforeEach
    void clear() {
        prefs = Preferences.userNodeForPackage(ClosedDayService.class);
        prefs.remove("admin.closedDays");
    }

    @AfterEach
    void tearDown() {
        prefs.remove("admin.closedDays");
    }

    @Test
    void addRemoveAndQuery() {
        ClosedDayService svc = new ClosedDayService();
        LocalDate d = LocalDate.of(2026, 7, 4);
        assertThat(svc.isDayClosed(d)).isFalse();
        svc.addClosedDay(d);
        assertThat(svc.isDayClosed(d)).isTrue();
        assertThat(svc.getClosedDays()).contains(d);
        svc.removeClosedDay(d);
        assertThat(svc.isDayClosed(d)).isFalse();
    }

    @Test
    void invalidDateTokensIgnored() {
        ClosedDayService svc = new ClosedDayService();
        prefs.put("admin.closedDays", "not-a-date,2026-08-01");
        assertThat(svc.getClosedDays()).hasSize(1);
    }

    @Test
    void blankAndWhitespaceTokensAreIgnored() {
        ClosedDayService svc = new ClosedDayService();
        prefs.put("admin.closedDays", " , ,2026-08-05,, ");
        assertThat(svc.getClosedDays()).containsExactly(LocalDate.of(2026, 8, 5));
    }
}
