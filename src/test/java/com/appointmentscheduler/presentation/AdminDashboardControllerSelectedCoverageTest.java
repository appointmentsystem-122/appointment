package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.application.ReportingService;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Extra targeted tests for branches in the Sonar-selected AdminDashboardController file.
 */
@ResourceLock("ApplicationContextServices")
class AdminDashboardControllerSelectedCoverageTest {

    private AuditLogService originalAuditLogService;
    private ReportingService originalReportingService;
    private com.appointmentscheduler.application.ScheduleService originalScheduleService;
    private com.appointmentscheduler.application.AuthService originalAuthService;

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @BeforeEach
    void rememberServices() {
        originalAuditLogService = ApplicationContext.getAuditLogService();
        originalReportingService = ApplicationContext.getReportingService();
        originalScheduleService = ApplicationContext.getScheduleService();
        originalAuthService = ApplicationContext.getAuthService();
    }

    @AfterEach
    void restoreServices() {
        ApplicationContext.setAuditLogService(originalAuditLogService);
        ApplicationContext.setReportingService(originalReportingService);
        ApplicationContext.setScheduleService(originalScheduleService);
        ApplicationContext.setAuthService(originalAuthService);
    }

    @Test
    void summarizeBookingRequests_coversNullEmptyFullAndTruncatedBranches() throws Exception {
        assertThat(invokeStaticString("summarizeBookingRequests", new Class<?>[]{Appointment.class}, new Object[]{null}))
                .isEmpty();

        Appointment empty = appointment();
        assertThat(invokeStaticString("summarizeBookingRequests", new Class<?>[]{Appointment.class}, new Object[]{empty}))
                .isEmpty();

        Appointment full = appointment();
        full.setCustomerNotes("  Bring reports  ");
        full.setContactPhone("  0599000000 ");
        full.setReminderChannel(BookingRequestFields.REMINDER_SMS);
        full.setPreferredLanguage(BookingRequestFields.LANG_AR);
        full.setAccessibilityNeeds("  Wheelchair access  ");
        full.setParticipantCount(3);

        String summary = invokeStaticString("summarizeBookingRequests", new Class<?>[]{Appointment.class}, new Object[]{full});
        assertThat(summary)
                .contains("Bring reports")
                .contains("☎ 0599000000")
                .contains("SMS")
                .contains("Arabic")
                .contains("Wheelchair access")
                .contains("Party: 3");

        Appointment longRequest = appointment();
        longRequest.setCustomerNotes("x".repeat(200));
        String truncated = invokeStaticString("summarizeBookingRequests", new Class<?>[]{Appointment.class}, new Object[]{longRequest});
        assertThat(truncated).hasSize(158).endsWith("…");
    }

    @Test
    void formatTrend_coversZeroFlatIncreaseAndDecreaseBranches() throws Exception {
        assertThat(invokeStaticString("formatTrend", new Class<?>[]{long.class, long.class, String.class}, new Object[]{0L, 0L, "yesterday"}))
                .isEmpty();
        assertThat(invokeStaticString("formatTrend", new Class<?>[]{long.class, long.class, String.class}, new Object[]{0L, 5L, "yesterday"}))
                .isEqualTo("↑ vs yesterday");
        assertThat(invokeStaticString("formatTrend", new Class<?>[]{long.class, long.class, String.class}, new Object[]{100L, 100L, "last week"}))
                .isEqualTo("→ vs last week");
        assertThat(invokeStaticString("formatTrend", new Class<?>[]{long.class, long.class, String.class}, new Object[]{100L, 125L, "last week"}))
                .isEqualTo("↑ 25% vs last week");
        assertThat(invokeStaticString("formatTrend", new Class<?>[]{long.class, long.class, String.class}, new Object[]{100L, 75L, "last week"}))
                .isEqualTo("↓ 25% vs last week");
    }

    @Test
    void applyCancellationRateKpi_coversInvalidClampAndAllTierBranches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label value = new Label();
        Label status = new Label();
        ProgressBar bar = new ProgressBar();
        setField(c, "cancellationRateLabel", value);
        setField(c, "cancellationRateStatusLabel", status);
        setField(c, "cancellationRateProgressBar", bar);

        invokePrivate(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, Double.NaN);
        assertThat(value.getText()).isEqualTo("0.0%");
        assertThat(status.getText()).contains("Excellent");

        invokePrivate(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, 7.0d);
        assertThat(status.getText()).contains("Good");

        invokePrivate(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, 12.0d);
        assertThat(status.getText()).contains("Moderate");

        invokePrivate(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, 17.0d);
        assertThat(status.getText()).contains("Needs attention");

        invokePrivate(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, Double.POSITIVE_INFINITY);
        assertThat(value.getText()).isEqualTo("0.0%");

        invokePrivate(c, "applyCancellationRateKpi", new Class<?>[]{double.class}, 125.0d);
        assertThat(value.getText()).isEqualTo("100.0%");
        assertThat(status.getText()).contains("High risk");
        assertThat(bar.getProgress()).isEqualTo(1.0d);
    }

    @Test
    void updateAlertsPanel_coversHiddenWarningAndCriticalBranches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        VBox panel = new VBox();
        HBox content = new HBox();
        setField(c, "adminAlertsPanel", panel);
        setField(c, "adminAlertsContent", content);

        invokePrivate(c, "updateAlertsPanel", new Class<?>[]{String.class, long.class, long.class, double.class}, "clinic", 3L, 5L, 2.0d);
        assertThat(panel.isVisible()).isFalse();
        assertThat(panel.isManaged()).isFalse();

        invokePrivate(c, "updateAlertsPanel", new Class<?>[]{String.class, long.class, long.class, double.class}, "clinic", 1L, 8L, 17.5d);
        assertThat(panel.isVisible()).isTrue();
        assertThat(content.getChildren()).hasSize(1);

        invokePrivate(c, "updateAlertsPanel", new Class<?>[]{String.class, long.class, long.class, double.class}, "clinic", 0L, 8L, 25.0d);
        assertThat(content.getChildren()).hasSize(2);
    }

    @Test
    void buttonStyleHelpers_attachNormalAndHoverHandlers() throws Exception {
        Button close = new Button();
        Button open = new Button();
        invokeStaticVoid("applyEnterpriseCloseButton", new Class<?>[]{Button.class}, new Object[]{close});
        invokeStaticVoid("applyEnterpriseOpenButton", new Class<?>[]{Button.class}, new Object[]{open});

        assertThat(close.getStyle()).contains("#dc2626");
        assertThat(open.getStyle()).contains("#047857");
        assertThat(close.getOnMouseEntered()).isNotNull();
        assertThat(open.getOnMouseExited()).isNotNull();
    }

    @Test
    void updateSessionAndAppointmentCountLabels_coverNonNullBranches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label session = new Label();
        Label count = new Label();
        TableView<Appointment> table = new TableView<>();
        table.getItems().add(appointment());
        table.getItems().add(appointment());
        setField(c, "adminSessionLabel", session);
        setField(c, "currentUser", new Administrator("adm", "Admin", "a@example.com", "pw"));
        setField(c, "appointmentsCountAdminLabel", count);
        setField(c, "appointmentsTable", table);

        invokePrivate(c, "updateSessionLabel", new Class<?>[]{});
        invokePrivate(c, "updateAdminAppointmentsCount", new Class<?>[]{});

        assertThat(session.getText()).contains("Admin", "Session until");
        assertThat(count.getText()).isNotBlank();
    }

    @Test
    void switchView_coversAuditAndReportsBranches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        VBox dashboard = new VBox();
        VBox audit = new VBox();
        VBox reports = new VBox();
        Button auditBtn = new Button();
        Button reportsBtn = new Button();
        TableView<AuditEntry> auditTable = new TableView<>();
        setField(c, "dashboardView", dashboard);
        setField(c, "auditView", audit);
        setField(c, "reportsView", reports);
        setField(c, "btnNavAudit", auditBtn);
        setField(c, "btnNavReports", reportsBtn);
        setField(c, "auditTable", auditTable);
        setField(c, "reportApptsPerTypeValue", new Label());
        setField(c, "reportCancellationRateLabel", new Label());
        setField(c, "reportPeakHourLabel", new Label());
        setField(c, "reportSummaryLabel", new Label());

        AuditLogService auditService = mock(AuditLogService.class);
        when(auditService.getRecentEntries(500)).thenReturn(List.of());
        ApplicationContext.setAuditLogService(auditService);

        com.appointmentscheduler.application.ScheduleService schedule = mock(com.appointmentscheduler.application.ScheduleService.class);
        com.appointmentscheduler.domain.Schedule emptySchedule = new com.appointmentscheduler.domain.Schedule();
        when(schedule.getMasterSchedule()).thenReturn(emptySchedule);
        ApplicationContext.setScheduleService(schedule);
        com.appointmentscheduler.application.AuthService auth = mock(com.appointmentscheduler.application.AuthService.class);
        com.appointmentscheduler.persistence.UserRepository users = mock(com.appointmentscheduler.persistence.UserRepository.class);
        when(auth.getUserRepository()).thenReturn(users);
        when(users.getAllUsers()).thenReturn(List.of());
        ApplicationContext.setAuthService(auth);

        ReportingService reporting = mock(ReportingService.class);
        when(reporting.getAppointmentsPerType()).thenReturn(Map.of("Consultation", 2L));
        when(reporting.getCancellationRate()).thenReturn(4.5d);
        when(reporting.getPeakBookingHour()).thenReturn(9);
        when(reporting.getTotalAppointmentsCount()).thenReturn(2L);
        when(reporting.getTodayAppointmentsCount()).thenReturn(1L);
        ApplicationContext.setReportingService(reporting);

        assertThatCode(() -> invokePrivate(c, "switchView", new Class<?>[]{VBox.class, Button.class}, audit, auditBtn))
                .doesNotThrowAnyException();
        assertThat(audit.isVisible()).isTrue();
        assertThat(auditBtn.getStyleClass()).contains("sidebar-btn-active");

        assertThatCode(() -> invokePrivate(c, "switchView", new Class<?>[]{VBox.class, Button.class}, reports, reportsBtn))
                .doesNotThrowAnyException();
        assertThat(reports.isVisible()).isTrue();
        assertThat(((Label) getField(c, "reportSummaryLabel")).getText()).contains("Total appointments");
    }

    private static Appointment appointment() {
        User patient = new User("p" + System.nanoTime(), "Patient", "p" + System.nanoTime() + "@example.com", "pw");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return new InPersonAppointment(patient, new TimeSlot(start, start.plusHours(1)), "Room A");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invokePrivate(Object target, String methodName, Class<?>[] argTypes, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        m.invoke(target, args);
    }

    private static String invokeStaticString(String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        return (String) m.invoke(null, args);
    }

    private static void invokeStaticVoid(String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        m.invoke(null, args);
    }
}
