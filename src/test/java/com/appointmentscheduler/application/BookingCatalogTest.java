package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@ResourceLock("AppConfigProps")
class BookingCatalogTest {

    @Test
    void listOptions_notEmpty() {
        assertThat(BookingCatalog.listOptions()).isNotEmpty();
    }

    @Test
    void listOptions_whenServiceTypesEmpty_returnsOneModePerType() {
        Preferences.userNodeForPackage(AppointmentTypeConfig.class).remove("admin.appointmentTypes");
        Properties p = new Properties();
        p.setProperty("booking.appointmentTypes", "TypeA,TypeB");
        p.setProperty("booking.serviceTypes", "");
        AppConfig.applyPropertiesForTest(p);

        List<BookingOption> out = BookingCatalog.listOptions();
        assertThat(out).isNotEmpty();
        // Empty property value becomes one empty token; branch still returns one mode per type.
        assertThat(out).allMatch(BookingOption::isOnline);
    }

    @Test
    void listOptions_withTwoModes_producesOnlineAndOnsite() {
        Preferences.userNodeForPackage(AppointmentTypeConfig.class).remove("admin.appointmentTypes");
        Properties p = new Properties();
        p.setProperty("booking.appointmentTypes", "TypeA");
        p.setProperty("booking.serviceTypes", "Remote,In person");
        AppConfig.applyPropertiesForTest(p);

        List<BookingOption> out = BookingCatalog.listOptions();
        assertThat(out).hasSize(2);
        assertThat(out).anyMatch(BookingOption::isOnline);
        assertThat(out).anyMatch(o -> !o.isOnline());
    }

    @Test
    void listOptions_whenModesNull_usesFallbackBranch() {
        try (MockedStatic<AppConfig> cfg = mockStatic(AppConfig.class);
             MockedStatic<AppointmentTypeConfig> types = mockStatic(AppointmentTypeConfig.class)) {
            types.when(AppointmentTypeConfig::getAll).thenReturn(List.of(
                    new AppointmentTypeConfig.Type("TypeA", 45, 2),
                    new AppointmentTypeConfig.Type("TypeB", 60, 1)
            ));
            cfg.when(AppConfig::getBookingServiceTypes).thenReturn(null);

            List<BookingOption> out = BookingCatalog.listOptions();
            assertThat(out).hasSize(2);
            assertThat(out).allMatch(o -> !o.isOnline());
        }
    }
}
