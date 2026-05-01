package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ClosedDayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDateMessagesTest {

    @BeforeEach
    @AfterEach
    void resetContextAndPrefs() {
        ApplicationContext.setClosedDayService(null);
        Preferences p = Preferences.userNodeForPackage(ClosedDayService.class);
        p.remove("admin.closedDays");
        try {
            p.flush();
        } catch (BackingStoreException ignored) {
        }
    }

    @Test
    void nullDate_usesNoSlotsMessage() {
        assertThat(BookingDateMessages.unavailable(null)).isNotBlank();
    }

    @Test
    void closedDay_usesClosedMessage() {
        ClosedDayService cds = new ClosedDayService();
        LocalDate d = LocalDate.of(2026, 12, 25);
        cds.addClosedDay(d);
        ApplicationContext.setClosedDayService(cds);
        assertThat(BookingDateMessages.unavailable(d)).isNotBlank();
    }

    @Test
    void openDay_noSlotsMessage() {
        ApplicationContext.setClosedDayService(new ClosedDayService());
        assertThat(BookingDateMessages.unavailable(LocalDate.now().plusWeeks(2))).isNotBlank();
    }

    @Test
    void closedDayServiceUnset_usesNoSlotsBranch() {
        ApplicationContext.setClosedDayService(null);
        assertThat(BookingDateMessages.unavailable(LocalDate.now().plusWeeks(1))).isNotBlank();
    }
}
