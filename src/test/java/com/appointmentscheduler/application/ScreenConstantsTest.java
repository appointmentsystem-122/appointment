package com.appointmentscheduler.application;

import com.appointmentscheduler.presentation.ScreenConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ResourceLock("AppConfigProps")
class ScreenConstantsTest {

    @BeforeEach
    @AfterEach
    void resetAppConfig() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void titleHelpers_includeConfiguredAppName() {
        Properties p = new Properties();
        p.setProperty("app.name", "CoverageApp");
        AppConfig.applyPropertiesForTest(p);

        assertThat(ScreenConstants.titleLogin()).contains("CoverageApp");
        assertThat(ScreenConstants.titleAdminDashboard()).contains("CoverageApp");
        assertThat(ScreenConstants.titlePatientDashboard()).contains("CoverageApp");
        assertThat(ScreenConstants.titleBookAppointment()).contains("CoverageApp");
        assertThat(ScreenConstants.titleModifyAppointment()).contains("CoverageApp");
    }

    @Test
    void fxmlConstants_nonBlank() {
        assertThat(ScreenConstants.FXML_LOGIN).endsWith(".fxml");
        assertThat(ScreenConstants.BASE_PATH).contains("presentation");
    }
}
