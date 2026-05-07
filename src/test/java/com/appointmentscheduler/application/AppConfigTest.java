package com.appointmentscheduler.application;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock("AppConfigProps")
class AppConfigTest {
    @BeforeEach
    @AfterEach
    void syncAppConfigWithClasspath() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void getters_returnSensibleDefaultsOrLoadedValues() {
        assertThat(AppConfig.getAppName()).isNotBlank();
        assertThat(AppConfig.getAppVersion()).isNotBlank();
        assertThat(AppConfig.getBrandName()).isNotBlank();
        assertThat(AppConfig.getBrandTagline()).isNotBlank();
        assertThat(AppConfig.getDomainType()).isNotBlank();
        assertThat(AppConfig.getSystemType()).isNotBlank();
        assertThat(AppConfig.getSystemTypeOptions()).hasSizeGreaterThan(3).contains("Healthcare");
        assertThat(AppConfig.getSessionTimeoutMinutes()).isPositive();
        assertThat(AppConfig.getSessionWarningMinutes()).isPositive();
        assertThat(AppConfig.getBusinessHourStart()).isBetween(0, 23);
        assertThat(AppConfig.getBusinessHourEnd()).isBetween(0, 23);
        assertThat(AppConfig.getBookingMaxDurationMinutes()).isPositive();
        assertThat(AppConfig.getBookingCutoffHoursBefore()).isNotNegative();
        assertThat(AppConfig.getBookingServiceTypes()).isNotEmpty();
        assertThat(AppConfig.getOnlineLocationLabel()).isNotBlank();
        assertThat(AppConfig.getOnsiteLocationLabel()).isNotBlank();
        assertThat(AppConfig.getBookingAppointmentTypes()).isNotEmpty();
        assertThat(AppConfig.getBookingConsultationTypeIndex()).isNotNegative();
        assertThat(AppConfig.getBookingFollowUpTypeIndex()).isNotNegative();
        assertThat(AppConfig.getDatabaseUrl()).isNotBlank();
        assertThat(AppConfig.getDatabaseUsername()).isNotNull();
    }

    @Test
    void getInt_invalidFallsBack() {
        assertThat(AppConfig.getInt("__missing_int_key__", 42)).isEqualTo(42);
    }

    @Test
    void getBoolean_absentKey_returnsDefault() {
        assertThat(AppConfig.getBoolean("__missing_bool_key__", true)).isTrue();
        assertThat(AppConfig.getBoolean("__missing_bool_key__", false)).isFalse();
    }

    @Test
    void getBoolean_trueCaseInsensitive() {
        Properties p = new Properties();
        p.setProperty("app.showDevCredentials", "TrUe");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.getBoolean("app.showDevCredentials", false)).isTrue();
    }

    @Test
    void getInt_invalidNumberFormatFallsBack() {
        Properties p = new Properties();
        p.setProperty("session.timeoutMinutes", "not-a-number");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.getInt("session.timeoutMinutes", 99)).isEqualTo(99);
    }

    @Test
    void getBoolean_readsConfiguredProperty() {
        Properties p = new Properties();
        p.setProperty("app.showDevCredentials", "false");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.getBoolean("app.showDevCredentials", true)).isFalse();
    }

    @Test
    void getBoolean_trueBranch() {
        Properties p = new Properties();
        p.setProperty("app.showDevCredentials", "true");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.getBoolean("app.showDevCredentials", false)).isTrue();
    }

    @Test
    void get_whenKeyNotLoaded_returnsDefault() {
        Properties p = new Properties();
        p.setProperty("app.name", "Custom App Name");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.get("app.name", "fallback")).isEqualTo("Custom App Name");
    }

    @Test
    void setSystemType_blankIgnored() {
        String before = AppConfig.getSystemType();
        AppConfig.setSystemType("   ");
        assertThat(AppConfig.getSystemType()).isEqualTo(before);
        AppConfig.setSystemType("Clinic");
        assertThat(AppConfig.getSystemType()).isEqualTo("Healthcare");
        AppConfig.setSystemType(null);
        assertThat(AppConfig.getSystemType()).isEqualTo("Healthcare");
        AppConfig.setSystemType(before);
    }

    @Test
    void isDatabaseEnabled_and_databaseCredentials_roundTrip() {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.password", "secret");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.isDatabaseEnabled()).isTrue();
        assertThat(AppConfig.getDatabasePassword()).isEqualTo("secret");
    }

    @Test
    void getBoolean_nonTrueString_isFalse() {
        Properties p = new Properties();
        p.setProperty("app.showDevCredentials", "yes");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.getBoolean("app.showDevCredentials", true)).isFalse();
    }

    @Test
    void getInt_trimmedWhitespace() {
        Properties p = new Properties();
        p.setProperty("business.hourStart", "  8 ");
        AppConfig.applyPropertiesForTest(p);
        assertThat(AppConfig.getInt("business.hourStart", 0)).isEqualTo(8);
    }

    @Test
    void get_whenLoadedFalse_returnsDefaultBranch() throws Exception {
        Field loaded = AppConfig.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        boolean prev = loaded.getBoolean(null);
        try {
            loaded.setBoolean(null, false);
            assertThat(AppConfig.get("any.key", "fallback-value")).isEqualTo("fallback-value");
        } finally {
            loaded.setBoolean(null, prev);
        }
    }
}