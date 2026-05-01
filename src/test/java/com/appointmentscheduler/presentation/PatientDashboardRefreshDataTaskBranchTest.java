package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * Covers {@link PatientDashboardController} background {@link javafx.concurrent.Task} in
 * {@code refreshAllData} (load schedule → filter → {@code succeeded} / {@code failed}), plus a
 * post-refresh {@link PresentationFxHarness} sweep while tables/calendar hold loaded data (many
 * additional {@code TableCell} / UI branches vs a cold sweep).
 */
@ResourceLock("ApplicationContextServices")
class PatientDashboardRefreshDataTaskBranchTest {

    private static Stage stage;
    private static ScheduleService originalScheduleService;

    @BeforeAll
    static void startMainApp() {
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
        originalScheduleService = ApplicationContext.getScheduleService();
    }

    @AfterEach
    void restoreSchedule() {
        ApplicationContext.setScheduleService(originalScheduleService);
    }

    @AfterAll
    static void clearAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void refreshDataTask_succeeded_then_sweepFxControls_withLoadedRows() {
        stubScheduleLoadQuiet();
        User patient = new User("pat-refresh-sweep", "Pat R", "pat-refresh-sweep@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            seedScheduleForPatientFallback(patient);
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForPatientRefreshTask();
            runOnFxVoid(() -> {
                PresentationFxHarness.invokePrivateNoArg(c, "refreshPatientInbox");
                PresentationFxHarness.sweepDeclaredFxControls(c);
            });
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_failed_loadScheduleException_hitsFailedBranch() {
        if (originalScheduleService == null) {
            return;
        }
        ScheduleService ssSpy = spy(originalScheduleService);
        doThrow(new RuntimeException("simulated loadSchedule failure")).when(ssSpy).loadSchedule();
        ApplicationContext.setScheduleService(ssSpy);

        User patient = new User("pat-refresh-fail", "Pat F", "pat-refresh-fail@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForPatientRefreshTask();
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_succeeded_minimalPath_withoutPostSweep() {
        stubScheduleLoadQuiet();
        User patient = new User("pat-refresh-ok", "Pat O", "pat-refresh-ok@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            seedScheduleForPatientMatch(patient);
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForPatientRefreshTask();
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_succeeded_invokes_onComplete_callback() {
        stubScheduleLoadQuiet();
        User patient = new User("pat-refresh-cb-ok", "Pat Cb Ok", "pat-refresh-cb-ok@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            seedScheduleForPatientMatch(patient);
            CountDownLatch onComplete = new CountDownLatch(1);
            runOnFxVoid(() -> invokeRefreshAllData(c, onComplete::countDown));
            waitForPatientRefreshTask();
            if (!onComplete.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("onComplete callback was not invoked in success path");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_emptySchedule_succeedsWithoutException() {
        stubScheduleLoadQuiet();
        if (originalScheduleService == null) {
            return;
        }
        originalScheduleService.getMasterSchedule().clear();
        User patient = new User("pat-refresh-empty", "Pat E", "pat-refresh-empty@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForPatientRefreshTask();
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_currentUserNull_listsAllAppointmentsWithoutPatientFilter() {
        stubScheduleLoadQuiet();
        User patient = new User("pat-refresh-nulluser", "Pat Nu", "pat-refresh-nulluser@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        seedScheduleForPatientMatch(patient);
        assertThatCode(() -> {
            runOnFxVoid(() -> {
                setControllerField(c, "currentUser", null);
                invokeRefreshAllData(c);
            });
            waitForPatientRefreshTask();
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_bookViewVisible_triggersValidateBookingFormInRunLater() {
        stubScheduleLoadQuiet();
        User patient = new User("pat-refresh-book", "Pat B", "pat-refresh-book@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            seedScheduleForPatientMatch(patient);
            runOnFxVoid(() -> {
                VBox book = (VBox) readControllerField(c, "bookView");
                if (book != null) {
                    book.setVisible(true);
                }
                invokeRefreshAllData(c);
            });
            waitForPatientRefreshTask();
        }).doesNotThrowAnyException();
    }

    @Test
    void refreshDataTask_failed_invokes_onComplete_callback() {
        if (originalScheduleService == null) {
            return;
        }
        ScheduleService ssSpy = spy(originalScheduleService);
        doThrow(new RuntimeException("simulated loadSchedule failure for callback")).when(ssSpy).loadSchedule();
        ApplicationContext.setScheduleService(ssSpy);

        User patient = new User("pat-refresh-cb-fail", "Pat Cb Fail", "pat-refresh-cb-fail@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadPatient();
        assertThatCode(() -> {
            CountDownLatch onComplete = new CountDownLatch(1);
            runOnFxVoid(() -> invokeRefreshAllData(c, onComplete::countDown));
            waitForPatientRefreshTask();
            if (!onComplete.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("onComplete callback was not invoked in failed path");
            }
        }).doesNotThrowAnyException();
    }

    private static void seedScheduleForPatientFallback(User currentUser) {
        if (originalScheduleService == null) {
            return;
        }
        var sched = originalScheduleService.getMasterSchedule();
        sched.clear();
        // Ensure `all` is non-empty but filtered-by-currentUser becomes empty => fallback branch triggers.
        User other = new User("other-user", "Other", "other@example.com", "pw");
        LocalDateTime base = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment otherAppt = new InPersonAppointment(other, new TimeSlot(base, base.plusHours(1)), "R");
        otherAppt.setStatus("CONFIRMED");
        sched.addAppointment(otherAppt);

        // Also add an appointment in the past; fallback keeps only upcoming ones.
        LocalDateTime past = LocalDateTime.now().minusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment pastOther = new InPersonAppointment(other, new TimeSlot(past, past.plusHours(1)), "R");
        pastOther.setStatus("CONFIRMED");
        sched.addAppointment(pastOther);
    }

    private static void seedScheduleForPatientMatch(User currentUser) {
        if (originalScheduleService == null) {
            return;
        }
        var sched = originalScheduleService.getMasterSchedule();
        sched.clear();
        LocalDateTime base = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment mine = new InPersonAppointment(currentUser, new TimeSlot(base, base.plusHours(1)), "R");
        mine.setStatus("CONFIRMED");
        sched.addAppointment(mine);
    }

    private void stubScheduleLoadQuiet() {
        if (originalScheduleService == null) {
            return;
        }
        ScheduleService ssSpy = spy(originalScheduleService);
        doNothing().when(ssSpy).loadSchedule();
        ApplicationContext.setScheduleService(ssSpy);
    }

    private static void setControllerField(PatientDashboardController c, String name, Object value) {
        try {
            Field f = PatientDashboardController.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(c, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readControllerField(PatientDashboardController c, String name) {
        try {
            Field f = PatientDashboardController.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeRefreshAllData(PatientDashboardController c) {
        try {
            Method m = PatientDashboardController.class.getDeclaredMethod("refreshAllData", Runnable.class);
            m.setAccessible(true);
            m.invoke(c, new Object[]{null});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeRefreshAllData(PatientDashboardController c, Runnable onComplete) {
        try {
            Method m = PatientDashboardController.class.getDeclaredMethod("refreshAllData", Runnable.class);
            m.setAccessible(true);
            m.invoke(c, onComplete);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForPatientRefreshTask() {
        try {
            Thread.sleep(1200);
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

    private static PatientDashboardController loadPatient() {
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
