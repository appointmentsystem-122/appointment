package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.BookingService;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.Schedule;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ResourceLock("ApplicationContextServices")
class ModifyAppointmentControllerTargetedBranchesTest {

    private AuthService authService;
    private ScheduleService scheduleService;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        JavaFxTestSupport.initPlatform();
        authService = mock(AuthService.class);
        scheduleService = mock(ScheduleService.class);
        bookingService = mock(BookingService.class);
        ApplicationContext.setAuthService(authService);
        ApplicationContext.setScheduleService(scheduleService);
        ApplicationContext.setBookingService(bookingService);
    }

    @AfterEach
    void tearDown() {
        ModifyAppointmentController.appointmentIdToModify = null;
        ApplicationContext.setBookingService(null);
        ApplicationContext.setScheduleService(null);
        ApplicationContext.setAuthService(null);
    }

    @Test
    void initialize_usesAppointmentIdToModify_andClearsStaticFlag() {
        User user = new User("pat-mod-id", "Pat", "pat-mod-id@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        Schedule schedule = new Schedule();
        IndividualAppointment mine = confirmed(user, 2);
        schedule.addAppointment(mine);
        when(scheduleService.getMasterSchedule()).thenReturn(schedule);
        ModifyAppointmentController.appointmentIdToModify = mine.getId();

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);

        Label currentApptLabel = getField(c, "currentApptLabel");
        assertThat(currentApptLabel.getText()).contains("Modifying:");
        assertThat(ModifyAppointmentController.appointmentIdToModify).isNull();
    }

    @Test
    void initialize_withoutStaticId_findsFirstConfirmedForCurrentUser_orShowsNoAppointment() {
        User user = new User("pat-mod-first", "Pat", "pat-mod-first@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);

        Schedule scheduleWithConfirmed = new Schedule();
        IndividualAppointment cancelled = confirmed(user, 3);
        cancelled.setStatus("CANCELLED");
        IndividualAppointment confirmed = confirmed(user, 4);
        scheduleWithConfirmed.addAppointment(cancelled);
        scheduleWithConfirmed.addAppointment(confirmed);
        when(scheduleService.getMasterSchedule()).thenReturn(scheduleWithConfirmed);

        ModifyAppointmentController c1 = buildController();
        runOnFxVoid(c1::initialize);
        Label label1 = getField(c1, "currentApptLabel");
        assertThat(label1.getText()).contains("Modifying:");

        Schedule scheduleNoConfirmed = new Schedule();
        scheduleNoConfirmed.addAppointment(cancelled);
        when(scheduleService.getMasterSchedule()).thenReturn(scheduleNoConfirmed);

        ModifyAppointmentController c2 = buildController();
        runOnFxVoid(c2::initialize);
        Label label2 = getField(c2, "currentApptLabel");
        assertThat(label2.getText()).isEqualTo("No confirmed appointment found to modify.");
    }

    @Test
    void initialize_staticAppointmentIdNotFound_clearsFlagAndShowsNoAppointment() {
        User user = new User("pat-mod-ghost", "Pat", "pat-mod-ghost@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        Schedule schedule = new Schedule();
        schedule.addAppointment(confirmed(user, 2));
        when(scheduleService.getMasterSchedule()).thenReturn(schedule);
        ModifyAppointmentController.appointmentIdToModify = "id-that-does-not-exist";

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        Label currentApptLabel = getField(c, "currentApptLabel");
        assertThat(currentApptLabel.getText()).isEqualTo("No confirmed appointment found to modify.");
        assertThat(ModifyAppointmentController.appointmentIdToModify).isNull();
    }

    @Test
    void isDateAllowedForModify_branches_nullPastCurrentAndScheduleDecision() throws Exception {
        User user = new User("pat-mod-allowed", "Pat", "pat-mod-allowed@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(false, true);

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "targetAppointment", confirmed(user, 5));

        Method m = ModifyAppointmentController.class.getDeclaredMethod("isDateAllowedForModify", LocalDate.class);
        m.setAccessible(true);

        boolean nullDate = (boolean) m.invoke(c, new Object[]{null});
        boolean pastDate = (boolean) m.invoke(c, LocalDate.now().minusDays(1));
        boolean currentDate = (boolean) m.invoke(c, LocalDate.now().plusDays(5));
        boolean futureClosed = (boolean) m.invoke(c, LocalDate.now().plusDays(7));
        boolean futureOpen = (boolean) m.invoke(c, LocalDate.now().plusDays(8));

        assertThat(nullDate).isFalse();
        assertThat(pastDate).isFalse();
        assertThat(currentDate).isTrue();
        assertThat(futureClosed).isFalse();
        assertThat(futureOpen).isTrue();
    }

    @Test
    void handleModify_successBranch_callsBackNavigation() {
        User user = new User("pat-mod-ok", "Pat", "pat-mod-ok@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(true);
        when(bookingService.modifyAppointment(anyString(), any(), any())).thenReturn(true);

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "currentUser", user);
        setField(c, "targetAppointment", confirmed(user, 6));

        runOnFxVoid(() -> {
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            datePicker.setValue(LocalDate.now().plusDays(6));
            TimeSlot replacement = new TimeSlot(LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(1));
            timeSlotCombo.getItems().setAll(List.of(replacement));
            timeSlotCombo.setValue(replacement);
        });

        assertThatCode(() -> runOnFxVoid(() -> {
            try (var main = mockStatic(MainApp.class)) {
                main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
                c.handleModify();
            }
        })).doesNotThrowAnyException();
        Label messageLabel = getField(c, "messageLabel");
        assertThat(messageLabel.getText()).isEqualTo("Appointment Time Modified Successfully!");
        verify(bookingService).modifyAppointment(anyString(), any(), any());
    }

    @Test
    void handleModify_noTarget_showsNoAppointmentChosen() {
        User user = new User("pat-mod-nt", "Pat", "pat-mod-nt@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        Schedule schedule = new Schedule();
        IndividualAppointment cancelled = confirmed(user, 9);
        cancelled.setStatus("CANCELLED");
        schedule.addAppointment(cancelled);
        when(scheduleService.getMasterSchedule()).thenReturn(schedule);

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        runOnFxVoid(c::handleModify);
        Label messageLabel = getField(c, "messageLabel");
        assertThat(messageLabel.getText()).isEqualTo("No appointment chosen to modify.");
    }

    @Test
    void handleModify_nullDate_showsDayUnavailableMessage() {
        User user = new User("pat-mod-nd", "Pat", "pat-mod-nd@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "currentUser", user);
        setField(c, "targetAppointment", confirmed(user, 5));
        runOnFxVoid(() -> {
            DatePicker dp = getField(c, "datePicker");
            dp.setValue(null);
        });
        runOnFxVoid(c::handleModify);
        Label messageLabel = getField(c, "messageLabel");
        assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.day_unavailable_confirm"));
    }

    @Test
    void handleModify_dateNotAllowed_showsDayUnavailableMessage() {
        User user = new User("pat-mod-da", "Pat", "pat-mod-da@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(false);
        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "currentUser", user);
        setField(c, "targetAppointment", confirmed(user, 5));
        runOnFxVoid(() -> {
            DatePicker dp = getField(c, "datePicker");
            dp.setValue(LocalDate.now().plusDays(20));
        });
        runOnFxVoid(c::handleModify);
        Label messageLabel = getField(c, "messageLabel");
        assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.day_unavailable_confirm"));
    }

    @Test
    void handleModify_nullSlot_showsSelectSlotMessage() {
        User user = new User("pat-mod-ns", "Pat", "pat-mod-ns@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(true);
        when(scheduleService.getAvailableSlots(any(LocalDate.class))).thenReturn(Collections.emptyList());
        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "currentUser", user);
        setField(c, "targetAppointment", confirmed(user, 6));
        runOnFxVoid(() -> {
            DatePicker dp = getField(c, "datePicker");
            ComboBox<TimeSlot> slots = getField(c, "timeSlotCombo");
            dp.setValue(LocalDate.now().plusDays(6));
            slots.setValue(null);
        });
        runOnFxVoid(c::handleModify);
        Label messageLabel = getField(c, "messageLabel");
        assertThat(messageLabel.getText()).isEqualTo("Please select a new time slot.");
    }

    @Test
    void handleModify_modifyReturnsFalse_showsFailureMessage() {
        User user = new User("pat-mod-fail", "Pat", "pat-mod-fail@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(true);
        when(bookingService.modifyAppointment(anyString(), any(), any())).thenReturn(false);

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "currentUser", user);
        setField(c, "targetAppointment", confirmed(user, 10));

        runOnFxVoid(() -> {
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            datePicker.setValue(LocalDate.now().plusDays(6));
            TimeSlot replacement = new TimeSlot(LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(1));
            timeSlotCombo.getItems().setAll(List.of(replacement));
            timeSlotCombo.setValue(replacement);
        });

        runOnFxVoid(c::handleModify);
        Label messageLabel = getField(c, "messageLabel");
        assertThat(messageLabel.getText()).contains("Failed to modify");
    }

    @Test
    void handleBack_patientAndAdmin_useStubbedMainApp() throws Exception {
        try (var main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            ModifyAppointmentController c = buildController();
            setField(c, "currentUser", new User("u-bk", "U", "u-bk@example.com", "pw"));
            c.handleBack();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_PATIENT_DASHBOARD), anyString()));
            setField(c, "currentUser", new Administrator("a-bk", "A", "a-bk@example.com", "pw"));
            c.handleBack();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_ADMIN_DASHBOARD), anyString()));
        }
    }

    @Test
    void showMessage_errorAndSuccess_coversStyleBranches() throws Exception {
        ModifyAppointmentController c = buildController();
        Label lbl = new Label();
        setField(c, "messageLabel", lbl);
        Method m = ModifyAppointmentController.class.getDeclaredMethod("showMessage", String.class, boolean.class);
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
    void onModifyDateChanged_nullDateAndSlotsBranches() throws Exception {
        User user = new User("pat-mod-od", "Pat", "pat-mod-od@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(true);
        TimeSlot sampleSlot = new TimeSlot(
                LocalDateTime.now().plusDays(3).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(3).withHour(11).withMinute(0));
        when(scheduleService.getAvailableSlots(any(LocalDate.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(sampleSlot));

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "targetAppointment", confirmed(user, 11));

        Method m = ModifyAppointmentController.class.getDeclaredMethod("onModifyDateChanged", LocalDate.class);
        m.setAccessible(true);

        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                m.invoke(c, new Object[]{null});
                m.invoke(c, LocalDate.now().plusDays(5));
                m.invoke(c, LocalDate.now().plusDays(6));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    @Test
    void initialize_dateCell_emptyAndNull_coversDisableBranches() {
        User user = new User("pat-mod-dc", "Pat", "pat-mod-dc@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        when(scheduleService.isDateBookable(any(LocalDate.class))).thenReturn(true);

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        DatePicker dp = getField(c, "datePicker");
        Callback<DatePicker, DateCell> factory = dp.getDayCellFactory();
        assertThat(factory).isNotNull();
        DateCell cell = factory.call(dp);
        assertThatCode(() -> runOnFxVoid(() -> {
            cell.updateItem(null, true);
            cell.updateItem(null, false);
        })).doesNotThrowAnyException();
    }

    @Test
    void isDateAllowedForModify_whenTargetHasNullTimeSlot_skipsCurrentDayShortcut() throws Exception {
        User user = new User("pat-mod-tsn", "Pat", "pat-mod-tsn@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());

        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);

        IndividualAppointment appt = confirmed(user, 9);
        Field ts = Appointment.class.getDeclaredField("timeSlot");
        ts.setAccessible(true);
        ts.set(appt, null);
        setField(c, "targetAppointment", appt);

        LocalDate future = LocalDate.now().plusDays(5);
        when(scheduleService.isDateBookable(future)).thenReturn(true);

        Method m = ModifyAppointmentController.class.getDeclaredMethod("isDateAllowedForModify", LocalDate.class);
        m.setAccessible(true);
        assertThat(m.invoke(c, future)).isEqualTo(true);
        verify(scheduleService).isDateBookable(future);
    }

    @Test
    void validateModifyForm_submitNull_skipsDisableOnButton() throws Exception {
        User user = new User("pat-mod-vf", "Pat", "pat-mod-vf@example.com", "pw");
        when(authService.getCurrentUser()).thenReturn(user);
        when(scheduleService.getMasterSchedule()).thenReturn(new Schedule());
        ModifyAppointmentController c = buildController();
        runOnFxVoid(c::initialize);
        setField(c, "btnSubmitModify", null);
        Method m = ModifyAppointmentController.class.getDeclaredMethod("validateModifyForm");
        m.setAccessible(true);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                m.invoke(c);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
    }

    private static IndividualAppointment confirmed(User user, int dayOffset) {
        LocalDateTime start = LocalDate.now().plusDays(dayOffset).atTime(10, 0);
        IndividualAppointment appt = new IndividualAppointment(user, new TimeSlot(start, start.plusHours(1)));
        appt.setStatus("CONFIRMED");
        return appt;
    }

    private static ModifyAppointmentController buildController() {
        ModifyAppointmentController c = new ModifyAppointmentController();
        runOnFxVoid(() -> {
            setField(c, "datePicker", new DatePicker());
            setField(c, "timeSlotCombo", new ComboBox<TimeSlot>());
            setField(c, "messageLabel", new Label());
            setField(c, "btnSubmitModify", new Button());
            setField(c, "currentApptLabel", new Label());
        });
        return c;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T runOnFx(Callable<T> task) {
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(task.call());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
        return ref.get();
    }

    private static void runOnFxVoid(Runnable r) {
        runOnFx(() -> {
            r.run();
            return null;
        });
    }
}
