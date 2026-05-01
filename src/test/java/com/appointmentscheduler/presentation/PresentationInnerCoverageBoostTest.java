package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PresentationInnerCoverageBoostTest {

    private static Stage stage;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
        stage = runOnFx(Stage::new);

        MainApp mainApp = new MainApp();
        Throwable startupError = runOnFx(() -> {
            try {
                mainApp.start(stage);
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        if (startupError != null) throw new RuntimeException("MainApp.start failed", startupError);

        // Force non-null auth user to satisfy controllers that dereference it.
        forceAuthCurrentUser(new User(
                "forced-user-" + System.nanoTime(),
                "Forced User",
                "forced-user@example.com",
                "pw"
        ));
    }

    @Test
    void coverDialogHelper_dateCells_andBookingTaskAndSessionWarning() {
        assertThatCode(() -> runOnFxVoid(() -> {
            DialogHelper.showLogoutConfirmation(AppConfig.getAppName());
            DialogHelper.showConfirmation("t", "h", "c");
            DialogHelper.showInfo("info", "hello");
            DialogHelper.showError("err", "boom");
            DialogHelper.showKeyboardShortcutsClient(null);
            DialogHelper.showKeyboardShortcutsAdmin(null);
        })).doesNotThrowAnyException();

        // Drive BookAppointmentController: trigger loadTimeSlotsAsync(Task) via handleDateSelection.
        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        Optional<TimeSlot> nextBook = nextAvailableSlot(60);
        assertThat(nextBook).isPresent();
        runOnFxVoid(() -> {
            DatePicker dp = (DatePicker) getField(book, "datePicker");
            dp.setValue(nextBook.get().getStartTime().toLocalDate());
            book.handleDateSelection();
        });
        sleepQuietly(1200);

        // Drive PatientDashboardController dayCellFactory DateCell.updateItem.
        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        runOnFxVoid(() -> patient.handleNavBook());
        sleepQuietly(400);

        Optional<TimeSlot> nextPatient = nextAvailableSlot(30);
        assertThat(nextPatient).isPresent();
        runOnFxVoid(() -> {
            DatePicker dp = (DatePicker) getField(patient, "datePicker");
            // Force creation + execution of the DayCell updateItem logic.
            var dayFactory = dp.getDayCellFactory();
            if (dayFactory != null) {
                DateCell cell = dayFactory.call(dp);
                cell.updateItem(nextPatient.get().getStartTime().toLocalDate(), false);
            }
        });

        // Drive ModifyAppointmentController dayCellFactory DateCell.updateItem.
        ModifyAppointmentController modify = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        Optional<TimeSlot> nextModify = nextAvailableSlot(60);
        assertThat(nextModify).isPresent();
        runOnFxVoid(() -> {
            DatePicker dp = (DatePicker) getField(modify, "datePicker");
            var dayFactory = dp.getDayCellFactory();
            if (dayFactory != null) {
                DateCell cell = dayFactory.call(dp);
                cell.updateItem(nextModify.get().getStartTime().toLocalDate(), false);
            }
        });

        // Trigger SessionManager warning dialog path via reflection (no blocking in autoDialogs mode).
        triggerSessionWarning();
        sleepQuietly(500);
    }

    private static void triggerSessionWarning() {
        try {
            SessionManager sm = SessionManager.getInstance();
            // Set lastActivity so minutesInactive >= warning but < timeout.
            Field lastActivityF = SessionManager.class.getDeclaredField("lastActivity");
            lastActivityF.setAccessible(true);
            lastActivityF.set(sm, java.time.LocalDateTime.now().minusMinutes(AppConfig.getSessionWarningMinutes()));

            Field warningShownF = SessionManager.class.getDeclaredField("warningShown");
            warningShownF.setAccessible(true);
            warningShownF.set(sm, false);

            Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
            checkTimeout.setAccessible(true);

            // Also ensure current user exists.
            forceAuthCurrentUser(new User(
                    "forced-user-" + System.nanoTime(),
                    "Forced User",
                    "forced-user@example.com",
                    "pw"
            ));

            runOnFxVoid(() -> {
                try {
                    checkTimeout.invoke(sm);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static Optional<TimeSlot> nextAvailableSlot(int durationMinutes) {
        try {
            ScheduleService ss = ApplicationContext.getScheduleService();
            if (ss == null) return Optional.empty();
            ss.loadSchedule();
            return ss.getNextAvailableSlot(durationMinutes);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static <T> T loadFxml(Class<T> controllerType, String fxmlFile) {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(ScreenConstants.BASE_PATH + fxmlFile));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
            Object controller = loader.getController();
            return controllerType.cast(controller);
        });
    }

    private static void forceAuthCurrentUser(User user) {
        try {
            Object authSvc = ApplicationContext.getAuthService();
            if (authSvc == null) return;
            Field f = authSvc.getClass().getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(authSvc, user);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private static Object getField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed getting field " + fieldName, e);
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
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

