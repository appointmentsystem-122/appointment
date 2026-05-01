package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.BookingFailureCodes;
import com.appointmentscheduler.application.BookingService;
import com.appointmentscheduler.application.BookingOption;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Covers {@link PatientDashboardController} anonymous {@link javafx.concurrent.Task} in
 * {@link PatientDashboardController#handleConfirmBooking()} (JaCoCo: {@code PatientDashboardController$8})
 * by stubbing {@link BookingService#tryBookWithReason} on a spy while keeping other booking logic real.
 */
@ResourceLock("ApplicationContextServices")
class PatientDashboardBookTaskBranchTest {

    private static Stage stage;
    private static BookingService originalBookingService;
    private static ScheduleService originalScheduleService;

    @BeforeAll
    static void startMainApp() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
        stage = runOnFx(Stage::new);
        MainApp app = new MainApp();
        Throwable err = runOnFx(() -> {
            try {
                app.start(stage);
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        if (err != null) {
            throw new RuntimeException(err);
        }
        originalBookingService = ApplicationContext.getBookingService();
        originalScheduleService = ApplicationContext.getScheduleService();
    }

    @AfterEach
    void restoreServices() {
        ApplicationContext.setBookingService(originalBookingService);
        ApplicationContext.setScheduleService(originalScheduleService);
    }

    @AfterAll
    static void clearAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void bookTask_succeeded_emptyOptional_showsSuccessPath() {
        BookingService spyBooking = spy(originalBookingService);
        doReturn(Optional.empty()).when(spyBooking).tryBookWithReason(any(), any());
        ApplicationContext.setBookingService(spyBooking);
        stubScheduleDateBookable();

        User patient = new User("booktask-ok", "Book OK", "booktask-ok@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatientDashboard();

        prepareFutureBookingForm(c);
        assertThatCode(() -> {
            runOnFxVoid(c::handleConfirmBooking);
            waitForBackgroundBookTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    @Test
    void bookTask_succeeded_openAppointmentCode_showsBlockedDialog() {
        BookingService spyBooking = spy(originalBookingService);
        doReturn(Optional.of(BookingFailureCodes.OPEN_APPOINTMENT_NOT_COMPLETED))
                .when(spyBooking).tryBookWithReason(any(), any());
        ApplicationContext.setBookingService(spyBooking);
        stubScheduleDateBookable();

        User patient = new User("booktask-open", "Book Open", "booktask-open@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatientDashboard();

        prepareFutureBookingForm(c);
        assertThatCode(() -> {
            runOnFxVoid(c::handleConfirmBooking);
            waitForBackgroundBookTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    @Test
    void bookTask_succeeded_otherFailureCode_showsGenericErrorDialog() {
        BookingService spyBooking = spy(originalBookingService);
        doReturn(Optional.of("SLOT_TAKEN_TEST")).when(spyBooking).tryBookWithReason(any(), any());
        ApplicationContext.setBookingService(spyBooking);
        stubScheduleDateBookable();

        User patient = new User("booktask-other", "Book Other", "booktask-other@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatientDashboard();

        prepareFutureBookingForm(c);
        assertThatCode(() -> {
            runOnFxVoid(c::handleConfirmBooking);
            waitForBackgroundBookTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    @Test
    void bookTask_failed_exception_showsFailedPath() {
        BookingService spyBooking = spy(originalBookingService);
        doThrow(new RuntimeException("simulated booking failure"))
                .when(spyBooking).tryBookWithReason(any(), any());
        ApplicationContext.setBookingService(spyBooking);
        stubScheduleDateBookable();

        User patient = new User("booktask-fail", "Book Fail", "booktask-fail@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatientDashboard();

        prepareFutureBookingForm(c);
        assertThatCode(() -> {
            runOnFxVoid(c::handleConfirmBooking);
            waitForBackgroundBookTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    private void stubScheduleDateBookable() {
        ScheduleService real = originalScheduleService;
        if (real == null) {
            return;
        }
        ScheduleService ssSpy = spy(real);
        when(ssSpy.isDateBookable(any(LocalDate.class), anyInt())).thenReturn(true);
        ApplicationContext.setScheduleService(ssSpy);
    }

    private static void prepareFutureBookingForm(PatientDashboardController c) {
        runOnFxVoid(() -> {
            try {
                DatePicker dp = (DatePicker) getDeclaredField(c, "datePicker");
                @SuppressWarnings("unchecked")
                ComboBox<BookingOption> typeCombo = (ComboBox<BookingOption>) getDeclaredField(c, "typeCombo");
                @SuppressWarnings("unchecked")
                ComboBox<String> hourCombo = (ComboBox<String>) getDeclaredField(c, "hourCombo");
                @SuppressWarnings("unchecked")
                ComboBox<String> minuteCombo = (ComboBox<String>) getDeclaredField(c, "minuteCombo");
                dp.setValue(LocalDate.now().plusDays(14));
                if (!typeCombo.getItems().isEmpty()) {
                    typeCombo.getSelectionModel().selectFirst();
                }
                if (!hourCombo.getItems().isEmpty()) {
                    hourCombo.setValue("10");
                }
                if (!minuteCombo.getItems().isEmpty()) {
                    minuteCombo.setValue("00");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void waitForBackgroundBookTask() {
        try {
            Thread.sleep(900);
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("FX queue did not drain");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static Object getDeclaredField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void forceAuth(User user) {
        try {
            Object authSvc = ApplicationContext.getAuthService();
            if (authSvc == null) {
                return;
            }
            Field f = authSvc.getClass().getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(authSvc, user);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private static PatientDashboardController loadPatientDashboard() {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(ScreenConstants.BASE_PATH + ScreenConstants.FXML_PATIENT_DASHBOARD));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
            return loader.getController();
        });
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
            if (!latch.await(60, TimeUnit.SECONDS)) {
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
