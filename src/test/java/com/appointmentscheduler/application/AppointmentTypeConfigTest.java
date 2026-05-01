package com.appointmentscheduler.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTypeConfigTest {

    private Preferences prefs;

    @BeforeEach
    void clearStoredTypes() {
        prefs = Preferences.userNodeForPackage(AppointmentTypeConfig.class);
        prefs.remove("admin.appointmentTypes");
    }

    @AfterEach
    void tearDown() {
        prefs.remove("admin.appointmentTypes");
    }

    @Test
    void getAll_defaultsWhenEmpty() {
        List<AppointmentTypeConfig.Type> all = AppointmentTypeConfig.getAll();
        assertThat(all).isNotEmpty();
    }

    @Test
    void save_add_remove_roundTrip() {
        List<AppointmentTypeConfig.Type> seed = List.of(
                new AppointmentTypeConfig.Type("Alpha", 45, 2),
                new AppointmentTypeConfig.Type("Beta", 30, 1)
        );
        AppointmentTypeConfig.save(seed);
        assertThat(AppointmentTypeConfig.getAll()).extracting(AppointmentTypeConfig.Type::getName)
                .contains("Alpha", "Beta");
        AppointmentTypeConfig.remove("Alpha");
        assertThat(AppointmentTypeConfig.getAll()).noneMatch(t -> "Alpha".equalsIgnoreCase(t.getName()));
        AppointmentTypeConfig.add(new AppointmentTypeConfig.Type("Gamma", 20, 1));
        assertThat(AppointmentTypeConfig.getTypeNames()).contains("Gamma");
    }

    @Test
    void type_constructor_normalizesBadValues() {
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type(null, -5, -1);
        assertThat(t.getName()).isEqualTo("General");
        assertThat(t.getDurationMinutes()).isEqualTo(60);
        assertThat(t.getMaxParticipants()).isEqualTo(1);
    }

    @Test
    void getAll_ignoresMalformedAndNonNumericRows() {
        prefs.put("admin.appointmentTypes", "OnlyName:10|Bad:NaN:2|Valid:30:3");
        List<AppointmentTypeConfig.Type> all = AppointmentTypeConfig.getAll();
        assertThat(all).extracting(AppointmentTypeConfig.Type::getName).containsExactly("Valid");
    }

    @Test
    void save_nullOrEmpty_doesNotOverwriteExistingValues() {
        AppointmentTypeConfig.save(List.of(new AppointmentTypeConfig.Type("KeepMe", 25, 1)));
        AppointmentTypeConfig.save(List.of());
        AppointmentTypeConfig.save(null);
        assertThat(AppointmentTypeConfig.getTypeNames()).contains("KeepMe");
    }
}
