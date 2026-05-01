package com.appointmentscheduler.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ResourceLock("AppConfigProps")
class AppConfigBranchBoostTest {

    @AfterEach
    void reset() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void getters_coverFallback_and_parsing_paths() {
        Properties p = new Properties();
        p.setProperty("bad.int", "x");
        p.setProperty("ok.int", "7");
        p.setProperty("ok.bool", "TrUe");
        p.setProperty("booking.serviceTypes", "Remote,In person");
        p.setProperty("booking.appointmentTypes", "A,B");
        AppConfig.applyPropertiesForTest(p);

        assertThat(AppConfig.getInt("bad.int", 9)).isEqualTo(9);
        assertThat(AppConfig.getInt("ok.int", 0)).isEqualTo(7);
        assertThat(AppConfig.getBoolean("ok.bool", false)).isTrue();
        assertThat(AppConfig.getBoolean("missing.bool", true)).isTrue();
        assertThat(AppConfig.get("missing.key", "def")).isEqualTo("def");
        assertThat(AppConfig.getBookingServiceTypes()).hasSize(2);
        assertThat(AppConfig.getBookingAppointmentTypes()).hasSize(2);
    }

    @Test
    void systemType_set_and_clinicMigrationBranch() {
        AppConfig.setSystemType("Clinic");
        assertThat(AppConfig.getSystemType()).isEqualTo("Healthcare");
        AppConfig.setSystemType("Other");
        assertThat(AppConfig.getSystemType()).isEqualTo("Other");
        AppConfig.setSystemType(" ");
        assertThat(AppConfig.getSystemType()).isEqualTo("Other");
        assertThat(AppConfig.getSystemTypeOptions()).contains("General", "Healthcare", "Other");
    }
}

