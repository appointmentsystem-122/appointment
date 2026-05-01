package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AppointmentTypeConfig;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.BookingFailureCodes;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.application.BookingService;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;

/**
 * Targeted branch tests for {@link BookAppointmentController#handleBook()}.
 */
@ResourceLock("ApplicationContextServices")
class BookAppointmentControllerTargetedBranchesTest {

    private ScheduleService originalScheduleService;
    private BookingService originalBookingService;
    private AuthService originalAuthService;

    @BeforeEach
    void init() {
        JavaFxTestSupport.initPlatform();
        System.setProperty("app.test.autoDialogs", "true");
        originalScheduleService = ApplicationContext.getScheduleService();
        originalBookingService = ApplicationContext.getBookingService();
        originalAuthService = ApplicationContext.getAuthService();
    }

    @AfterEach
    void restore() {
        ApplicationContext.setScheduleService(originalScheduleService);
        ApplicationContext.setBookingService(originalBookingService);
        ApplicationContext.setAuthService(originalAuthService);
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void handleBook_branch_unavailable_whenTypeMissing() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(false, Optional.of("unused"));

        // opt == null branch
        ((DatePicker) getField(c, "datePicker")).setValue(LocalDate.now().plusDays(2));
        assertThatCode(c::handleBook).doesNotThrowAnyException();
    }

    @Test
    void handleConfirmBooking_delegatesToHandleBook() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(false, Optional.of("unused"));
        ((DatePicker) getField(c, "datePicker")).setValue(LocalDate.now().plusDays(2));
        assertThatCode(c::handleConfirmBooking).doesNotThrowAnyException();
    }

    @Test
    void handleBook_branch_error_whenSlotMissing() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.of("unused"));

        setValidDateAndType(c);
        timeSlotCombo(c).setValue(null);
        assertThatCode(c::handleBook).doesNotThrowAnyException();
    }

    @Test
    void handleBook_branch_blockedOpenAppointment() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.of("unused"));

        setField(c, "currentUser", new User("u-block", "U", "u-block@example.com", "pw"));
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());

        BookingService bs = ApplicationContext.getBookingService();
        when(bs.patientHasBlockingOpenAppointment(anyString())).thenReturn(true);

        assertThatCode(c::handleBook).doesNotThrowAnyException();
    }

    @Test
    void handleBook_branch_fail_openAppointmentCode() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.of(BookingFailureCodes.OPEN_APPOINTMENT_NOT_COMPLETED));
        setField(c, "currentUser", new User("u-open", "U", "u-open@example.com", "pw"));

        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        assertThatCode(c::handleBook).doesNotThrowAnyException();
    }

    @Test
    void handleBook_branch_fail_otherReason() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.of("SOME_OTHER_FAILURE"));
        setField(c, "currentUser", new User("u-fail", "U", "u-fail@example.com", "pw"));

        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        assertThatCode(c::handleBook).doesNotThrowAnyException();
    }

    @Test
    void handleBook_branch_dayNotBookable_returnsEarly() throws Exception {
        BookAppointmentController c = freshController();
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);
        ApplicationContext.setScheduleService(ss);
        BookingService bs = mock(BookingService.class);
        when(bs.patientHasBlockingOpenAppointment(anyString())).thenReturn(false);
        ApplicationContext.setBookingService(bs);
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        assertThatCode(c::handleBook).doesNotThrowAnyException();
    }

    @Test
    void handleBook_branch_nullDate_returnsEarly_withoutTryBook() throws Exception {
        BookAppointmentController c = freshController();
        BookingService bs = mock(BookingService.class);
        when(bs.patientHasBlockingOpenAppointment(anyString())).thenReturn(false);
        when(bs.tryBookWithReason(any(), any())).thenReturn(Optional.of("unused"));
        ApplicationContext.setBookingService(bs);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        ApplicationContext.setScheduleService(ss);

        setValidDateAndType(c);
        ((DatePicker) getField(c, "datePicker")).setValue(null);
        timeSlotCombo(c).setValue(sampleSlot());
        assertThatCode(c::handleBook).doesNotThrowAnyException();
        verify(bs, never()).tryBookWithReason(any(), any());
    }

    @Test
    void loadTimeSlotsAsync_succeeded_nonEmptySlots_populatesComboAndClearsErrorStyle() throws Exception {
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        LocalDate d = LocalDate.now().plusDays(4);
        TimeSlot slot = new TimeSlot(d.atTime(10, 0), d.atTime(11, 0));
        when(ss.getAvailableSlots(any(LocalDate.class), anyInt())).thenReturn(List.of(slot));
        ApplicationContext.setScheduleService(ss);

        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        Label msg = (Label) getField(c, "messageLabel");

        runOnFxVoid(() -> {
            try {
                ((DatePicker) getField(c, "datePicker")).setValue(d);
                @SuppressWarnings("unchecked")
                ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
                type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 4), false));
                invokePrivateOneArg(c, "loadTimeSlotsAsync", LocalDate.class, d);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(500);
        waitForFx();
        @SuppressWarnings("unchecked")
        ComboBox<TimeSlot> slots = (ComboBox<TimeSlot>) getField(c, "timeSlotCombo");
        assertThat(slots.getItems()).isNotEmpty();
        assertThat(msg.getStyleClass()).doesNotContain("error-label");
    }

    @Test
    void loadTimeSlotsAsync_succeeded_emptySlots_setsUnavailableMessage() throws Exception {
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(LocalDate.class), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        Label msg = (Label) getField(c, "messageLabel");
        LocalDate d = LocalDate.now().plusDays(3);
        runOnFxVoid(() -> {
            try {
                ((DatePicker) getField(c, "datePicker")).setValue(d);
                @SuppressWarnings("unchecked")
                ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
                type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 4), false));
                invokePrivateOneArg(c, "loadTimeSlotsAsync", LocalDate.class, d);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(400);
        waitForFx();
        assertThat(msg.getText()).isNotBlank();
    }

    @Test
    void loadTimeSlotsAsync_failed_invokesFailedHandler() throws Exception {
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(LocalDate.class), anyInt())).thenThrow(new RuntimeException("slots failed"));
        ApplicationContext.setScheduleService(ss);
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        LocalDate d = LocalDate.now().plusDays(3);
        runOnFxVoid(() -> {
            try {
                ((DatePicker) getField(c, "datePicker")).setValue(d);
                @SuppressWarnings("unchecked")
                ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
                type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 4), false));
                invokePrivateOneArg(c, "loadTimeSlotsAsync", LocalDate.class, d);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(400);
        waitForFx();
        @SuppressWarnings("unchecked")
        ComboBox<TimeSlot> slots = (ComboBox<TimeSlot>) getField(c, "timeSlotCombo");
        assertThat(slots.isDisabled()).isFalse();
    }

    @Test
    void handleBack_adminUser_loadsAdminDashboard() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "currentUser", new Administrator("a-hb", "Admin", "a-hb@example.com", "pw"));
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            // Invoke on the test thread so MockedStatic intercepts MainApp.loadScreen (FX thread would bypass it).
            assertThatCode(c::handleBack).doesNotThrowAnyException();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_ADMIN_DASHBOARD), anyString()));
        }
    }

    @Test
    void handleBack_patientUser_loadsPatientDashboard() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "currentUser", new User("p-hb", "Pat", "p-hb@example.com", "pw"));
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(c::handleBack).doesNotThrowAnyException();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_PATIENT_DASHBOARD), anyString()));
        }
    }

    @Test
    void showLoadingState_trueAndFalse_coversBranches() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        Method m = BookAppointmentController.class.getDeclaredMethod("showLoadingState", boolean.class);
        m.setAccessible(true);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                m.invoke(c, true);
                m.invoke(c, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    @Test
    void showLoadingState_trueWithNullConfirmButton_skipsButtonDisable() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", null);
        Method m = BookAppointmentController.class.getDeclaredMethod("showLoadingState", boolean.class);
        m.setAccessible(true);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                m.invoke(c, true);
                m.invoke(c, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    /**
     * {@code typeCombo} listener: date set but {@code !isDateBookable(d, newDuration)} → {@code onBookingDateChanged}
     * (not {@code loadTimeSlotsAsync}).
     */
    @Test
    void initialize_typeChange_whenDateNotBookableForNewDuration_skipsLoadAsync() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-tc", "T", "u-tc@example.com", "pw"));
        ApplicationContext.setAuthService(auth);

        LocalDate target = LocalDate.now().plusDays(12);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(), anyInt())).thenAnswer(inv -> {
            LocalDate d = inv.getArgument(0);
            int dur = inv.getArgument(1);
            if (target.equals(d) && dur == 90) {
                return false;
            }
            return true;
        });
        when(ss.getAvailableSlots(any(), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);

        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        runOnFxVoid(() -> {
            try {
                DatePicker dp = (DatePicker) getField(c, "datePicker");
                dp.setValue(target);
                @SuppressWarnings("unchecked")
                ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
                type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("LongSession", 90, 4), false));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        waitForFx();

        verify(ss, atLeastOnce()).isDateBookable(eq(target), eq(90));
        verify(ss, never()).getAvailableSlots(eq(target), eq(90));
    }

    @Test
    void handleLogout_nullMessageLabel_passesNullOwner() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "messageLabel", null);
        setField(c, "currentUser", new User("u-lo", "L", "u-lo@example.com", "pw"));
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.performLogout(any(), any(User.class))).thenAnswer(inv -> null);
            assertThatCode(c::handleLogout).doesNotThrowAnyException();
            main.verify(() -> MainApp.performLogout(isNull(), any(User.class)));
        }
    }

    @Test
    void initialize_notesListener_trimsTextBeyond1000Chars() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-n", "N", "u-n@example.com", "pw"));
        ApplicationContext.setAuthService(auth);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(LocalDate.class), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);

        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        setField(c, "partySizeSpinner", new Spinner<>(1, 10, 1));
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();

        TextArea notes = (TextArea) getField(c, "notesField");
        assertThatCode(() -> runOnFxVoid(() -> notes.setText("y".repeat(1001)))).doesNotThrowAnyException();
        assertThat(notes.getText().length()).isEqualTo(1000);
    }

    @Test
    void handleBook_branch_success_callsBackNavigation() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.empty());
        setField(c, "currentUser", new User("u-ok", "U", "u-ok@example.com", "pw"));

        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(c::handleBook).doesNotThrowAnyException();
            main.verify(() -> MainApp.loadScreen(org.mockito.ArgumentMatchers.eq(ScreenConstants.FXML_PATIENT_DASHBOARD), anyString()));
        }
    }

    @Test
    void selectedDurationMinutes_nullTypeUses60() throws Exception {
        BookAppointmentController c = freshController();
        var m = BookAppointmentController.class.getDeclaredMethod("selectedDurationMinutes");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        assertThat(m.invoke(c)).isEqualTo(60);
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("D", 45, 3), false));
        assertThat(m.invoke(c)).isEqualTo(45);
    }

    @Test
    void refreshPartySpinnerForType_hitsMaxFromOptionOrDefault() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "partySizeSpinner", new Spinner<>(1, 20, 2));
        var m = BookAppointmentController.class.getDeclaredMethod("refreshPartySpinnerForType");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                m.invoke(c);
                type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("P", 60, 8), false));
                m.invoke(c);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    @Test
    void applyBookScreenDayCells_dateCell_empty_null_bookable_notBookable() throws Exception {
        BookAppointmentController c = freshController();
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("DC", 60, 4), false));
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true, false);
        ApplicationContext.setScheduleService(ss);
        invokePrivateNoArg(c, "applyBookScreenDayCells");
        Callback<DatePicker, DateCell> factory = dp.getDayCellFactory();
        assertThat(factory).isNotNull();
        DateCell cell = factory.call(dp);
        assertThatCode(() -> runOnFxVoid(() -> {
            cell.updateItem(null, true);
            cell.updateItem(LocalDate.now().plusDays(20), false);
            cell.updateItem(LocalDate.now().plusDays(21), false);
        })).doesNotThrowAnyException();
        type.setValue(null);
        invokePrivateNoArg(c, "applyBookScreenDayCells");
        Callback<DatePicker, DateCell> factoryNullType = dp.getDayCellFactory();
        DateCell cell2 = factoryNullType.call(dp);
        assertThatCode(() -> runOnFxVoid(() -> cell2.updateItem(LocalDate.now().plusDays(22), false)))
                .doesNotThrowAnyException();
    }

    @Test
    void onBookingDateChanged_null_clearsSlots_and_unavailable_clearsAfterRunLater() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<TimeSlot> slots = (ComboBox<TimeSlot>) getField(c, "timeSlotCombo");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("OD", 60, 4), false));
        slots.getItems().add(sampleSlot());
        slots.setValue(sampleSlot());

        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);
        ApplicationContext.setScheduleService(ss);
        dp.setValue(LocalDate.now().plusDays(30));
        assertThatCode(() -> invokePrivateOneArg(c, "onBookingDateChanged", LocalDate.class, dp.getValue()))
                .doesNotThrowAnyException();
        waitForFx();
        assertThat(dp.getValue()).isNull();

        assertThatCode(() -> invokePrivateOneArg(c, "onBookingDateChanged", LocalDate.class, (LocalDate) null))
                .doesNotThrowAnyException();
    }

    @Test
    void showMessage_errorAndSuccess_coversStyleBranches() throws Exception {
        BookAppointmentController c = freshController();
        Label lbl = new Label();
        setField(c, "messageLabel", lbl);
        Method m = BookAppointmentController.class.getDeclaredMethod("showMessage", String.class, boolean.class);
        m.setAccessible(true);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                m.invoke(c, "e", true);
                m.invoke(c, "s", false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
        assertThat(lbl.getText()).isEqualTo("s");
        assertThat(lbl.getStyleClass()).contains("success-label");
    }

    @Test
    void validateBookingForm_nullConfirmButton_skipsDisableBranch() throws Exception {
        BookAppointmentController c = freshController();
        assertThat(getField(c, "btnConfirmBooking")).isNull();
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        assertThatCode(() -> invokePrivateNoArg(c, "validateBookingForm")).doesNotThrowAnyException();
    }

    @Test
    void validateBookingForm_blockedOpen_disablesConfirm() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        setField(c, "currentUser", new User("u-vbf", "V", "u-vbf@example.com", "pw"));
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        BookingService bs = mock(BookingService.class);
        when(bs.patientHasBlockingOpenAppointment("u-vbf")).thenReturn(true);
        ApplicationContext.setBookingService(bs);
        assertThatCode(() -> invokePrivateNoArg(c, "validateBookingForm")).doesNotThrowAnyException();
        assertThat(((Button) getField(c, "btnConfirmBooking")).isDisable()).isTrue();
    }

    @Test
    void initialize_messageLabelInScene_registersLogoutAccelerator() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-acc", "A", "u-acc@example.com", "pw"));
        ApplicationContext.setAuthService(auth);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);

        BookAppointmentController c = freshController();
        Label msg = (Label) getField(c, "messageLabel");
        setField(c, "btnConfirmBooking", new Button());
        runOnFxVoid(() -> {
            StackPane root = new StackPane(msg);
            new Scene(root, 120, 120);
        });
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
        waitForFx();
        runOnFxVoid(() -> {
            assertThat(msg.getScene()).isNotNull();
            assertThat(msg.getScene().getAccelerators()).isNotEmpty();
        });
    }

    @Test
    void initialize_typeComboListener_nullType_usesSixtyMinuteDurationForSlots() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-clr", "C", "u-clr@example.com", "pw"));
        ApplicationContext.setAuthService(auth);
        LocalDate d = LocalDate.now().plusDays(22);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        TimeSlot slot = new TimeSlot(d.atTime(14, 0), d.atTime(15, 0));
        when(ss.getAvailableSlots(eq(d), eq(60))).thenReturn(List.of(slot));
        ApplicationContext.setScheduleService(ss);

        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        runOnFxVoid(() -> {
            try {
                ((DatePicker) getField(c, "datePicker")).setValue(d);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        waitForFx();
        Thread.sleep(500);
        runOnFxVoid(() -> {
            try {
                @SuppressWarnings("unchecked")
                ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
                type.setValue(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(ss, timeout(12_000).atLeastOnce()).getAvailableSlots(eq(d), eq(60));
    }

    @Test
    void handleBook_partySpinnerDisabled_usesDefaultPartyAndSucceeds() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.empty());
        setField(c, "currentUser", new User("u-pd", "P", "u-pd@example.com", "pw"));
        Spinner<Integer> spin = new Spinner<>(1, 10, 5);
        spin.setDisable(true);
        setField(c, "partySizeSpinner", spin);
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(c::handleBook).doesNotThrowAnyException();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_PATIENT_DASHBOARD), anyString()));
        }
    }

    @Test
    void initialize_notesFieldNull_skipsLengthLimiter() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-nf", "N", "u-nf@example.com", "pw"));
        ApplicationContext.setAuthService(auth);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);
        BookAppointmentController c = freshController();
        setField(c, "notesField", null);
        setField(c, "btnConfirmBooking", new Button());
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    @Test
    void initialize_messageLabelWithoutScene_skipsAcceleratorRegistration() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-ms", "M", "u-ms@example.com", "pw"));
        ApplicationContext.setAuthService(auth);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);
        BookAppointmentController c = freshController();
        Label msg = new Label();
        setField(c, "messageLabel", msg);
        setField(c, "btnConfirmBooking", new Button());
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
        waitForFx();
        assertThat(msg.getScene()).isNull();
    }

    @Test
    void handleBack_nullCurrentUser_loadsPatientDashboard() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "currentUser", null);
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(c::handleBack).doesNotThrowAnyException();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_PATIENT_DASHBOARD), anyString()));
        }
    }

    @Test
    void handleLogout_messageLabelWithoutScene_passesNullOwner() throws Exception {
        BookAppointmentController c = freshController();
        Label msg = new Label();
        setField(c, "messageLabel", msg);
        setField(c, "currentUser", new User("u-lo2", "L", "u-lo2@example.com", "pw"));
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.performLogout(any(), any(User.class))).thenAnswer(inv -> null);
            assertThatCode(c::handleLogout).doesNotThrowAnyException();
            main.verify(() -> MainApp.performLogout(isNull(), any(User.class)));
        }
    }

    @Test
    void validateBookingForm_allInputsValid_enablesConfirm() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        setField(c, "currentUser", new User("u-vbf2", "V", "u-vbf2@example.com", "pw"));
        mockCommonServices(true, Optional.of("unused"));
        BookingService bs = ApplicationContext.getBookingService();
        when(bs.patientHasBlockingOpenAppointment("u-vbf2")).thenReturn(false);
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        assertThatCode(() -> invokePrivateNoArg(c, "validateBookingForm")).doesNotThrowAnyException();
        assertThat(((Button) getField(c, "btnConfirmBooking")).isDisabled()).isFalse();
    }

    @Test
    void handleBook_nullOptionalFields_stillBooks() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.empty());
        setField(c, "currentUser", new User("u-nul", "N", "u-nul@example.com", "pw"));
        setField(c, "notesField", null);
        setField(c, "contactPhoneField", null);
        setField(c, "reminderCombo", null);
        setField(c, "accessibilityField", null);
        setField(c, "languageCombo", null);
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(c::handleBook).doesNotThrowAnyException();
        }
    }

    @Test
    void handleBook_partySpinnerEnabled_usesSpinnerParticipantCount() throws Exception {
        BookAppointmentController c = freshController();
        mockCommonServices(true, Optional.empty());
        setField(c, "currentUser", new User("u-par", "P", "u-par@example.com", "pw"));
        Spinner<Integer> spin = new Spinner<>(1, 10, 3);
        spin.setDisable(false);
        setField(c, "partySizeSpinner", spin);
        setValidDateAndType(c);
        timeSlotCombo(c).setValue(sampleSlot());
        BookingService bs = ApplicationContext.getBookingService();
        ArgumentCaptor<Appointment> appt = ArgumentCaptor.forClass(Appointment.class);
        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(c::handleBook).doesNotThrowAnyException();
            verify(bs).tryBookWithReason(appt.capture(), isNull());
            assertThat(appt.getValue().getParticipantCount()).isEqualTo(3);
        }
    }

    @Test
    void initialize_notesListener_nullNewText_doesNotTrim() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-nn", "N", "u-nn@example.com", "pw"));
        ApplicationContext.setAuthService(auth);
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                c.initialize();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
        TextArea notes = (TextArea) getField(c, "notesField");
        assertThatCode(() -> runOnFxVoid(() -> notes.setText(null))).doesNotThrowAnyException();
    }

    @Test
    void applyBookScreenDayCells_updateItem_nullDateNotEmpty_disablesCell() throws Exception {
        BookAppointmentController c = freshController();
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("ND", 60, 4), false));
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        ApplicationContext.setScheduleService(ss);
        invokePrivateNoArg(c, "applyBookScreenDayCells");
        Callback<DatePicker, DateCell> factory = dp.getDayCellFactory();
        DateCell cell = factory.call(dp);
        assertThatCode(() -> runOnFxVoid(() -> cell.updateItem(null, false))).doesNotThrowAnyException();
    }

    @Test
    void onBookingDateChanged_runLater_keepsDateWhenDayBecomesBookable() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        @SuppressWarnings("unchecked")
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        type.setValue(BookingOption.of(new AppointmentTypeConfig.Type("RB", 60, 4), false));
        LocalDate d = LocalDate.now().plusDays(40);
        AtomicInteger bookableCalls = new AtomicInteger();
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenAnswer(inv -> bookableCalls.getAndIncrement() == 0 ? false : true);
        ApplicationContext.setScheduleService(ss);
        dp.setValue(d);
        assertThatCode(() -> invokePrivateOneArg(c, "onBookingDateChanged", LocalDate.class, d))
                .doesNotThrowAnyException();
        waitForFx();
        assertThat(dp.getValue()).isEqualTo(d);
    }

    @Test
    void handleDateSelection_triggersSlotLoadPath() throws Exception {
        BookAppointmentController c = freshController();
        setField(c, "btnConfirmBooking", new Button());
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(), anyInt())).thenReturn(true);
        when(ss.getAvailableSlots(any(), anyInt())).thenReturn(Collections.emptyList());
        ApplicationContext.setScheduleService(ss);
        LocalDate d = LocalDate.now().plusDays(18);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                DatePicker dp = (DatePicker) getField(c, "datePicker");
                dp.setValue(d);
                c.handleDateSelection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
        Thread.sleep(500);
        waitForFx();
        verify(ss, atLeastOnce()).getAvailableSlots(eq(d), anyInt());
    }

    private void mockCommonServices(boolean dateBookable, Optional<String> tryBookResult) {
        ScheduleService ss = mock(ScheduleService.class);
        when(ss.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(dateBookable);
        ApplicationContext.setScheduleService(ss);

        BookingService bs = mock(BookingService.class);
        when(bs.patientHasBlockingOpenAppointment(anyString())).thenReturn(false);
        when(bs.tryBookWithReason(any(), any())).thenReturn(tryBookResult);
        ApplicationContext.setBookingService(bs);
    }

    private static BookAppointmentController freshController() throws Exception {
        BookAppointmentController c = new BookAppointmentController();
        setField(c, "messageLabel", new Label());
        setField(c, "datePicker", new DatePicker());
        setField(c, "typeCombo", new ComboBox<BookingOption>());
        setField(c, "timeSlotCombo", new ComboBox<TimeSlot>());
        setField(c, "reminderCombo", new ComboBox<String>());
        setField(c, "languageCombo", new ComboBox<String>());
        setField(c, "notesField", new javafx.scene.control.TextArea());
        setField(c, "contactPhoneField", new javafx.scene.control.TextField());
        setField(c, "accessibilityField", new javafx.scene.control.TextField());
        return c;
    }

    @SuppressWarnings("unchecked")
    private static void setValidDateAndType(BookAppointmentController c) throws Exception {
        DatePicker dp = (DatePicker) getField(c, "datePicker");
        ComboBox<BookingOption> type = (ComboBox<BookingOption>) getField(c, "typeCombo");
        dp.setValue(LocalDate.now().plusDays(3));
        BookingOption opt = BookingOption.of(new AppointmentTypeConfig.Type("Consult", 60, 4), false);
        type.setValue(opt);
    }

    private static TimeSlot sampleSlot() {
        LocalDateTime s = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return new TimeSlot(s, s.plusHours(1));
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

    @SuppressWarnings("unchecked")
    private static ComboBox<TimeSlot> timeSlotCombo(BookAppointmentController c) throws Exception {
        return (ComboBox<TimeSlot>) getField(c, "timeSlotCombo");
    }

    private static void invokePrivateNoArg(Object target, String name) throws Exception {
        var m = BookAppointmentController.class.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }

    private static void invokePrivateOneArg(Object target, String name, Class<?> argType, Object arg) throws Exception {
        var m = BookAppointmentController.class.getDeclaredMethod(name, argType);
        m.setAccessible(true);
        m.invoke(target, arg);
    }

    private static void runOnFxVoid(Runnable r) {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
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

    private static void waitForFx() {
        try {
            Thread.sleep(200);
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("FX queue did not drain");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}

