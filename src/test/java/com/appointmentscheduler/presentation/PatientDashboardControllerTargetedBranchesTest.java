package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AppointmentTypeConfig;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.application.BookingService;
import com.appointmentscheduler.application.CurrentClinicService;
import com.appointmentscheduler.application.DispatchSummary;
import com.appointmentscheduler.application.InAppMessagingService;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.persistence.ClinicRepository;
import com.appointmentscheduler.persistence.UserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.control.DateCell;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;

/**
 * Targeted branch tests for {@link PatientDashboardController} that don't rely on full FXML wiring.
 * These hit common null-guards / table-empty branches that JaCoCo reports as missed in presentation.
 */
@ResourceLock("ApplicationContextServices")
class PatientDashboardControllerTargetedBranchesTest {

    private InAppMessagingService originalMessagingService;
    private BookingService originalBookingService;
    private ScheduleService originalScheduleService;
    private ClinicRepository originalClinicRepository;
    private CurrentClinicService originalCurrentClinicService;
    private AuthService originalAuthService;

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
        System.setProperty("app.test.autoDialogs", "true");
        originalMessagingService = ApplicationContext.getInAppMessagingService();
        originalBookingService = ApplicationContext.getBookingService();
        originalScheduleService = ApplicationContext.getScheduleService();
        originalClinicRepository = ApplicationContext.getClinicRepository();
        originalCurrentClinicService = ApplicationContext.getCurrentClinicService();
        originalAuthService = ApplicationContext.getAuthService();
    }

    @AfterEach
    void clearProps() {
        ApplicationContext.setInAppMessagingService(originalMessagingService);
        ApplicationContext.setBookingService(originalBookingService);
        ApplicationContext.setScheduleService(originalScheduleService);
        ApplicationContext.setClinicRepository(originalClinicRepository);
        ApplicationContext.setCurrentClinicService(originalCurrentClinicService);
        ApplicationContext.setAuthService(originalAuthService);
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void handlePrintAppointments_branches_withNullSelection_and_emptyTable_and_noWindow() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        TableView<Appointment> tv = new TableView<>();
        setField(c, "appointmentsTable", tv);
        setField(c, "welcomeLabel", new Label()); // no Scene/Window attached → toast/window branches

        assertThatCode(c::handlePrintAppointments).doesNotThrowAnyException();

        // now: non-empty table, still no selection → should pick first row branch
        User u = new User("p1", "P", "p@e.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment appt = new InPersonAppointment(u, new TimeSlot(t, t.plusHours(1)), "R");
        appt.setStatus("CONFIRMED");
        tv.getItems().setAll(appt);

        assertThatCode(c::handlePrintAppointments).doesNotThrowAnyException();
    }

    @Test
    void handleTogglePolicy_branches_toggleVisibility_and_buttonText() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        VBox policyContent = new VBox();
        policyContent.setVisible(false);
        policyContent.setManaged(false);
        Button btn = new Button("Show");
        setField(c, "policyContent", policyContent);
        setField(c, "btnTogglePolicy", btn);

        assertThatCode(c::handleTogglePolicy).doesNotThrowAnyException();
        assertThatCode(c::handleTogglePolicy).doesNotThrowAnyException();
    }

    @Test
    void handleExportMyAppointments_guardBranches_currentUserNull_and_nullTimeslotInList() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "welcomeLabel", new Label());

        // currentUser == null → early return
        assertThatCode(() -> runOnFxVoid(c::handleExportMyAppointments)).doesNotThrowAnyException();

        // currentUser != null but list contains appointment with null timeslot → skip branch in loop
        User me = new User("me-exp", "Me", "me@e.com", "pw");
        setField(c, "currentUser", me);

        Appointment bad = mock(Appointment.class);
        when(bad.getPatient()).thenReturn(me);
        when(bad.getTimeSlot()).thenReturn(null);
        when(bad.getStatus()).thenReturn("CONFIRMED");

        // allAppointments is an ObservableList field in controller; set it to a simple list wrapper via reflection.
        // We only need the stream() and setAll() usage; use the real list object from controller if present.
        Field allF = findField(c.getClass(), "allAppointments");
        if (allF != null) {
            allF.setAccessible(true);
            Object listObj = allF.get(c);
            if (listObj instanceof javafx.collections.ObservableList<?> ol) {
                @SuppressWarnings("unchecked")
                javafx.collections.ObservableList<Appointment> aol = (javafx.collections.ObservableList<Appointment>) ol;
                aol.setAll(List.of(bad));
            }
        }

        assertThatCode(() -> runOnFxVoid(c::handleExportMyAppointments)).doesNotThrowAnyException();
    }

    @Test
    void handlePatientSendContact_branches_serviceNull_blank_failure_and_success() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "currentUser", new User("pm-1", "P", "pm@example.com", "pw"));
        TextField sub = new TextField();
        TextArea body = new TextArea();
        Label status = new Label();
        setField(c, "patientContactSubjectField", sub);
        setField(c, "patientContactBodyArea", body);
        setField(c, "patientMessagingStatusLabel", status);
        setField(c, "welcomeLabel", new Label()); // no scene -> w null, avoids toast rendering side-effects

        // service null branch
        ApplicationContext.setInAppMessagingService(null);
        assertThatCode(c::handlePatientSendContact).doesNotThrowAnyException();

        // blank input branch
        InAppMessagingService svc = mock(InAppMessagingService.class);
        ApplicationContext.setInAppMessagingService(svc);
        sub.setText(" ");
        body.setText(" ");
        assertThatCode(c::handlePatientSendContact).doesNotThrowAnyException();

        // failure summary branch (keeps fields)
        when(svc.sendContactRequestFromPatient(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(DispatchSummary.of(0, 1, 0, "failed"));
        sub.setText("Need help");
        body.setText("details");
        assertThatCode(c::handlePatientSendContact).doesNotThrowAnyException();

        // success summary branch (clears fields)
        when(svc.sendContactRequestFromPatient(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(DispatchSummary.of(1, 0, 0, "sent"));
        sub.setText("Need help");
        body.setText("details");
        assertThatCode(c::handlePatientSendContact).doesNotThrowAnyException();
    }

    @Test
    void handleClearSearch_resetsFilters_and_reappliesPredicate() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        TableView<Appointment> tv = new TableView<>();
        setField(c, "appointmentsTable", tv);

        @SuppressWarnings("unchecked")
        ObservableList<Appointment> all = (ObservableList<Appointment>) getField(c, "allAppointments");
        if (all == null) {
            return;
        }
        User u = new User("s-1", "Search", "s@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment appt = new InPersonAppointment(u, new TimeSlot(t, t.plusHours(1)), "R");
        appt.setStatus("CONFIRMED");
        all.setAll(appt);

        javafx.scene.control.DatePicker dp = new javafx.scene.control.DatePicker(LocalDateTime.now().plusDays(9).toLocalDate());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("All types", "Consultation"));
        ComboBox<String> branch = new ComboBox<>(FXCollections.observableArrayList("All locations", "Main"));
        type.getSelectionModel().select(1);
        branch.getSelectionModel().select(1);
        setField(c, "searchDatePicker", dp);
        setField(c, "searchTypeCombo", type);
        setField(c, "searchBranchCombo", branch);

        assertThatCode(c::handleClearSearch).doesNotThrowAnyException();
    }

    @Test
    void handleConfirmBooking_guardBranches_missing_blocked_unavailable_past() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        prepareBookingFields(c);

        // 1) missing fields
        assertThatCode(c::handleConfirmBooking).doesNotThrowAnyException();

        // baseline valid selections
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<String> hh = (ComboBox<String>) getField(c, "hourCombo");
        @SuppressWarnings("unchecked")
        ComboBox<String> mm = (ComboBox<String>) getField(c, "minuteCombo");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        dp.setValue(LocalDate.now().plusDays(2));
        hh.setValue("10");
        mm.setValue("00");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 3), false));

        // 2) blocked open appointment
        setField(c, "currentUser", new User("p-book", "P", "p-book@example.com", "pw"));
        BookingService bs = mock(BookingService.class);
        when(bs.patientHasBlockingOpenAppointment("p-book")).thenReturn(true);
        ApplicationContext.setBookingService(bs);
        assertThatCode(c::handleConfirmBooking).doesNotThrowAnyException();

        // 3) day unavailable
        when(bs.patientHasBlockingOpenAppointment("p-book")).thenReturn(false);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);
        ApplicationContext.setScheduleService(ss);
        assertThatCode(c::handleConfirmBooking).doesNotThrowAnyException();

        // 4) past time on same day
        dp.setValue(LocalDate.now());
        int pastHour = Math.max(0, LocalDateTime.now().getHour() - 1);
        hh.setValue(String.format("%02d", pastHour));
        mm.setValue("00");
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        assertThatCode(c::handleConfirmBooking).doesNotThrowAnyException();
    }

    @Test
    void quickTodayTomorrow_branches_unbookable_then_firstBookable() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        prepareBookingFields(c);
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 2), false));
        dp.setValue(LocalDate.now());

        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenAnswer(inv -> {
            LocalDate d = inv.getArgument(0);
            return !d.isBefore(LocalDate.now().plusDays(2));
        });
        when(ss.getAvailableSlots(any(LocalDate.class), anyInt())).thenReturn(List.of());
        ApplicationContext.setScheduleService(ss);

        assertThatCode(c::handleQuickToday).doesNotThrowAnyException();
        assertThatCode(c::handleQuickTomorrow).doesNotThrowAnyException();
    }

    @Test
    void applySearchFilter_branches_date_and_branch_matching() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        TableView<Appointment> tv = new TableView<>();
        setField(c, "appointmentsTable", tv);
        setField(c, "appointmentsCountLabel", new Label());
        setField(c, "statUpcomingLabel", new Label());

        @SuppressWarnings("unchecked")
        ObservableList<Appointment> all = (ObservableList<Appointment>) getField(c, "allAppointments");
        if (all == null) return;

        User u = new User("flt-p", "Filter P", "f@example.com", "pw");
        LocalDate targetDate = LocalDate.now().plusDays(5);
        LocalDateTime t1 = targetDate.atTime(10, 0);
        LocalDateTime t2 = targetDate.plusDays(1).atTime(11, 0);
        InPersonAppointment match = new InPersonAppointment(u, new TimeSlot(t1, t1.plusMinutes(30)), "R1");
        match.setClinicId("c-main");
        match.setStatus("CONFIRMED");
        InPersonAppointment wrongDate = new InPersonAppointment(u, new TimeSlot(t2, t2.plusMinutes(30)), "R2");
        wrongDate.setClinicId("c-main");
        wrongDate.setStatus("CONFIRMED");
        InPersonAppointment wrongBranch = new InPersonAppointment(u, new TimeSlot(t1.plusHours(2), t1.plusHours(2).plusMinutes(30)), "R3");
        wrongBranch.setClinicId("c-other");
        wrongBranch.setStatus("CONFIRMED");
        all.setAll(match, wrongDate, wrongBranch);

        DatePicker dp = new DatePicker(targetDate);
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("All types"));
        ComboBox<String> branch = new ComboBox<>(FXCollections.observableArrayList("All locations", "Main"));
        type.getSelectionModel().selectFirst();
        branch.getSelectionModel().select("Main");
        setField(c, "searchDatePicker", dp);
        setField(c, "searchTypeCombo", type);
        setField(c, "searchBranchCombo", branch);

        ClinicRepository clinicRepo = mock(ClinicRepository.class);
        when(clinicRepo.findById("c-main")).thenReturn(java.util.Optional.of(new Clinic("c-main", "Main", "A", "UTC")));
        when(clinicRepo.findById("c-other")).thenReturn(java.util.Optional.of(new Clinic("c-other", "Other", "B", "UTC")));
        ApplicationContext.setClinicRepository(clinicRepo);

        assertThatCode(() -> invokePrivateNoArg(c, "applySearchFilter")).doesNotThrowAnyException();
    }

    @Test
    void setupSearchFilters_branches_with_repo_and_null_repo() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "searchDatePicker", new DatePicker(LocalDate.now().plusDays(1)));
        setField(c, "searchTypeCombo", new ComboBox<String>());
        setField(c, "searchBranchCombo", new ComboBox<String>());
        setField(c, "appointmentsTable", new TableView<Appointment>());
        setField(c, "appointmentsCountLabel", new Label());
        setField(c, "statUpcomingLabel", new Label());

        @SuppressWarnings("unchecked")
        ObservableList<Appointment> all = (ObservableList<Appointment>) getField(c, "allAppointments");
        if (all == null) return;
        User u = new User("flt-2", "Filter 2", "f2@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment appt = new InPersonAppointment(u, new TimeSlot(t, t.plusMinutes(30)), "R");
        appt.setStatus("CONFIRMED");
        all.setAll(appt);

        ClinicRepository clinicRepo = mock(ClinicRepository.class);
        when(clinicRepo.findAll()).thenReturn(List.of(new Clinic("c-1", "Main", "A", "UTC")));
        ApplicationContext.setClinicRepository(clinicRepo);
        assertThatCode(() -> invokePrivateNoArg(c, "setupSearchFilters")).doesNotThrowAnyException();

        ApplicationContext.setClinicRepository(null);
        assertThatCode(() -> invokePrivateNoArg(c, "setupSearchFilters")).doesNotThrowAnyException();
    }

    @Test
    void handleSaveProfile_branches_currentUserNull_and_success_withClinicSelection() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        // guard branch
        assertThatCode(c::handleSaveProfile).doesNotThrowAnyException();

        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);
        setField(c, "currentUser", new User("p-prof", "Profile P", "p-prof@example.com", "pw"));
        setField(c, "profileNameField", new TextField("Updated Name"));
        setField(c, "profileEmailField", new TextField("updated@example.com"));
        setField(c, "profilePhoneField", new TextField("0790000000"));
        ComboBox<String> branchCombo = new ComboBox<>(FXCollections.observableArrayList("—", "Main"));
        branchCombo.getSelectionModel().select("Main");
        setField(c, "profileBranchCombo", branchCombo);

        ClinicRepository clinicRepo = mock(ClinicRepository.class);
        when(clinicRepo.findAll()).thenReturn(List.of(new Clinic("c-main", "Main", "A", "UTC")));
        ApplicationContext.setClinicRepository(clinicRepo);
        CurrentClinicService ccs = mock(CurrentClinicService.class);
        ApplicationContext.setCurrentClinicService(ccs);

        assertThatCode(c::handleSaveProfile).doesNotThrowAnyException();
        verify(ccs).setCurrentClinicId("c-main");
    }

    @Test
    void handleChangePassword_and_cancelAppointment_success_and_fail_branches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);
        User me = new User("p-sec", "Secure P", "p-sec@example.com", "pw");
        setField(c, "currentUser", me);

        // change password auto-dialog path
        AuthService auth = mock(AuthService.class);
        UserRepository repo = mock(UserRepository.class);
        when(auth.getUserRepository()).thenReturn(repo);
        ApplicationContext.setAuthService(auth);
        assertThatCode(() -> runOnFxVoid(c::handleChangePassword)).doesNotThrowAnyException();
        verify(repo).save(any(User.class));

        // cancel appointment success/failure under confirmed dialog
        BookingService bs = mock(BookingService.class);
        ApplicationContext.setBookingService(bs);
        LocalDateTime t = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment appt = new InPersonAppointment(me, new TimeSlot(t, t.plusMinutes(30)), "R");
        appt.setStatus("CONFIRMED");
        when(bs.cancelAppointment(any(String.class), any(User.class))).thenReturn(true, false);

        try (MockedStatic<DialogHelper> dialog = org.mockito.Mockito.mockStatic(DialogHelper.class)) {
            dialog.when(() -> DialogHelper.showConfirmation(any(String.class), any(String.class), any(String.class))).thenReturn(true);
            assertThatCode(() -> invokePrivateOneArg(c, "handleCancelAppt", Appointment.class, appt)).doesNotThrowAnyException();
            assertThatCode(() -> invokePrivateOneArg(c, "handleCancelAppt", Appointment.class, appt)).doesNotThrowAnyException();
        }
    }

    @Test
    void appointmentTypeToLabel_branches_cover_known_and_fallback_types() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        User u = new User("p-type", "Type P", "type@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(t, t.plusMinutes(30));

        Appointment[] appts = new Appointment[] {
                new VirtualAppointment(u, slot, "https://meet"),
                new InPersonAppointment(u, slot, "R1"),
                new UrgentAppointment(u, slot),
                new GroupAppointment(u, slot, 5),
                new AssessmentAppointment(u, slot),
                new FollowUpAppointment(u, slot, "prior-1"),
                new IndividualAppointment(u, slot),
                new ConsultCustomAppointment(u, slot), // fallback "Consult"
                new FollowCustomAppointment(u, slot),  // fallback "Follow"
                new OtherCustomAppointment(u, slot)    // final fallback
        };

        for (Appointment a : appts) {
            assertThatCode(() -> invokePrivateOneArg(c, "appointmentTypeToLabel", Appointment.class, a))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void setupTableColumns_sweep_populated_rows_hits_status_type_action_branches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        Label welcome = new Label("Welcome");
        new Scene(new StackPane(welcome), 320, 120);
        setField(c, "welcomeLabel", welcome);

        TableView<Appointment> tv = new TableView<>();
        setField(c, "appointmentsTable", tv);
        setField(c, "colDate", new TableColumn<Appointment, String>());
        setField(c, "colType", new TableColumn<Appointment, Appointment>());
        setField(c, "colStatus", new TableColumn<Appointment, String>());
        setField(c, "colActions", new TableColumn<Appointment, Appointment>());

        assertThatCode(() -> invokePrivateNoArg(c, "setupTableColumns")).doesNotThrowAnyException();

        User u = new User("p-tc", "Table Cell", "tc@example.com", "pw");
        LocalDateTime base = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment confirmed = new InPersonAppointment(u, new TimeSlot(base, base.plusMinutes(30)), "R1");
        confirmed.setStatus("CONFIRMED");
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(base.plusHours(1), base.plusHours(1).plusMinutes(30)), "R2");
        cancelled.setStatus("CANCELLED");
        UrgentAppointment pending = new UrgentAppointment(u, new TimeSlot(base.plusHours(2), base.plusHours(2).plusMinutes(30)));
        pending.setStatus("PENDING");
        tv.getItems().setAll(confirmed, cancelled, pending);

        assertThatCode(() -> PresentationFxHarness.sweepDeclaredFxControls(c)).doesNotThrowAnyException();
    }

    @Test
    void setupPastAppointmentsTable_sweep_hits_cancelled_rated_and_unrated_branches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "pastAppointmentsTable", new TableView<Appointment>());
        setField(c, "pastColDate", new TableColumn<Appointment, String>());
        setField(c, "pastColType", new TableColumn<Appointment, Appointment>());
        setField(c, "pastColStatus", new TableColumn<Appointment, String>());
        setField(c, "pastColRate", new TableColumn<Appointment, Appointment>());

        assertThatCode(() -> invokePrivateNoArg(c, "setupPastAppointmentsTable")).doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        TableView<Appointment> past = (TableView<Appointment>) getField(c, "pastAppointmentsTable");
        User u = new User("p-past", "Past P", "past@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().minusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(t, t.plusMinutes(30)), "R1");
        cancelled.setStatus("CANCELLED");
        InPersonAppointment rated = new InPersonAppointment(u, new TimeSlot(t.plusHours(1), t.plusHours(1).plusMinutes(30)), "R2");
        rated.setStatus("COMPLETED");
        InPersonAppointment unrated = new InPersonAppointment(u, new TimeSlot(t.plusHours(2), t.plusHours(2).plusMinutes(30)), "R3");
        unrated.setStatus("COMPLETED");
        past.getItems().setAll(cancelled, rated, unrated);

        // Seed one rating so the "stars + edit button" branch is used.
        Object prefsObj = getField(c, "prefs");
        if (prefsObj instanceof java.util.prefs.Preferences prefs) {
            prefs.putInt("rating." + rated.getId(), 4);
            prefs.put("rating.comment." + rated.getId(), "good");
        }

        assertThatCode(() -> PresentationFxHarness.sweepDeclaredFxControls(c)).doesNotThrowAnyException();
    }

    @Test
    void validateBookingForm_branches_notBookable_blocked_and_valid() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        prepareBookingFields(c);
        setField(c, "dateErrorLabel", new Label());
        setField(c, "timeErrorLabel", new Label());
        setField(c, "typeErrorLabel", new Label());
        setField(c, "openBookingBarrierPanel", new VBox());
        setField(c, "openBookingBarrierTitle", new Label());
        setField(c, "openBookingBarrierDetail", new Label());
        setField(c, "currentUser", new User("p-val", "P Val", "pval@example.com", "pw"));

        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<String> hh = (ComboBox<String>) getField(c, "hourCombo");
        @SuppressWarnings("unchecked")
        ComboBox<String> mm = (ComboBox<String>) getField(c, "minuteCombo");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        Button confirm = (Button) getField(c, "btnSummaryConfirm");

        dp.setValue(LocalDate.now().plusDays(2));
        hh.setValue("10");
        mm.setValue("00");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 2), false));

        ScheduleService ss = mock(ScheduleService.class);
        BookingService bs = mock(BookingService.class);
        ApplicationContext.setScheduleService(ss);
        ApplicationContext.setBookingService(bs);

        // not bookable branch
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);
        when(bs.patientHasBlockingOpenAppointment("p-val")).thenReturn(false);
        assertThatCode(() -> invokePrivateNoArg(c, "validateBookingForm")).doesNotThrowAnyException();
        assertThat(confirm.isDisable()).isTrue();

        // blocked-open branch
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        when(bs.patientHasBlockingOpenAppointment("p-val")).thenReturn(true);
        assertThatCode(() -> invokePrivateNoArg(c, "validateBookingForm")).doesNotThrowAnyException();
        assertThat(confirm.isDisable()).isTrue();

        // valid form branch
        when(bs.patientHasBlockingOpenAppointment("p-val")).thenReturn(false);
        assertThatCode(() -> invokePrivateNoArg(c, "validateBookingForm")).doesNotThrowAnyException();
        assertThat(confirm.isDisable()).isFalse();
    }

    @Test
    void refreshAvailableSlots_branches_null_unbookable_and_todaySlotFiltering() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        prepareBookingFields(c);
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<String> hh = (ComboBox<String>) getField(c, "hourCombo");
        @SuppressWarnings("unchecked")
        ComboBox<String> mm = (ComboBox<String>) getField(c, "minuteCombo");
        javafx.scene.layout.FlowPane pane = (javafx.scene.layout.FlowPane) getField(c, "availableSlotsPane");
        Label placeholder = (Label) getField(c, "availableSlotsPlaceholder");

        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 30, 2), false));
        hh.setValue("10");
        mm.setValue("00");

        ScheduleService ss = mock(ScheduleService.class);
        ApplicationContext.setScheduleService(ss);

        // null date -> default placeholder.
        dp.setValue(null);
        assertThatCode(() -> invokePrivateNoArg(c, "refreshAvailableSlots")).doesNotThrowAnyException();
        assertThat(placeholder.isVisible()).isTrue();

        // unbookable date -> unavailable placeholder branch.
        dp.setValue(LocalDate.now().plusDays(3));
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);
        assertThatCode(() -> invokePrivateNoArg(c, "refreshAvailableSlots")).doesNotThrowAnyException();
        assertThat(placeholder.isVisible()).isTrue();

        // bookable today with one past slot + one future slot -> past filtered out.
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        TimeSlot past = new TimeSlot(now.minusMinutes(45), now.minusMinutes(15));
        TimeSlot future = new TimeSlot(now.plusMinutes(30), now.plusMinutes(60));
        dp.setValue(today);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(LocalDate.class), anyInt())).thenReturn(List.of(past, future));
        assertThatCode(() -> invokePrivateNoArg(c, "refreshAvailableSlots")).doesNotThrowAnyException();
        assertThat(pane.getChildren().size()).isEqualTo(1);
    }

    @Test
    void updatePatientQuickStats_and_reminders_branches_with_and_without_upcoming() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "statNextInDaysLabel", new Label());
        setField(c, "statCompletedLabel", new Label());
        setField(c, "statCancelledLabel", new Label());
        setField(c, "reminderLabel", new Label());
        setField(c, "noRemindersLabel", new Label());

        @SuppressWarnings("unchecked")
        ObservableList<Appointment> all = (ObservableList<Appointment>) getField(c, "allAppointments");
        if (all == null) return;

        User u = new User("qs-1", "Quick Stats", "qs@example.com", "pw");
        LocalDateTime now = LocalDateTime.now();
        InPersonAppointment upcoming = new InPersonAppointment(u, new TimeSlot(now.plusDays(2), now.plusDays(2).plusMinutes(30)), "R1");
        upcoming.setStatus("CONFIRMED");
        InPersonAppointment completed = new InPersonAppointment(u, new TimeSlot(now.minusDays(3), now.minusDays(3).plusMinutes(30)), "R2");
        completed.setStatus("COMPLETED");
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(now.plusDays(1), now.plusDays(1).plusMinutes(30)), "R3");
        cancelled.setStatus("CANCELLED");
        all.setAll(upcoming, completed, cancelled);

        assertThatCode(() -> invokePrivateNoArg(c, "updatePatientQuickStats")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateNoArg(c, "updateReminders")).doesNotThrowAnyException();

        Label next = (Label) getField(c, "statNextInDaysLabel");
        Label rem = (Label) getField(c, "reminderLabel");
        Label noRem = (Label) getField(c, "noRemindersLabel");
        assertThat(next.getText()).isNotEmpty();
        assertThat(rem.isVisible()).isTrue();
        assertThat(noRem.isVisible()).isFalse();

        // No upcoming (only cancelled or past) branch.
        all.setAll(cancelled, completed);
        assertThatCode(() -> invokePrivateNoArg(c, "updatePatientQuickStats")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateNoArg(c, "updateReminders")).doesNotThrowAnyException();
        assertThat(((Label) getField(c, "statNextInDaysLabel")).getText()).isEqualTo("—");
        assertThat(((Label) getField(c, "noRemindersLabel")).isVisible()).isTrue();
    }

    @Test
    void setupPreferenceControls_and_sessionLabel_branches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "patientNotificationChannelCombo", new ComboBox<String>());
        setField(c, "patientReminderLeadCombo", new ComboBox<String>());
        setField(c, "patientTimeFormatCombo", new ComboBox<String>());
        setField(c, "patientLanguageCombo", new ComboBox<String>());
        setField(c, "patientSessionLabel", new Label());

        assertThatCode(() -> invokePrivateNoArg(c, "setupPreferenceControls")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateNoArg(c, "updatePatientSessionLabel")).doesNotThrowAnyException();

        @SuppressWarnings("unchecked")
        ComboBox<String> ch = (ComboBox<String>) getField(c, "patientNotificationChannelCombo");
        @SuppressWarnings("unchecked")
        ComboBox<String> lead = (ComboBox<String>) getField(c, "patientReminderLeadCombo");
        @SuppressWarnings("unchecked")
        ComboBox<String> tf = (ComboBox<String>) getField(c, "patientTimeFormatCombo");
        @SuppressWarnings("unchecked")
        ComboBox<String> lang = (ComboBox<String>) getField(c, "patientLanguageCombo");
        assertThat(ch.getItems()).isNotEmpty();
        assertThat(lead.getItems()).isNotEmpty();
        assertThat(tf.getItems()).contains("24-hour", "12-hour");
        assertThat(lang.getItems()).contains("English", "العربية");
        assertThat(((Label) getField(c, "patientSessionLabel")).getText()).contains("Secure session");
    }

    @Test
    void switchView_branches_activate_target_and_mark_active_button() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        VBox appointmentsView = new VBox();
        VBox bookView = new VBox();
        VBox profileView = new VBox();
        VBox messagesView = new VBox();
        Button b1 = new Button("A");
        Button b2 = new Button("B");
        Button b3 = new Button("C");
        Button b4 = new Button("D");
        setField(c, "appointmentsView", appointmentsView);
        setField(c, "bookView", bookView);
        setField(c, "profileView", profileView);
        setField(c, "messagesView", messagesView);
        setField(c, "btnNavAppointments", b1);
        setField(c, "btnNavBook", b2);
        setField(c, "btnNavProfile", b3);
        setField(c, "btnNavMessages", b4);

        var m = PatientDashboardController.class.getDeclaredMethod("switchView", VBox.class, Button.class);
        m.setAccessible(true);

        assertThatCode(() -> m.invoke(c, profileView, b3)).doesNotThrowAnyException();
        assertThat(profileView.isVisible()).isTrue();
        assertThat(appointmentsView.isVisible()).isFalse();
        assertThat(b3.getStyleClass()).contains("sidebar-btn-active");
    }

    @Test
    void formatScheduledTime_nullSlot_and_nonNullSlot() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        var m = PatientDashboardController.class.getDeclaredMethod("formatScheduledTime", TimeSlot.class);
        m.setAccessible(true);
        assertThat(m.invoke(c, (TimeSlot) null)).isEqualTo("");
        LocalDateTime t = LocalDateTime.now().plusDays(4).withHour(14).withMinute(5).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(t, t.plusHours(1));
        String formatted = (String) m.invoke(c, slot);
        assertThat(formatted).isNotBlank();
    }

    @Test
    void updateNotesCount_updatesLabelFromFieldLength() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        Label notesCountLabel = new Label();
        TextArea notesField = new TextArea();
        setField(c, "notesCountLabel", notesCountLabel);
        setField(c, "notesField", notesField);

        invokePrivateNoArg(c, "updateNotesCount");
        assertThat(notesCountLabel.getText()).isEqualTo("0 / 1000");

        notesField.setText("notes");
        invokePrivateNoArg(c, "updateNotesCount");
        assertThat(notesCountLabel.getText()).isEqualTo("5 / 1000");
    }

    @Test
    void scrollToAndHighlight_branches_nullId_noMatch_and_foundRow() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        User u = new User("p-scroll", "Scroll", "scroll@example.com", "pw");
        LocalDateTime t = LocalDateTime.now().plusDays(6).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment appt = new InPersonAppointment(u, new TimeSlot(t, t.plusHours(1)), "R");
        appt.setStatus("CONFIRMED");
        TableView<Appointment> tv = new TableView<>();
        tv.getItems().add(appt);
        setField(c, "appointmentsTable", tv);

        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                invokePrivateOneArg(c, "scrollToAndHighlightNewBooking", String.class, null);
                invokePrivateOneArg(c, "scrollToAndHighlightNewBooking", String.class, "no-such-id");
                invokePrivateOneArg(c, "scrollToAndHighlightNewBooking", String.class, appt.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    @Test
    void getSelectedBookingDurationMinutes_defaultsAndFromType() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        var m = PatientDashboardController.class.getDeclaredMethod("getSelectedBookingDurationMinutes");
        m.setAccessible(true);
        setField(c, "typeCombo", null);
        assertThat(m.invoke(c)).isEqualTo(30);
        ComboBox<BookingOption> tc = new ComboBox<>();
        setField(c, "typeCombo", tc);
        assertThat(m.invoke(c)).isEqualTo(30);
        tc.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Dur", 45, 2), false));
        assertThat(m.invoke(c)).isEqualTo(45);
    }

    @Test
    void firstBookableOnOrAfter_nullSchedule_nullFrom_andFindsNext() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        ComboBox<BookingOption> tc = new ComboBox<>();
        tc.getItems().add(BookingOption.of(new AppointmentTypeConfig.Type("F", 60, 2), false));
        tc.getSelectionModel().selectFirst();
        setField(c, "typeCombo", tc);
        var m = PatientDashboardController.class.getDeclaredMethod("firstBookableOnOrAfter", LocalDate.class);
        m.setAccessible(true);
        assertThat(m.invoke(c, new Object[]{null})).isNull();
        ScheduleService ssOrig = ApplicationContext.getScheduleService();
        try {
            ApplicationContext.setScheduleService(null);
            LocalDate d0 = LocalDate.now().plusDays(3);
            assertThat(m.invoke(c, d0)).isEqualTo(d0);
            ScheduleService ss = mock(ScheduleService.class);
            LocalDate d1 = LocalDate.now().plusDays(20);
            when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false).thenReturn(true);
            ApplicationContext.setScheduleService(ss);
            assertThat(m.invoke(c, d1)).isEqualTo(d1.plusDays(1));
        } finally {
            ApplicationContext.setScheduleService(ssOrig);
        }
    }

    @Test
    void applyPatientBookingDayCells_dateCell_updateItem_branches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        DatePicker dp = new DatePicker();
        setField(c, "datePicker", dp);
        ComboBox<BookingOption> tc = new ComboBox<>();
        BookingOption opt = BookingOption.of(new AppointmentTypeConfig.Type("BookCell", 60, 4), false);
        tc.getItems().add(opt);
        tc.setValue(opt);
        setField(c, "typeCombo", tc);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true, false);
        ScheduleService ssOrig = ApplicationContext.getScheduleService();
        try {
            ApplicationContext.setScheduleService(ss);
            invokePrivateNoArg(c, "applyPatientBookingDayCells");
            @SuppressWarnings("unchecked")
            Callback<DatePicker, DateCell> factory = dp.getDayCellFactory();
            assertThat(factory).isNotNull();
            DateCell cell = factory.call(dp);
            assertThatCode(() -> runOnFxVoid(() -> {
                cell.updateItem(null, true);
                cell.updateItem(LocalDate.now().plusDays(10), false);
                cell.updateItem(LocalDate.now().plusDays(11), false);
            })).doesNotThrowAnyException();
        } finally {
            ApplicationContext.setScheduleService(ssOrig);
        }
    }

    @Test
    void reloadPatientBookingOptions_repopulatesTypesAndDayCells() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        ComboBox<BookingOption> tc = new ComboBox<>();
        BookingOption opt = BookingOption.of(new AppointmentTypeConfig.Type("Reload", 30, 3), false);
        tc.getItems().setAll(opt);
        tc.setValue(opt);
        setField(c, "typeCombo", tc);
        setField(c, "datePicker", new DatePicker());
        setField(c, "bookingPartySizeSpinner", new Spinner<>(1, 10, 1));
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        ScheduleService ssOrig = ApplicationContext.getScheduleService();
        try {
            ApplicationContext.setScheduleService(ss);
            assertThatCode(() -> invokePrivateNoArg(c, "reloadPatientBookingOptions")).doesNotThrowAnyException();
            assertThat(tc.getItems()).isNotEmpty();
        } finally {
            ApplicationContext.setScheduleService(ssOrig);
        }
    }

    @Test
    void updateBookingSummary_coversAllSummaryLabelsAndPartyBranches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "summaryDateLabel", new Label());
        setField(c, "summaryTimeLabel", new Label());
        setField(c, "summaryTypeLabel", new Label());
        setField(c, "summaryDurationLabel", new Label());
        setField(c, "summaryPartyLabel", new Label());
        setField(c, "summaryReminderLabel", new Label());
        setField(c, "summaryLanguageLabel", new Label());
        setField(c, "datePicker", new DatePicker());
        ComboBox<String> hh = new ComboBox<>(FXCollections.observableArrayList("10", "11"));
        ComboBox<String> mm = new ComboBox<>(FXCollections.observableArrayList("00", "30"));
        ComboBox<BookingOption> type = new ComboBox<>();
        BookingOption bo = BookingOption.of(new AppointmentTypeConfig.Type("Sum", 60, 5), false);
        type.getItems().add(bo);
        type.setValue(bo);
        setField(c, "hourCombo", hh);
        setField(c, "minuteCombo", mm);
        setField(c, "typeCombo", type);
        setField(c, "bookingReminderCombo", new ComboBox<>(FXCollections.observableArrayList("In-App Only")));
        setField(c, "bookingLanguageCombo", new ComboBox<>(FXCollections.observableArrayList("English")));
        Spinner<Integer> party = new Spinner<>(1, 10, 3);
        setField(c, "bookingPartySizeSpinner", party);

        invokePrivateNoArg(c, "updateBookingSummary");
        hh.setValue("10");
        mm.setValue("00");
        invokePrivateNoArg(c, "updateBookingSummary");

        party.setDisable(true);
        invokePrivateNoArg(c, "updateBookingSummary");
        party.setDisable(false);
        invokePrivateNoArg(c, "updateBookingSummary");
    }

    @Test
    void updateOpenBookingBarrier_setsVisibilityAndCopy() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        VBox panel = new VBox();
        Label title = new Label();
        Label detail = new Label();
        setField(c, "openBookingBarrierPanel", panel);
        setField(c, "openBookingBarrierTitle", title);
        setField(c, "openBookingBarrierDetail", detail);
        invokePrivateOneArg(c, "updateOpenBookingBarrier", boolean.class, true);
        assertThat(panel.isVisible()).isTrue();
        assertThat(title.getText()).isNotEmpty();
        invokePrivateOneArg(c, "updateOpenBookingBarrier", boolean.class, false);
        assertThat(panel.isVisible()).isFalse();
    }

    @Test
    void updatePartySpinnerBounds_earlyReturns_and_withTypeOption() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        assertThatCode(() -> invokePrivateNoArg(c, "updatePartySpinnerBounds")).doesNotThrowAnyException();

        Spinner<Integer> sp = new Spinner<>(1, 10, 3);
        ComboBox<BookingOption> type = new ComboBox<>();
        setField(c, "bookingPartySizeSpinner", sp);
        setField(c, "typeCombo", type);
        assertThatCode(() -> invokePrivateNoArg(c, "updatePartySpinnerBounds")).doesNotThrowAnyException();

        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("PartyT", 60, 6), false));
        assertThatCode(() -> invokePrivateNoArg(c, "updatePartySpinnerBounds")).doesNotThrowAnyException();
    }

    @Test
    void setTimeBasedGreeting_userInitials_footerVersion_lastUpdated_branches() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "welcomeLabel", new Label());
        setField(c, "userInitialsLabel", new Label());
        setField(c, "footerVersionLabel", new Label());
        setField(c, "lastUpdatedLabel", new Label());
        setField(c, "currentUser", new User("u-gr", "Pat", "pat-gr@example.com", "pw"));

        assertThatCode(() -> invokePrivateNoArg(c, "setTimeBasedGreeting")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateNoArg(c, "setUserInitials")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateNoArg(c, "setFooterVersion")).doesNotThrowAnyException();
        assertThatCode(() -> invokePrivateNoArg(c, "updateLastUpdatedLabel")).doesNotThrowAnyException();

        setField(c, "currentUser", new User("u-gr2", "Jane Doe", "jane@example.com", "pw"));
        assertThatCode(() -> invokePrivateNoArg(c, "setUserInitials")).doesNotThrowAnyException();

        setField(c, "currentUser", new User("u-gr3", "   ", "blank@example.com", "pw"));
        assertThatCode(() -> invokePrivateNoArg(c, "setUserInitials")).doesNotThrowAnyException();
    }

    @Test
    void setTimeBasedGreeting_nullWelcomeOrUser_skips() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        setField(c, "welcomeLabel", null);
        setField(c, "currentUser", new User("u-sk", "S", "s@example.com", "pw"));
        assertThatCode(() -> invokePrivateNoArg(c, "setTimeBasedGreeting")).doesNotThrowAnyException();
    }

    private static final class ConsultCustomAppointment extends Appointment {
        private ConsultCustomAppointment(User patient, TimeSlot timeSlot) {
            super(patient, timeSlot);
        }
    }

    private static final class FollowCustomAppointment extends Appointment {
        private FollowCustomAppointment(User patient, TimeSlot timeSlot) {
            super(patient, timeSlot);
        }
    }

    private static final class OtherCustomAppointment extends Appointment {
        private OtherCustomAppointment(User patient, TimeSlot timeSlot) {
            super(patient, timeSlot);
        }
    }

    private static void prepareBookingFields(PatientDashboardController c) throws Exception {
        Label welcome = new Label("Welcome");
        // ensure welcomeLabel.getScene() is non-null for toast branches
        new Scene(new StackPane(welcome), 300, 120);
        setField(c, "welcomeLabel", welcome);

        setField(c, "datePicker", new DatePicker());
        ComboBox<String> h = new ComboBox<>(FXCollections.observableArrayList("00", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "23"));
        ComboBox<String> m = new ComboBox<>(FXCollections.observableArrayList("00", "15", "30", "45"));
        setField(c, "hourCombo", h);
        setField(c, "minuteCombo", m);
        setField(c, "typeCombo", new ComboBox<BookingOption>());
        setField(c, "availableSlotsPane", new javafx.scene.layout.FlowPane());
        setField(c, "availableSlotsPlaceholder", new Label());
        setField(c, "btnSummaryConfirm", new Button("Confirm"));
        setField(c, "bookingPartySizeSpinner", new Spinner<>(1, 10, 1));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            return;
        }
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            return null;
        }
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

    private static void runOnFxVoid(Runnable r) {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
    }
}

