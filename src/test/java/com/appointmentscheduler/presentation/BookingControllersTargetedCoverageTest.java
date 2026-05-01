package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.BookingCatalog;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.application.BookingService;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingControllersTargetedCoverageTest {

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
        ApplicationContext.setBookingService(null);
        ApplicationContext.setScheduleService(null);
        ApplicationContext.setAuthService(null);
    }

    @Test
    void book_handleBook_invalidDate_showsUnavailableMessage() {
        BookAppointmentController c = buildBookController();
        runOnFxVoid(() -> {
            ComboBox<BookingOption> typeCombo = getField(c, "typeCombo");
            DatePicker datePicker = getField(c, "datePicker");
            Label messageLabel = getField(c, "messageLabel");
            typeCombo.setValue(BookingCatalog.listOptions().get(0));
            LocalDate d = LocalDate.now().plusDays(2);
            datePicker.setValue(d);
            when(scheduleService.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);

            c.handleBook();
            assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.day_unavailable_confirm"));
        });
    }

    @Test
    void book_handleBook_withoutSlot_showsErrorMessage() {
        BookAppointmentController c = buildBookController();
        runOnFxVoid(() -> {
            ComboBox<BookingOption> typeCombo = getField(c, "typeCombo");
            DatePicker datePicker = getField(c, "datePicker");
            Label messageLabel = getField(c, "messageLabel");

            typeCombo.setValue(BookingCatalog.listOptions().get(0));
            datePicker.setValue(LocalDate.now().plusDays(3));
            when(scheduleService.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);

            c.handleBook();
            assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.error.message"));
        });
    }

    @Test
    void book_handleBook_blockingOpenAppointment_showsBlockedMessage() {
        BookAppointmentController c = buildBookController();
        runOnFxVoid(() -> {
            ComboBox<BookingOption> typeCombo = getField(c, "typeCombo");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            DatePicker datePicker = getField(c, "datePicker");
            Label messageLabel = getField(c, "messageLabel");

            User user = new User("u-1", "U", "u@test.com", "pw");
            setField(c, "currentUser", user);

            typeCombo.setValue(BookingCatalog.listOptions().get(0));
            datePicker.setValue(LocalDate.now().plusDays(3));
            timeSlotCombo.setValue(new TimeSlot(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1)));

            when(scheduleService.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
            when(bookingService.patientHasBlockingOpenAppointment(anyString())).thenReturn(true);

            c.handleBook();
            assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.blocked.completion_required.detail"));
        });
    }

    @Test
    void book_handleBook_failureCodes_coverSpecificAndGenericBranches() {
        BookAppointmentController c = buildBookController();
        runOnFxVoid(() -> {
            ComboBox<BookingOption> typeCombo = getField(c, "typeCombo");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            DatePicker datePicker = getField(c, "datePicker");
            Label messageLabel = getField(c, "messageLabel");
            User user = new User("u-2", "U2", "u2@test.com", "pw");
            setField(c, "currentUser", user);

            typeCombo.setValue(BookingCatalog.listOptions().get(0));
            datePicker.setValue(LocalDate.now().plusDays(4));
            timeSlotCombo.setValue(new TimeSlot(LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(4).plusHours(1)));
            when(scheduleService.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
            when(bookingService.patientHasBlockingOpenAppointment(anyString())).thenReturn(false);
            java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
            when(bookingService.tryBookWithReason(any(), any())).thenAnswer(invocation ->
                    calls.getAndIncrement() == 0
                            ? Optional.of("OPEN_APPOINTMENT_NOT_COMPLETED")
                            : Optional.of("ANY_OTHER_REASON"));

            c.handleBook();
            assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.blocked.completion_required.detail"));

            c.handleBook();
            assertThat(messageLabel.getText()).contains(I18n.get("booking.error.message"));
            assertThat(messageLabel.getText()).contains("ANY_OTHER_REASON");
        });
    }

    @Test
    void modify_handleModify_nullTarget_and_missingSlot_and_failedModify() {
        ModifyAppointmentController c = buildModifyController();
        runOnFxVoid(() -> {
            Label messageLabel = getField(c, "messageLabel");
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");

            c.handleModify();
            assertThat(messageLabel.getText()).isEqualTo("No appointment chosen to modify.");

            User user = new User("u-3", "U3", "u3@test.com", "pw");
            setField(c, "currentUser", user);
            TimeSlot original = new TimeSlot(LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1));
            IndividualAppointment appt = new IndividualAppointment(user, original);
            appt.setStatus("CONFIRMED");
            setField(c, "targetAppointment", appt);

            datePicker.setValue(original.getStartTime().toLocalDate());
            c.handleModify();
            assertThat(messageLabel.getText()).isEqualTo("Please select a new time slot.");

            timeSlotCombo.setValue(new TimeSlot(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(6).plusHours(1)));
            when(bookingService.modifyAppointment(anyString(), any(), any())).thenReturn(false);
            c.handleModify();
            assertThat(messageLabel.getText()).contains("Failed to modify appointment");
        });
    }

    @Test
    void modify_handleDateSelection_nullDate_clearsMessageAndSelection() {
        ModifyAppointmentController c = buildModifyController();
        runOnFxVoid(() -> {
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            Label messageLabel = getField(c, "messageLabel");
            timeSlotCombo.getItems().setAll(List.of(
                    new TimeSlot(LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(1))
            ));
            timeSlotCombo.getSelectionModel().selectFirst();

            datePicker.setValue(null);
            c.handleDateSelection();

            assertThat(timeSlotCombo.getItems()).isEmpty();
            assertThat(timeSlotCombo.getValue()).isNull();
            assertThat(messageLabel.getText()).isEmpty();
        });
    }

    @Test
    void modify_handleDateSelection_unavailableAndAvailableBranches() {
        ModifyAppointmentController c = buildModifyController();
        runOnFxVoid(() -> {
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            Label messageLabel = getField(c, "messageLabel");

            User user = new User("u-4", "U4", "u4@test.com", "pw");
            setField(c, "currentUser", user);
            TimeSlot original = new TimeSlot(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1));
            IndividualAppointment appt = new IndividualAppointment(user, original);
            appt.setStatus("CONFIRMED");
            setField(c, "targetAppointment", appt);

            // unavailable branch: past date
            datePicker.setValue(LocalDate.now().minusDays(1));
            c.handleDateSelection();
            assertThat(messageLabel.getText()).isEqualTo(BookingDateMessages.unavailable(LocalDate.now().minusDays(1)));

            // available branch with empty slots
            LocalDate d = original.getStartTime().toLocalDate();
            when(scheduleService.getAvailableSlots(d)).thenReturn(List.of());
            datePicker.setValue(d);
            c.handleDateSelection();
            assertThat(timeSlotCombo.getItems()).isEmpty();
            assertThat(messageLabel.getText()).isEqualTo(BookingDateMessages.unavailable(d));

            // available branch with at least one slot
            TimeSlot next = new TimeSlot(LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(4).plusHours(1));
            when(scheduleService.getAvailableSlots(d)).thenReturn(List.of(next));
            c.handleDateSelection();
            assertThat(timeSlotCombo.getItems()).isNotEmpty();
            assertThat(messageLabel.getText()).isEmpty();
        });
    }

    @Test
    void book_handleBack_adminAndPatientBranches() {
        BookAppointmentController c = buildBookController();
        runOnFxVoid(() -> {
            try (var main = org.mockito.Mockito.mockStatic(MainApp.class)) {
                User admin = new com.appointmentscheduler.domain.Administrator("adm-bb", "Admin", "adm@example.com", "pw");
                setField(c, "currentUser", admin);
                c.handleBack();
                main.verify(() -> MainApp.loadScreen(ScreenConstants.FXML_ADMIN_DASHBOARD, ScreenConstants.titleAdminDashboard()));

                User patient = new User("pat-bb", "Pat", "pat@example.com", "pw");
                setField(c, "currentUser", patient);
                c.handleBack();
                main.verify(() -> MainApp.loadScreen(ScreenConstants.FXML_PATIENT_DASHBOARD, ScreenConstants.titlePatientDashboard()));
            }
        });
    }

    @Test
    void book_handleDateSelection_unavailable_then_null_clears_slots() {
        BookAppointmentController c = buildBookController();
        runOnFxVoid(() -> {
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<BookingOption> typeCombo = getField(c, "typeCombo");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");
            Label messageLabel = getField(c, "messageLabel");

            typeCombo.setItems(FXCollections.observableArrayList(BookingCatalog.listOptions()));
            typeCombo.getSelectionModel().selectFirst();

            LocalDate d = LocalDate.now().plusDays(2);
            when(scheduleService.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(false);
            datePicker.setValue(d);
            c.handleDateSelection();
            assertThat(messageLabel.getText()).isEqualTo(BookingDateMessages.unavailable(d));

            timeSlotCombo.getItems().setAll(List.of(
                    new TimeSlot(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1))
            ));
            datePicker.setValue(null);
            c.handleDateSelection();
            assertThat(timeSlotCombo.getItems()).isEmpty();
            assertThat(messageLabel.getText()).isEmpty();
        });
    }

    @Test
    void modify_handleModify_dateUnavailableBranch_beforeBookingServiceCall() {
        ModifyAppointmentController c = buildModifyController();
        runOnFxVoid(() -> {
            Label messageLabel = getField(c, "messageLabel");
            DatePicker datePicker = getField(c, "datePicker");
            ComboBox<TimeSlot> timeSlotCombo = getField(c, "timeSlotCombo");

            User user = new User("u-5", "U5", "u5@test.com", "pw");
            setField(c, "currentUser", user);
            TimeSlot original = new TimeSlot(LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1));
            IndividualAppointment appt = new IndividualAppointment(user, original);
            appt.setStatus("CONFIRMED");
            setField(c, "targetAppointment", appt);

            datePicker.setValue(LocalDate.now().minusDays(1));
            timeSlotCombo.setValue(new TimeSlot(LocalDateTime.now().plusDays(6), LocalDateTime.now().plusDays(6).plusHours(1)));

            c.handleModify();
            assertThat(messageLabel.getText()).isEqualTo(I18n.get("booking.day_unavailable_confirm"));
            verify(bookingService, never()).modifyAppointment(anyString(), any(), any());
        });
    }

    private BookAppointmentController buildBookController() {
        BookAppointmentController c = new BookAppointmentController();
        runOnFxVoid(() -> {
            setField(c, "datePicker", new DatePicker());
            setField(c, "timeSlotCombo", new ComboBox<TimeSlot>());
            setField(c, "typeCombo", new ComboBox<BookingOption>());
            setField(c, "messageLabel", new Label());
            setField(c, "btnConfirmBooking", new Button());
            setField(c, "notesField", new TextArea());
            setField(c, "contactPhoneField", new TextField());
            setField(c, "accessibilityField", new TextField());
            setField(c, "reminderCombo", new ComboBox<String>());
            setField(c, "languageCombo", new ComboBox<String>());
            setField(c, "partySizeSpinner", new Spinner<Integer>());
        });
        return c;
    }

    private ModifyAppointmentController buildModifyController() {
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
    private static <T> T getField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
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
            if (!latch.await(15, TimeUnit.SECONDS)) throw new AssertionError("FX task timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) throw new RuntimeException(err.get());
        return ref.get();
    }

    private static void runOnFxVoid(Runnable r) {
        runOnFx(() -> {
            r.run();
            return null;
        });
    }
}
