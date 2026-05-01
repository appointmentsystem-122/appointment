package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.AppointmentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTypeServiceTest {

    private Preferences prefs;

    @BeforeEach
    void clear() {
        prefs = Preferences.userNodeForPackage(AppointmentTypeService.class);
        prefs.remove("admin.appointmentTypes");
    }

    @AfterEach
    void tearDown() {
        prefs.remove("admin.appointmentTypes");
    }

    @Test
    void getAll_defaultsWhenMissing() {
        List<AppointmentType> all = AppointmentTypeService.getAll();
        assertThat(all).isNotEmpty();
    }

    @Test
    void saveAll_add_remove() {
        List<AppointmentType> list = List.of(
                new AppointmentType("CustomA", 25, 2),
                new AppointmentType("CustomB", 35, 1)
        );
        AppointmentTypeService.saveAll(list);
        assertThat(AppointmentTypeService.getAll()).extracting(AppointmentType::getName).contains("CustomA", "CustomB");
        AppointmentTypeService.remove("CustomA");
        assertThat(AppointmentTypeService.getAll()).noneMatch(t -> "CustomA".equalsIgnoreCase(t.getName()));
        AppointmentTypeService.add(new AppointmentType("CustomC", 40, 1));
        assertThat(AppointmentTypeService.getAll()).anyMatch(t -> "CustomC".equalsIgnoreCase(t.getName()));
    }

    @Test
    void getAll_fallsBackToDefaultsWhenStoredValueBlank() {
        prefs.put("admin.appointmentTypes", "   ");
        List<AppointmentType> all = AppointmentTypeService.getAll();
        assertThat(all).isNotEmpty();
        assertThat(all).anyMatch(t -> "Standard".equalsIgnoreCase(t.getName()));
    }

    @Test
    void getAll_ignoresMalformedRecordsAndKeepsValidOnes() {
        prefs.put("admin.appointmentTypes", "Bad|not-number|x,Good|20|2,NoSep,AlsoBad|30");
        List<AppointmentType> all = AppointmentTypeService.getAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getName()).isEqualTo("Good");
        assertThat(all.get(0).getDurationMinutes()).isEqualTo(20);
        assertThat(all.get(0).getMaxParticipants()).isEqualTo(2);
    }

    @Test
    void getAll_whenNothingParsable_fallsBackToBuiltInDefaults() {
        prefs.put("admin.appointmentTypes", "Bad|x|y,,|30|,NoSep");
        List<AppointmentType> all = AppointmentTypeService.getAll();
        assertThat(all).hasSize(6);
        assertThat(all).extracting(AppointmentType::getName)
                .contains("Standard", "New session", "Return visit", "Express", "Extended session", "Preparation");
    }

    @Test
    void saveAll_ignoresNullOrEmptyInput() {
        AppointmentTypeService.saveAll(null);
        AppointmentTypeService.saveAll(List.of());
        assertThat(AppointmentTypeService.getAll()).isNotEmpty();
    }

    @Test
    void add_replacesExistingByNameIgnoringCase() {
        AppointmentTypeService.saveAll(List.of(
                new AppointmentType("Consult", 20, 1),
                new AppointmentType("Other", 10, 1)
        ));
        AppointmentTypeService.add(new AppointmentType("consult", 45, 3));
        List<AppointmentType> all = AppointmentTypeService.getAll();
        assertThat(all.stream().filter(t -> "consult".equalsIgnoreCase(t.getName())).count()).isEqualTo(1);
        assertThat(all).anyMatch(t -> "consult".equalsIgnoreCase(t.getName())
                && t.getDurationMinutes() == 45
                && t.getMaxParticipants() == 3);
    }
}
