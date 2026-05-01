package com.appointmentscheduler.application;

import com.appointmentscheduler.persistence.ClinicRepository;
import com.appointmentscheduler.persistence.DoctorRepository;
import com.appointmentscheduler.persistence.RoomRepository;
import com.appointmentscheduler.presentation.AppNotificationStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Static service locator: populated at application startup; {@link #clearContext()} must run before
 * and after each test so order and other suites cannot leak global state.
 */
@ResourceLock("ApplicationContext")
@DisplayName("ApplicationContext (service locator)")
class ApplicationContextTest {

    @BeforeEach
    void beforeEach() {
        clearContext();
    }

    @AfterEach
    void afterEach() {
        clearContext();
    }

    private static void clearContext() {
        ApplicationContext.setAuthService(null);
        ApplicationContext.setBookingService(null);
        ApplicationContext.setScheduleService(null);
        ApplicationContext.setNotificationService(null);
        ApplicationContext.setAppointmentReminderPort(null);
        ApplicationContext.setInAppMessagingService(null);
        ApplicationContext.setAuditLogService(null);
        ApplicationContext.setPermissionService(null);
        ApplicationContext.setReportingService(null);
        ApplicationContext.setSlotRecommendationService(null);
        ApplicationContext.setLoginAttemptService(null);
        ApplicationContext.setGlobalSearchService(null);
        ApplicationContext.setPdfReportService(null);
        ApplicationContext.setAppNotificationStore(null);
        ApplicationContext.setDoctorRepository(null);
        ApplicationContext.setRoomRepository(null);
        ApplicationContext.setClinicRepository(null);
        ApplicationContext.setCurrentClinicService(null);
        ApplicationContext.setBackupRestoreService(null);
        ApplicationContext.setClosedDayService(null);
        ApplicationContext.setUsingDatabase(false);
    }

    @Test
    @DisplayName("Setters wire all enterprise services; isUsingDatabase can be toggled")
    void setters_populateAllSlots() {
        ApplicationContext.setAuthService(mock(AuthService.class));
        ApplicationContext.setBookingService(mock(BookingService.class));
        ApplicationContext.setScheduleService(mock(ScheduleService.class));
        ApplicationContext.setNotificationService(mock(NotificationService.class));
        ApplicationContext.setAppointmentReminderPort(mock(AppointmentReminderPort.class));
        ApplicationContext.setInAppMessagingService(mock(InAppMessagingService.class));
        ApplicationContext.setAuditLogService(mock(AuditLogService.class));
        ApplicationContext.setPermissionService(mock(PermissionService.class));
        ApplicationContext.setReportingService(mock(ReportingService.class));
        ApplicationContext.setSlotRecommendationService(mock(SlotRecommendationService.class));
        ApplicationContext.setLoginAttemptService(mock(LoginAttemptService.class));
        ApplicationContext.setGlobalSearchService(mock(GlobalSearchService.class));
        ApplicationContext.setPdfReportService(mock(PdfReportService.class));
        ApplicationContext.setAppNotificationStore(mock(AppNotificationStore.class));
        ApplicationContext.setDoctorRepository(mock(DoctorRepository.class));
        ApplicationContext.setRoomRepository(mock(RoomRepository.class));
        ApplicationContext.setClinicRepository(mock(ClinicRepository.class));
        ApplicationContext.setCurrentClinicService(mock(CurrentClinicService.class));
        ApplicationContext.setBackupRestoreService(mock(BackupRestoreService.class));
        ApplicationContext.setClosedDayService(new ClosedDayService());
        ApplicationContext.setUsingDatabase(true);

        assertThat(ApplicationContext.isUsingDatabase()).isTrue();
        assertThat(ApplicationContext.getAuthService()).isNotNull();
        assertThat(ApplicationContext.getBookingService()).isNotNull();
        assertThat(ApplicationContext.getScheduleService()).isNotNull();
        assertThat(ApplicationContext.getNotificationService()).isNotNull();
        assertThat(ApplicationContext.getAppointmentReminderPort()).isNotNull();
        assertThat(ApplicationContext.getInAppMessagingService()).isNotNull();
        assertThat(ApplicationContext.getAuditLogService()).isNotNull();
        assertThat(ApplicationContext.getPermissionService()).isNotNull();
        assertThat(ApplicationContext.getReportingService()).isNotNull();
        assertThat(ApplicationContext.getSlotRecommendationService()).isNotNull();
        assertThat(ApplicationContext.getLoginAttemptService()).isNotNull();
        assertThat(ApplicationContext.getGlobalSearchService()).isNotNull();
        assertThat(ApplicationContext.getPdfReportService()).isNotNull();
        assertThat(ApplicationContext.getAppNotificationStore()).isNotNull();
        assertThat(ApplicationContext.getDoctorRepository()).isNotNull();
        assertThat(ApplicationContext.getRoomRepository()).isNotNull();
        assertThat(ApplicationContext.getClinicRepository()).isNotNull();
        assertThat(ApplicationContext.getCurrentClinicService()).isNotNull();
        assertThat(ApplicationContext.getBackupRestoreService()).isNotNull();
        assertThat(ApplicationContext.getClosedDayService()).isNotNull();
    }
}
