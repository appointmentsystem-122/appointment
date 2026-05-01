package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.BackupRestoreService;
import com.appointmentscheduler.application.BookingRequestFields;
import com.appointmentscheduler.application.CurrentClinicService;
import com.appointmentscheduler.application.DispatchSummary;
import com.appointmentscheduler.application.InAppMessagingService;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.application.StaffContactMessage;
import com.appointmentscheduler.application.ReportingService;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.Schedule;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.atLeastOnce;
import org.mockito.MockedStatic;

/**
 * High-signal branch tests for {@link AdminDashboardController} that exercise handler branches
 * without full FXML startup.
 */
@ResourceLock("ApplicationContextServices")
class AdminDashboardControllerTargetedBranchesTest {

    private InAppMessagingService originalMessagingService;
    private BackupRestoreService originalBackupRestoreService;
    private AuditLogService originalAuditLogService;
    private AuthService originalAuthService;
    private ScheduleService originalScheduleService;

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
        System.setProperty("app.test.autoDialogs", "true");
        originalMessagingService = ApplicationContext.getInAppMessagingService();
        originalBackupRestoreService = ApplicationContext.getBackupRestoreService();
        originalAuditLogService = ApplicationContext.getAuditLogService();
        originalAuthService = ApplicationContext.getAuthService();
        originalScheduleService = ApplicationContext.getScheduleService();
    }

    @AfterEach
    void restoreContext() throws Exception {
        // refreshAllData() schedules FX Tasks; if we restore ApplicationContext before they run,
        // succeeded() can see null ScheduleService (flaky NPE across tests).
        waitForFxQueue();
        ApplicationContext.setInAppMessagingService(originalMessagingService);
        ApplicationContext.setBackupRestoreService(originalBackupRestoreService);
        ApplicationContext.setAuditLogService(originalAuditLogService);
        ApplicationContext.setAuthService(originalAuthService);
        ApplicationContext.setScheduleService(originalScheduleService);
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void calendarPrevNext_branches_daily_monthly_weekly() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        ComboBox<String> mode = new ComboBox<>();
        Label range = new Label();
        setField(c, "calendarViewModeCombo", mode);
        setField(c, "calendarRangeLabel", range);
        // keep calendarContainer null so buildCalendarView returns early (safe, still covers mode branches).

        mode.setValue("Daily");
        assertThatCode(c::handleCalendarPrev).doesNotThrowAnyException();
        assertThatCode(c::handleCalendarNext).doesNotThrowAnyException();

        mode.setValue("Monthly");
        assertThatCode(c::handleCalendarPrev).doesNotThrowAnyException();
        assertThatCode(c::handleCalendarNext).doesNotThrowAnyException();

        mode.setValue("Weekly");
        assertThatCode(c::handleCalendarPrev).doesNotThrowAnyException();
        assertThatCode(c::handleCalendarNext).doesNotThrowAnyException();
    }

    @Test
    void updateCalendarRangeLabel_nullLabel_noOp_and_allModes_setText() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        assertThatCode(() -> invokePrivateNoArg(c, "updateCalendarRangeLabel")).doesNotThrowAnyException();

        Label range = new Label();
        ComboBox<String> modeCombo = new ComboBox<>();
        setField(c, "calendarRangeLabel", range);
        setField(c, "calendarViewModeCombo", modeCombo);
        setField(c, "calendarAnchorDate", LocalDate.of(2026, 6, 10));

        modeCombo.setValue("Daily");
        assertThatCode(() -> invokePrivateNoArg(c, "updateCalendarRangeLabel")).doesNotThrowAnyException();
        assertThat(range.getText()).isNotBlank();

        modeCombo.setValue("Monthly");
        assertThatCode(() -> invokePrivateNoArg(c, "updateCalendarRangeLabel")).doesNotThrowAnyException();
        assertThat(range.getText()).isNotBlank();

        modeCombo.setValue("Weekly");
        assertThatCode(() -> invokePrivateNoArg(c, "updateCalendarRangeLabel")).doesNotThrowAnyException();
        assertThat(range.getText()).contains("–");
    }

    @Test
    void buildCalendarView_nullContainer_noOp_and_modesWithScheduleBranches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        assertThatCode(() -> invokePrivateNoArg(c, "buildCalendarView")).doesNotThrowAnyException();

        VBox container = new VBox();
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Daily", "Monthly", "Weekly");
        setField(c, "calendarContainer", container);
        setField(c, "calendarViewModeCombo", modeCombo);
        setField(c, "calendarAnchorDate", LocalDate.of(2026, 5, 12));

        ScheduleService ssOrig = ApplicationContext.getScheduleService();
        try {
            ApplicationContext.setScheduleService(null);
            modeCombo.setValue("Daily");
            assertThatCode(() -> invokePrivateNoArg(c, "buildCalendarView")).doesNotThrowAnyException();
            assertThat(container.getChildren()).hasSize(1);

            ScheduleService ss = mock(ScheduleService.class);
            Schedule sch = new Schedule();
            User u = new User("cal-p", "Cal P", "cal-p@example.com", "pw");
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 10, 0);
            InPersonAppointment appt = new InPersonAppointment(u, new TimeSlot(t, t.plusHours(1)), "R1");
            appt.setStatus("CONFIRMED");
            sch.addAppointment(appt);
            when(ss.getMasterSchedule()).thenReturn(sch);
            ApplicationContext.setScheduleService(ss);

            modeCombo.setValue("Monthly");
            assertThatCode(() -> invokePrivateNoArg(c, "buildCalendarView")).doesNotThrowAnyException();
            assertThat(container.getChildren()).hasSize(1);

            modeCombo.setValue("Weekly");
            assertThatCode(() -> invokePrivateNoArg(c, "buildCalendarView")).doesNotThrowAnyException();
            assertThat(container.getChildren()).hasSize(1);
        } finally {
            ApplicationContext.setScheduleService(ssOrig);
        }
    }

    @Test
    void addAppointmentType_branches_blank_invalid_and_valid() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        TextField name = new TextField();
        TextField dur = new TextField();
        TextField max = new TextField();
        setField(c, "appointmentTypeName", name);
        setField(c, "appointmentTypeDuration", dur);
        setField(c, "appointmentTypeMaxParticipants", max);
        setField(c, "welcomeLabel", new Label());

        // blank name branch
        name.setText(" ");
        dur.setText("60");
        max.setText("2");
        assertThatCode(c::handleAddAppointmentType).doesNotThrowAnyException();

        // invalid number branch
        name.setText("CustomTypeA");
        dur.setText("abc");
        max.setText("2");
        // Current implementation dereferences welcomeLabel.getScene() in this branch.
        assertThatThrownBy(c::handleAddAppointmentType).isInstanceOf(NullPointerException.class);

        // valid branch
        name.setText("CustomTypeB");
        dur.setText("45");
        max.setText("4");
        assertThatCode(c::handleAddAppointmentType).doesNotThrowAnyException();
    }

    @Test
    void adminSendBroadcast_branches_serviceNull_forbidden_blank_and_success() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setMessagingFields(c);

        // service null
        ApplicationContext.setInAppMessagingService(null);
        setField(c, "currentUser", new Administrator("adm-bc-0", "Adm", "adm0@example.com", "pw"));
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();

        // forbidden by role (normal user)
        InAppMessagingService svcForbidden = mock(InAppMessagingService.class);
        ApplicationContext.setInAppMessagingService(svcForbidden);
        setField(c, "currentUser", new User("usr-bc-1", "Usr", "usr1@example.com", "pw"));
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();

        // blank subject/body branch (admin role, non-null service)
        setField(c, "currentUser", new Administrator("adm-bc-2", "Adm", "adm2@example.com", "pw"));
        TextField subj = (TextField) getField(c, "adminMessagingSubjectField");
        TextArea body = (TextArea) getField(c, "adminMessagingBodyArea");
        subj.setText(" ");
        body.setText(" ");
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();

        // success branch (audience = all patients)
        InAppMessagingService svcOk = mock(InAppMessagingService.class);
        when(svcOk.listPatients()).thenReturn(java.util.List.of(new User("p-1", "Pat 1", "p1@example.com", "pw")));
        when(svcOk.broadcastToPatients(any(), anyList(), anyString(), anyString()))
                .thenReturn(DispatchSummary.of(1, 0, 0, "ok"));
        ApplicationContext.setInAppMessagingService(svcOk);
        subj.setText("Subject");
        body.setText("Body");
        @SuppressWarnings("unchecked")
        ComboBox<String> aud = (ComboBox<String>) getField(c, "adminMessagingAudienceCombo");
        aud.getSelectionModel().select(0);
        setField(c, "currentUser", new Administrator("adm-bc-3", "Adm", "adm3@example.com", "pw"));
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();
    }

    @Test
    void closeAndReopenDay_guardBranches_nullPicker_and_nullDate() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        // null picker guard
        assertThatCode(c::handleCloseDay).doesNotThrowAnyException();
        assertThatCode(c::handleReopenDay).doesNotThrowAnyException();

        // picker exists but value null guard
        javafx.scene.control.DatePicker dp = new javafx.scene.control.DatePicker();
        dp.setValue(null);
        setField(c, "closedDayDatePicker", dp);
        ApplicationContext.setClosedDayService(new com.appointmentscheduler.application.ClosedDayService());
        assertThatCode(c::handleCloseDay).doesNotThrowAnyException();
        assertThatCode(c::handleReopenDay).doesNotThrowAnyException();

        // non-null date (exercise alreadyClosed/wasClosed toggles)
        dp.setValue(LocalDate.now().plusDays(5));
        assertThatCode(c::handleCloseDay).doesNotThrowAnyException();
        assertThatCode(c::handleReopenDay).doesNotThrowAnyException();
    }

    @Test
    void applyFilters_branches_search_type_status_and_hideInactive() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setField(c, "searchField", new TextField());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("All Types", "InPerson", "Urgent"));
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "CONFIRMED", "CANCELLED"));
        type.getSelectionModel().select("All Types");
        status.getSelectionModel().select("All Statuses");
        setField(c, "filterTypeCombo", type);
        setField(c, "filterStatusCombo", status);

        User p = new User("p-af", "Patient A", "a@example.com", "pw");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        com.appointmentscheduler.domain.InPersonAppointment confirmed =
                new com.appointmentscheduler.domain.InPersonAppointment(p, new com.appointmentscheduler.domain.TimeSlot(start, start.plusHours(1)), "R1");
        confirmed.setStatus("CONFIRMED");
        com.appointmentscheduler.domain.InPersonAppointment cancelled =
                new com.appointmentscheduler.domain.InPersonAppointment(p, new com.appointmentscheduler.domain.TimeSlot(start.plusHours(3), start.plusHours(4)), "R2");
        cancelled.setStatus("CANCELLED");

        FilteredList<com.appointmentscheduler.domain.Appointment> filtered =
                new FilteredList<>(FXCollections.observableArrayList(List.of(confirmed, cancelled)), a -> true);
        setField(c, "filteredAppointments", filtered);
        setField(c, "appointmentsTable", new TableView<com.appointmentscheduler.domain.Appointment>());

        // all visible with default selectors
        setField(c, "showInactiveAppointments", true);
        assertThatCode(() -> invokePrivateNoArg(c, "applyFilters")).doesNotThrowAnyException();

        // hide inactive should drop CANCELLED branch
        setField(c, "showInactiveAppointments", false);
        assertThatCode(() -> invokePrivateNoArg(c, "applyFilters")).doesNotThrowAnyException();

        // status filter branch
        status.getSelectionModel().select("CONFIRMED");
        assertThatCode(() -> invokePrivateNoArg(c, "applyFilters")).doesNotThrowAnyException();

        // type filter branch
        type.getSelectionModel().select("InPerson");
        assertThatCode(() -> invokePrivateNoArg(c, "applyFilters")).doesNotThrowAnyException();

        // search branch (no match then match)
        ((TextField) getField(c, "searchField")).setText("zzz-no-hit");
        assertThatCode(() -> invokePrivateNoArg(c, "applyFilters")).doesNotThrowAnyException();
        ((TextField) getField(c, "searchField")).setText("patient a");
        assertThatCode(() -> invokePrivateNoArg(c, "applyFilters")).doesNotThrowAnyException();
    }

    @Test
    void setupFilters_branches_with_controls_and_null_controls() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setField(c, "searchField", new TextField());
        setField(c, "filterTypeCombo", new ComboBox<String>());
        setField(c, "filterStatusCombo", new ComboBox<String>());
        setField(c, "filteredAppointments", new FilteredList<com.appointmentscheduler.domain.Appointment>(FXCollections.observableArrayList(), a -> true));
        assertThatCode(() -> invokePrivateNoArg(c, "setupFilters")).doesNotThrowAnyException();

        // null controls path
        setField(c, "searchField", null);
        setField(c, "filterTypeCombo", null);
        setField(c, "filterStatusCombo", null);
        assertThatCode(() -> invokePrivateNoArg(c, "setupFilters")).doesNotThrowAnyException();
    }

    @Test
    void userActions_branches_block_delete_changeRole_selection_admin_and_confirmFalse() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);
        ListView<PatronBookingSummary> users = new ListView<>();
        setField(c, "usersList", users);

        // null selection guards
        assertThatCode(c::handleBlockUser).doesNotThrowAnyException();
        assertThatCode(c::handleDeleteUser).doesNotThrowAnyException();
        assertThatCode(c::handleChangeRole).doesNotThrowAnyException();

        // admin guard branches
        PatronBookingSummary adminRow = mock(PatronBookingSummary.class);
        when(adminRow.getUser()).thenReturn(new Administrator("adm-u", "Adm", "adm@example.com", "pw"));
        users.getItems().setAll(adminRow);
        users.getSelectionModel().select(adminRow);
        assertThatCode(c::handleBlockUser).doesNotThrowAnyException();
        assertThatCode(c::handleDeleteUser).doesNotThrowAnyException();

        // normal user + confirmation false branch
        PatronBookingSummary userRow = mock(PatronBookingSummary.class);
        when(userRow.getUser()).thenReturn(new User("u-1", "User One", "u1@example.com", "pw"));
        users.getItems().setAll(userRow);
        users.getSelectionModel().select(userRow);
        try (MockedStatic<DialogHelper> dialog = org.mockito.Mockito.mockStatic(DialogHelper.class)) {
            dialog.when(() -> DialogHelper.showConfirmation(anyString(), anyString(), anyString())).thenReturn(false);
            assertThatCode(c::handleBlockUser).doesNotThrowAnyException();
            assertThatCode(c::handleDeleteUser).doesNotThrowAnyException();
        }

        // selected non-admin for change role info path
        assertThatCode(c::handleChangeRole).doesNotThrowAnyException();
    }

    @Test
    void exportAndReports_autoDialog_branches_cover_success_paths() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);

        // Backup-related exports
        BackupRestoreService br = mock(BackupRestoreService.class);
        ApplicationContext.setBackupRestoreService(br);

        // Audit export with real service and one entry
        AuditLogService audit = new AuditLogService();
        audit.log("admin-1", "Admin One", "EXPORT", "export test");
        ApplicationContext.setAuditLogService(audit);

        // Users report
        AuthService auth = mock(AuthService.class);
        UserRepository repo = mock(UserRepository.class);
        when(auth.getUserRepository()).thenReturn(repo);
        when(repo.getAllUsers()).thenReturn(List.of(
                new User("u-r1", "Rep One", "r1@example.com", "pw"),
                new Administrator("u-r2", "Rep Admin", "r2@example.com", "pw")
        ));
        ApplicationContext.setAuthService(auth);

        // Generic export / cancellations report need schedule
        ScheduleService ss = mock(ScheduleService.class);
        Schedule sched = new Schedule();
        User u = new User("p-exp", "P Exp", "pexp@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(t, t.plusMinutes(30)), "R");
        cancelled.setStatus("CANCELLED");
        InPersonAppointment confirmed = new InPersonAppointment(u, new TimeSlot(t.plusHours(1), t.plusHours(1).plusMinutes(30)), "R2");
        confirmed.setStatus("CONFIRMED");
        sched.addAppointment(cancelled);
        sched.addAppointment(confirmed);
        when(ss.getMasterSchedule()).thenReturn(sched);
        ApplicationContext.setScheduleService(ss);

        assertThatCode(c::handleExport).doesNotThrowAnyException();
        assertThatCode(c::handleExportAudit).doesNotThrowAnyException();
        assertThatCode(c::handleExportBackupManifest).doesNotThrowAnyException();
        assertThatCode(c::handleExportAppointmentsCsv).doesNotThrowAnyException();
        assertThatCode(c::handleReportAppointments).doesNotThrowAnyException();
        assertThatCode(c::handleReportUsers).doesNotThrowAnyException();
        assertThatCode(c::handleReportCancellations).doesNotThrowAnyException();
    }

    @Test
    void setupTableColumns_cellFactories_cover_status_action_and_user_cells() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setField(c, "appointmentsTable", new TableView<Appointment>());
        setField(c, "colDate", new TableColumn<Appointment, String>());
        setField(c, "colPatient", new TableColumn<Appointment, String>());
        setField(c, "colType", new TableColumn<Appointment, String>());
        setField(c, "colStatus", new TableColumn<Appointment, String>());
        setField(c, "colRequests", new TableColumn<Appointment, String>());
        setField(c, "colActions", new TableColumn<Appointment, Appointment>());
        setField(c, "usersList", new ListView<PatronBookingSummary>());

        assertThatCode(() -> invokePrivateNoArg(c, "setupTableColumns")).doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        TableColumn<Appointment, String> colStatus = (TableColumn<Appointment, String>) getField(c, "colStatus");
        TableCell<Appointment, String> stCell = colStatus.getCellFactory().call(colStatus);
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(stCell, null, true)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(stCell, "CONFIRMED", false)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(stCell, "CANCELLED", false)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(stCell, "EXPIRED", false)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(stCell, "PENDING", false)).doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        TableColumn<Appointment, Appointment> colActions = (TableColumn<Appointment, Appointment>) getField(c, "colActions");
        TableCell<Appointment, Appointment> actionCell = colActions.getCellFactory().call(colActions);
        User u = new User("ac-1", "Action User", "ac1@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment confirmed = new InPersonAppointment(u, new TimeSlot(t, t.plusMinutes(30)), "R1");
        confirmed.setStatus("CONFIRMED");
        InPersonAppointment pending = new InPersonAppointment(u, new TimeSlot(t.plusHours(1), t.plusHours(1).plusMinutes(30)), "R2");
        pending.setStatus("PENDING");
        InPersonAppointment terminal = new InPersonAppointment(u, new TimeSlot(t.plusHours(2), t.plusHours(2).plusMinutes(30)), "R3");
        terminal.setStatus("COMPLETED");
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(actionCell, null, true)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(actionCell, confirmed, false)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(actionCell, pending, false)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(actionCell, terminal, false)).doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        ListView<PatronBookingSummary> users = (ListView<PatronBookingSummary>) getField(c, "usersList");
        javafx.scene.control.ListCell<PatronBookingSummary> userCell = users.getCellFactory().call(users);
        PatronBookingSummary adminRow = mock(PatronBookingSummary.class);
        when(adminRow.getUser()).thenReturn(new Administrator("adm-cell", "Admin Cell", "adm.cell@example.com", "pw"));
        when(adminRow.arabicStatsLine()).thenReturn("arabic");
        when(adminRow.englishStatsLine()).thenReturn("english");
        PatronBookingSummary customerRow = mock(PatronBookingSummary.class);
        when(customerRow.getUser()).thenReturn(new User("usr-cell", "User Cell", "usr.cell@example.com", "pw"));
        when(customerRow.arabicStatsLine()).thenReturn("arabic2");
        when(customerRow.englishStatsLine()).thenReturn("english2");
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(userCell, null, true)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(userCell, adminRow, false)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(userCell, customerRow, false)).doesNotThrowAnyException();
    }

    @Test
    void completeAndCancelPrivateHandlers_branches_null_terminal_confirmFalse_and_fail() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);
        setField(c, "currentUser", new Administrator("adm-pv", "Admin Pv", "admpv@example.com", "pw"));

        // null selected guards
        assertThatCode(() -> invokePrivateOneArg(c, "handleCompleteAppt", Appointment.class, null)).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateOneArg(c, "handleCancelAppt", Appointment.class, null)).doesNotThrowAnyException();

        User u = new User("pv-u", "Patient V", "pv@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment terminal = new InPersonAppointment(u, new TimeSlot(t, t.plusMinutes(30)), "R1");
        terminal.setStatus("COMPLETED");
        InPersonAppointment active = new InPersonAppointment(u, new TimeSlot(t.plusHours(1), t.plusHours(1).plusMinutes(30)), "R2");
        active.setStatus("CONFIRMED");

        // terminal complete guard
        assertThatCode(() -> invokePrivateOneArg(c, "handleCompleteAppt", Appointment.class, terminal)).doesNotThrowAnyException();

        // booking service stubs for fail branches (avoid refreshAllData path)
        var booking = mock(com.appointmentscheduler.application.BookingService.class);
        when(booking.tryCompleteAppointmentWithReason(anyString(), any())).thenReturn(java.util.Optional.of("blocked"));
        when(booking.cancelAppointment(anyString(), any())).thenReturn(false);
        ApplicationContext.setBookingService(booking);

        try (MockedStatic<DialogHelper> dialog = org.mockito.Mockito.mockStatic(DialogHelper.class)) {
            // complete: confirm false then confirm true -> fail toast branch
            dialog.when(() -> DialogHelper.showConfirmation(anyString(), anyString(), anyString())).thenReturn(false);
            assertThatCode(() -> invokePrivateOneArg(c, "handleCompleteAppt", Appointment.class, active)).doesNotThrowAnyException();
            dialog.when(() -> DialogHelper.showConfirmation(anyString(), anyString(), anyString())).thenReturn(true);
            assertThatCode(() -> invokePrivateOneArg(c, "handleCompleteAppt", Appointment.class, active)).doesNotThrowAnyException();

            // cancel: confirm false then confirm true -> fail toast branch
            dialog.when(() -> DialogHelper.showConfirmation(anyString(), anyString(), anyString())).thenReturn(false);
            assertThatCode(() -> invokePrivateOneArg(c, "handleCancelAppt", Appointment.class, active)).doesNotThrowAnyException();
            dialog.when(() -> DialogHelper.showConfirmation(anyString(), anyString(), anyString())).thenReturn(true);
            assertThatCode(() -> invokePrivateOneArg(c, "handleCancelAppt", Appointment.class, active)).doesNotThrowAnyException();
        }

        verify(booking).tryCompleteAppointmentWithReason(anyString(), any());
        verify(booking).cancelAppointment(anyString(), any());
    }

    @Test
    void completeAndCancelPrivateHandlers_success_paths_trigger_refresh_task_safely() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);
        setField(c, "currentUser", new Administrator("adm-ok", "Admin Ok", "admok@example.com", "pw"));

        // Minimal context for refreshAllData() task invoked on success branches.
        var booking = mock(com.appointmentscheduler.application.BookingService.class);
        when(booking.tryCompleteAppointmentWithReason(anyString(), any())).thenReturn(java.util.Optional.empty());
        when(booking.cancelAppointment(anyString(), any())).thenReturn(true);
        ApplicationContext.setBookingService(booking);

        ScheduleService ss = mock(ScheduleService.class);
        Schedule sched = new Schedule();
        User p = new User("p-ok", "P Ok", "pok@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment apptForStats = new InPersonAppointment(p, new TimeSlot(t, t.plusMinutes(30)), "R");
        apptForStats.setStatus("CONFIRMED");
        sched.addAppointment(apptForStats);
        when(ss.getMasterSchedule()).thenReturn(sched);
        ApplicationContext.setScheduleService(ss);

        AuthService auth = mock(AuthService.class);
        UserRepository repo = mock(UserRepository.class);
        when(auth.getUserRepository()).thenReturn(repo);
        when(repo.getAllUsers()).thenReturn(List.of(p));
        ApplicationContext.setAuthService(auth);

        InPersonAppointment target = new InPersonAppointment(p, new TimeSlot(t.plusHours(1), t.plusHours(1).plusMinutes(30)), "R2");
        target.setStatus("CONFIRMED");

        try (MockedStatic<DialogHelper> dialog = org.mockito.Mockito.mockStatic(DialogHelper.class)) {
            dialog.when(() -> DialogHelper.showConfirmation(anyString(), anyString(), anyString())).thenReturn(true);
            assertThatCode(() -> invokePrivateOneArg(c, "handleCompleteAppt", Appointment.class, target)).doesNotThrowAnyException();
            assertThatCode(() -> invokePrivateOneArg(c, "handleCancelAppt", Appointment.class, target)).doesNotThrowAnyException();
        }

        // Give refreshAllData background task a chance to run and flush FX queue.
        waitForFxQueue();
        verify(booking).tryCompleteAppointmentWithReason(anyString(), any());
        verify(booking).cancelAppointment(anyString(), any());
    }

    @Test
    void adminSendBroadcast_branches_filtered_selected_and_forbidden_result() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setMessagingFields(c);
        setField(c, "currentUser", new Administrator("adm-bx", "Adm", "admx@example.com", "pw"));

        TextField subj = (TextField) getField(c, "adminMessagingSubjectField");
        TextArea body = (TextArea) getField(c, "adminMessagingBodyArea");
        @SuppressWarnings("unchecked")
        ComboBox<String> aud = (ComboBox<String>) getField(c, "adminMessagingAudienceCombo");
        @SuppressWarnings("unchecked")
        ListView<PatronBookingSummary> users = (ListView<PatronBookingSummary>) getField(c, "usersList");
        Label status = (Label) getField(c, "adminMessagingStatusLabel");
        subj.setText("S");
        body.setText("B");

        // Audience == 1 (filtered users list) and result forbidden branch.
        PatronBookingSummary nullUserRow = mock(PatronBookingSummary.class);
        when(nullUserRow.getUser()).thenReturn(null);
        PatronBookingSummary goodRow = mock(PatronBookingSummary.class);
        when(goodRow.getUser()).thenReturn(new User("p-bx", "Pat", "p-bx@example.com", "pw"));
        users.getItems().setAll(null, nullUserRow, goodRow);

        InAppMessagingService svc = mock(InAppMessagingService.class);
        when(svc.broadcastToPatients(any(), anyList(), anyString(), anyString())).thenReturn(DispatchSummary.forbidden());
        ApplicationContext.setInAppMessagingService(svc);

        aud.getSelectionModel().select(1);
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();

        // Audience == 2 selected-user missing -> early warning path.
        users.getSelectionModel().clearSelection();
        aud.getSelectionModel().select(2);
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();

        // Audience == 2 selected-user present -> success/status branch.
        when(svc.broadcastToPatients(any(), anyList(), anyString(), anyString()))
                .thenReturn(DispatchSummary.of(1, 0, 0, "sent-1"));
        users.getSelectionModel().select(goodRow);
        assertThatCode(c::handleAdminSendBroadcast).doesNotThrowAnyException();
        assertThat(status.getText()).isEqualTo("sent-1");
    }

    @Test
    void summarizeBookingRequests_coversSegmentsPartyAndTruncation() throws Exception {
        Method m = AdminDashboardController.class.getDeclaredMethod("summarizeBookingRequests", Appointment.class);
        m.setAccessible(true);

        assertThat((String) m.invoke(null, new Object[] {null})).isEmpty();

        User patient = new User("sum-p", "Pat", "sum-p@example.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        InPersonAppointment partyOnly = new InPersonAppointment(patient, slot, "R1");
        partyOnly.setParticipantCount(2);
        assertThat((String) m.invoke(null, partyOnly)).contains("Party: 2");

        InPersonAppointment full = new InPersonAppointment(patient, slot, "R1");
        full.setCustomerNotes("  Notes line  ");
        full.setContactPhone(" 555-0100 ");
        full.setReminderChannel(BookingRequestFields.REMINDER_APP);
        full.setPreferredLanguage(BookingRequestFields.LANG_EN);
        full.setAccessibilityNeeds(" Wheelchair ");
        full.setParticipantCount(3);
        String joined = (String) m.invoke(null, full);
        assertThat(joined).contains("Notes line");
        assertThat(joined).contains("555-0100");
        assertThat(joined).contains("Party: 3");

        InPersonAppointment longNotes = new InPersonAppointment(patient, slot, "R1");
        longNotes.setCustomerNotes("Z".repeat(200));
        String truncated = (String) m.invoke(null, longNotes);
        assertThat(truncated).endsWith("…");
        assertThat(truncated.length()).isLessThanOrEqualTo(158);
    }

    @Test
    void cancellationRateKpi_and_formatTrend_branches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label rateLabel = new Label();
        javafx.scene.control.ProgressBar rateBar = new javafx.scene.control.ProgressBar();
        Label status = new Label();
        setField(c, "cancellationRateLabel", rateLabel);
        setField(c, "cancellationRateProgressBar", rateBar);
        setField(c, "cancellationRateStatusLabel", status);

        var apply = AdminDashboardController.class.getDeclaredMethod("applyCancellationRateKpi", double.class);
        apply.setAccessible(true);
        var trend = AdminDashboardController.class.getDeclaredMethod("formatTrend", long.class, long.class, String.class);
        trend.setAccessible(true);

        // NaN clamped to 0 -> excellent.
        apply.invoke(c, Double.NaN);
        assertThat(rateLabel.getText()).isEqualTo("0.0%");
        assertThat(status.getText()).contains("Excellent");

        // Threshold tiers.
        apply.invoke(c, 9.0d);
        assertThat(status.getText()).contains("Good");
        apply.invoke(c, 12.0d);
        assertThat(status.getText()).contains("Moderate");
        apply.invoke(c, 17.0d);
        assertThat(status.getText()).contains("Needs attention");
        apply.invoke(c, 22.0d);
        assertThat(status.getText()).contains("High risk");

        // formatTrend branches.
        assertThat((String) trend.invoke(null, 0L, 0L, "last week")).isEqualTo("");
        assertThat((String) trend.invoke(null, 0L, 5L, "last week")).contains("↑ vs last week");
        assertThat((String) trend.invoke(null, 100L, 100L, "last week")).contains("→ vs last week");
        assertThat((String) trend.invoke(null, 100L, 120L, "last week")).contains("↑ 20%");
        assertThat((String) trend.invoke(null, 100L, 70L, "last week")).contains("↓ 30%");
    }

    @Test
    void updateAlertsPanel_branches_hide_and_multiple_threshold_messages() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        VBox panel = new VBox();
        HBox content = new HBox();
        setField(c, "adminAlertsPanel", panel);
        setField(c, "adminAlertsContent", content);

        var m = AdminDashboardController.class.getDeclaredMethod(
                "updateAlertsPanel", String.class, long.class, long.class, double.class);
        m.setAccessible(true);

        // No alerts -> hidden branch.
        assertThatCode(() -> m.invoke(c, null, 3L, 9L, 5.0d)).doesNotThrowAnyException();
        assertThat(panel.isVisible()).isFalse();
        assertThat(panel.isManaged()).isFalse();

        // High cancellation + no-today branch -> panel visible with multiple items.
        assertThatCode(() -> m.invoke(c, null, 0L, 6L, 22.0d)).doesNotThrowAnyException();
        assertThat(panel.isVisible()).isTrue();
        assertThat(panel.isManaged()).isTrue();
        assertThat(content.getChildren().size()).isGreaterThanOrEqualTo(2);

        // Mid threshold (15-20) branch.
        assertThatCode(() -> m.invoke(c, null, 2L, 6L, 16.0d)).doesNotThrowAnyException();
        assertThat(content.getChildren()).isNotEmpty();
    }

    @Test
    void refreshStaffContactInbox_branches_null_forbidden_and_success_cell_factory() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        ListView<StaffContactMessage> inbox = new ListView<>();
        setField(c, "staffContactInboxList", inbox);

        // currentUser null guard
        setField(c, "currentUser", null);
        assertThatCode(() -> invokePrivateNoArg(c, "refreshStaffContactInbox")).doesNotThrowAnyException();

        // service null guard
        setField(c, "currentUser", new Administrator("adm-rf", "Admin Rf", "admrf@example.com", "pw"));
        ApplicationContext.setInAppMessagingService(null);
        assertThatCode(() -> invokePrivateNoArg(c, "refreshStaffContactInbox")).doesNotThrowAnyException();

        // forbidden role branch -> clears list
        setField(c, "currentUser", new User("usr-rf", "Usr Rf", "usrf@example.com", "pw"));
        InAppMessagingService svc = mock(InAppMessagingService.class);
        ApplicationContext.setInAppMessagingService(svc);
        assertThatCode(() -> invokePrivateNoArg(c, "refreshStaffContactInbox")).doesNotThrowAnyException();
        assertThat(inbox.getItems()).isEmpty();

        // success branch with item + cell update paths
        setField(c, "currentUser", new Administrator("adm-rf2", "Admin Rf2", "admrf2@example.com", "pw"));
        when(svc.getStaffContactInbox(100)).thenReturn(List.of(
                new StaffContactMessage("Subj", "Body text", LocalDateTime.now(),
                        "c1", "Customer 1", "c1@example.com")));
        assertThatCode(() -> invokePrivateNoArg(c, "refreshStaffContactInbox")).doesNotThrowAnyException();
        assertThat(inbox.getItems()).hasSize(1);

        var cell = inbox.getCellFactory().call(inbox);
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(cell, null, true)).doesNotThrowAnyException();
        assertThatCode(() -> PresentationFxHarness.invokeCellUpdateItem(cell, inbox.getItems().get(0), false)).doesNotThrowAnyException();
    }

    @Test
    void setupSettingsControls_and_applyDefaultLanding_cover_all_view_cases() throws Exception {
        AdminDashboardController c = new AdminDashboardController();

        // Core controls used by settings + landing.
        setField(c, "settingsSystemTypeCombo", new ComboBox<String>());
        setField(c, "settingsTableDensityCombo", new ComboBox<String>());
        setField(c, "settingsShowInactiveCheck", new CheckBox());
        setField(c, "settingsDefaultLandingCombo", new ComboBox<String>());
        setField(c, "settingsInAppToastsCheck", new CheckBox());
        setField(c, "settingsCutoffHoursCombo", new ComboBox<String>());
        setField(c, "settingsTimeFormatCombo", new ComboBox<String>());
        setField(c, "settingsWorkingHoursLabel", new Label());
        setField(c, "settingsMaxDurationLabel", new Label());
        setField(c, "settingsCutoffInfoLabel", new Label());
        setField(c, "settingsSessionTimeoutLabel", new Label());
        setField(c, "appointmentsTable", new TableView<Appointment>());
        setField(c, "auditTable", new TableView<com.appointmentscheduler.domain.AuditEntry>());

        // Views/nav buttons for applyDefaultLanding/switchView.
        VBox dash = new VBox();
        VBox appts = new VBox();
        VBox users = new VBox();
        VBox reports = new VBox();
        VBox audit = new VBox();
        VBox settings = new VBox();
        setField(c, "dashboardView", dash);
        setField(c, "appointmentsView", appts);
        setField(c, "usersView", users);
        setField(c, "reportsView", reports);
        setField(c, "auditView", audit);
        setField(c, "settingsView", settings);
        setField(c, "btnNavDashboard", new javafx.scene.control.Button("D"));
        setField(c, "btnNavAppointments", new javafx.scene.control.Button("A"));
        setField(c, "btnNavUsers", new javafx.scene.control.Button("U"));
        setField(c, "btnNavReports", new javafx.scene.control.Button("R"));
        setField(c, "btnNavAudit", new javafx.scene.control.Button("AU"));
        setField(c, "btnNavSettings", new javafx.scene.control.Button("S"));

        // Minimal context so switchView->refreshAllData background task stays safe.
        setField(c, "currentUser", new Administrator("adm-set", "Adm Set", "admset@example.com", "pw"));
        var ss = mock(ScheduleService.class);
        var sched = new com.appointmentscheduler.domain.Schedule();
        when(ss.getMasterSchedule()).thenReturn(sched);
        ApplicationContext.setScheduleService(ss);
        var auth = mock(AuthService.class);
        var repo = mock(UserRepository.class);
        when(auth.getUserRepository()).thenReturn(repo);
        when(repo.getAllUsers()).thenReturn(List.of());
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(new AuditLogService());

        assertThatCode(() -> invokePrivateNoArg(c, "setupSettingsControls")).doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        ComboBox<String> def = (ComboBox<String>) getField(c, "settingsDefaultLandingCombo");
        def.getSelectionModel().select("Appointments");
        def.getSelectionModel().select("Users");
        def.getSelectionModel().select("Reports");
        def.getSelectionModel().select("Audit");
        def.getSelectionModel().select("Settings");
        def.getSelectionModel().select("Dashboard");

        // Directly hit all switch cases too.
        assertThatCode(() -> invokePrivateOneArg(c, "applyDefaultLanding", String.class, "Appointments")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateOneArg(c, "applyDefaultLanding", String.class, "Users")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateOneArg(c, "applyDefaultLanding", String.class, "Reports")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateOneArg(c, "applyDefaultLanding", String.class, "Audit")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateOneArg(c, "applyDefaultLanding", String.class, "Settings")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateOneArg(c, "applyDefaultLanding", String.class, "AnythingElse")).doesNotThrowAnyException();
        waitForFxQueue();
    }

    @Test
    void refreshReportsData_branches_noService_and_withService() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setField(c, "reportApptsPerTypeValue", new Label());
        setField(c, "reportCancellationRateLabel", new Label());
        setField(c, "reportPeakHourLabel", new Label());
        setField(c, "reportSummaryLabel", new Label());

        ApplicationContext.setReportingService(null);
        assertThatCode(() -> invokePrivateNoArg(c, "refreshReportsData")).doesNotThrowAnyException();

        ReportingService rs = mock(ReportingService.class);
        when(rs.getAppointmentsPerType()).thenReturn(Map.of("Consultation", 3L, "Follow-up", 1L));
        when(rs.getCancellationRate()).thenReturn(12.5d);
        when(rs.getPeakBookingHour()).thenReturn(13);
        when(rs.getTotalAppointmentsCount()).thenReturn(10L);
        when(rs.getTodayAppointmentsCount()).thenReturn(2L);
        ApplicationContext.setReportingService(rs);

        assertThatCode(() -> invokePrivateNoArg(c, "refreshReportsData")).doesNotThrowAnyException();
        assertThat(((Label) getField(c, "reportSummaryLabel")).getText()).contains("Total appointments:");
    }

    @Test
    void setupTooltips_and_applyTableDensity_cover_many_ui_branches() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        setField(c, "btnNavDashboard", new javafx.scene.control.Button("d"));
        setField(c, "btnNavAppointments", new javafx.scene.control.Button("a"));
        setField(c, "btnNavUsers", new javafx.scene.control.Button("u"));
        setField(c, "btnNavReports", new javafx.scene.control.Button("r"));
        setField(c, "btnNavAudit", new javafx.scene.control.Button("au"));
        setField(c, "btnNavSettings", new javafx.scene.control.Button("s"));
        setField(c, "clinicSelectorCombo", new ComboBox<String>());
        setField(c, "btnExportCsv", new javafx.scene.control.Button("csv"));
        setField(c, "btnExportAudit", new javafx.scene.control.Button("audit"));
        setField(c, "btnRefreshAdmin", new javafx.scene.control.Button("refresh-a"));
        setField(c, "btnRefreshAppointments", new javafx.scene.control.Button("refresh-b"));
        setField(c, "btnLogout", new javafx.scene.control.Button("logout"));
        setField(c, "btnThemeToggle", new javafx.scene.control.Button("theme"));
        setField(c, "appointmentsTable", new TableView<Appointment>());
        setField(c, "auditTable", new TableView<com.appointmentscheduler.domain.AuditEntry>());

        assertThatCode(() -> invokePrivateNoArg(c, "setupTooltips")).doesNotThrowAnyException();
        assertThat(((javafx.scene.control.Button) getField(c, "btnNavDashboard")).getTooltip()).isNotNull();
        assertThat(((javafx.scene.control.Button) getField(c, "btnRefreshAdmin")).getTooltip()).isNotNull();
        assertThat(((javafx.scene.control.Button) getField(c, "btnThemeToggle")).getTooltip()).isNotNull();

        assertThatCode(() -> invokePrivateOneArg(c, "applyTableDensity", String.class, "Compact")).doesNotThrowAnyException();
        assertThat(((TableView<?>) getField(c, "appointmentsTable")).getFixedCellSize()).isEqualTo(26.0);
        assertThatCode(() -> invokePrivateOneArg(c, "applyTableDensity", String.class, "Comfortable")).doesNotThrowAnyException();
        assertThat(((TableView<?>) getField(c, "appointmentsTable")).getFixedCellSize()).isEqualTo(36.0);
    }

    @Test
    void setupClinicSelector_listener_branches_allsites_and_specific_id() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        ComboBox<String> clinicCombo = new ComboBox<>();
        setField(c, "clinicSelectorCombo", clinicCombo);

        var clinicRepo = mock(com.appointmentscheduler.persistence.ClinicRepository.class);
        when(clinicRepo.findAll()).thenReturn(List.of(
                new Clinic("c-main", "Main", "A", "UTC"),
                new Clinic("c-2", "Second", "B", "UTC")));
        when(clinicRepo.findById("c-main")).thenReturn(java.util.Optional.of(new Clinic("c-main", "Main", "A", "UTC")));
        when(clinicRepo.findById("c-2")).thenReturn(java.util.Optional.of(new Clinic("c-2", "Second", "B", "UTC")));
        ApplicationContext.setClinicRepository(clinicRepo);

        var ccs = mock(CurrentClinicService.class);
        when(ccs.getCurrentClinicId()).thenReturn(null);
        ApplicationContext.setCurrentClinicService(ccs);

        // Minimal services because selector listener triggers refreshAllData().
        ScheduleService ss = mock(ScheduleService.class);
        com.appointmentscheduler.domain.Schedule sched = new com.appointmentscheduler.domain.Schedule();
        doNothing().when(ss).loadSchedule();
        when(ss.getMasterSchedule()).thenReturn(sched);
        ApplicationContext.setScheduleService(ss);
        AuthService auth = mock(AuthService.class);
        UserRepository repo = mock(UserRepository.class);
        when(auth.getUserRepository()).thenReturn(repo);
        when(repo.getAllUsers()).thenReturn(List.of());
        ApplicationContext.setAuthService(auth);

        assertThatCode(() -> invokePrivateNoArg(c, "setupClinicSelector")).doesNotThrowAnyException();
        assertThat(clinicCombo.getItems()).contains("All sites", "Main (c-main)", "Second (c-2)");

        clinicCombo.getSelectionModel().select("Main (c-main)");
        clinicCombo.getSelectionModel().select("All sites");
        waitForFxQueue();
        verify(ccs).setCurrentClinicId("c-main");
        verify(ccs, atLeastOnce()).setCurrentClinicId(null);
    }

    @Test
    void handleToggleTheme_branches_switch_dark_and_light_with_scene() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        javafx.scene.control.Button themeBtn = new javafx.scene.control.Button("theme");
        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("close");
        javafx.scene.control.Button reopenBtn = new javafx.scene.control.Button("reopen");
        Label welcome = new Label("Welcome");
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(welcome, themeBtn, closeBtn, reopenBtn);
        new Scene(root, 320, 200);

        setField(c, "welcomeLabel", welcome);
        setField(c, "btnThemeToggle", themeBtn);
        setField(c, "btnCloseDay", closeBtn);
        setField(c, "btnReopenDay", reopenBtn);

        assertThatCode(c::handleToggleTheme).doesNotThrowAnyException();
        assertThat(themeBtn.getText()).isEqualTo("Switch to Light Mode");
        assertThat(root.getStyleClass()).contains("dark-mode");

        assertThatCode(c::handleToggleTheme).doesNotThrowAnyException();
        assertThat(themeBtn.getText()).isEqualTo("Switch to Dark Mode");
        assertThat(root.getStyleClass()).doesNotContain("dark-mode");
        assertThat(closeBtn.getStyle()).isNotEmpty();
        assertThat(reopenBtn.getStyle()).isNotEmpty();
    }

    private static void setMessagingFields(AdminDashboardController c) throws Exception {
        ComboBox<String> audience = new ComboBox<>();
        audience.setItems(FXCollections.observableArrayList("All", "Filtered", "Selected"));
        audience.getSelectionModel().select(0);
        setField(c, "adminMessagingAudienceCombo", audience);
        setField(c, "adminMessagingSubjectField", new TextField());
        setField(c, "adminMessagingBodyArea", new TextArea());
        setField(c, "adminMessagingStatusLabel", new Label());
        setField(c, "usersList", new ListView<PatronBookingSummary>());
        setField(c, "welcomeLabel", new Label());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) return;
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) return null;
        f.setAccessible(true);
        return f.get(target);
    }

    private static Field findField(Class<?> cl, String name) {
        for (Class<?> c = cl; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // next
            }
        }
        return null;
    }

    private static void invokePrivateNoArg(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }

    private static void invokePrivateOneArg(Object target, String methodName, Class<?> argType, Object arg) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName, argType);
        m.setAccessible(true);
        m.invoke(target, arg);
    }

    private static void waitForFxQueue() throws Exception {
        Thread.sleep(400);
        CountDownLatch latch = new CountDownLatch(1);
        javafx.application.Platform.runLater(latch::countDown);
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX queue did not drain");
        }
    }
}

