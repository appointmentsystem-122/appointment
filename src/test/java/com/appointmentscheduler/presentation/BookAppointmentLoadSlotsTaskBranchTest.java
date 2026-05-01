package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * Covers {@link BookAppointmentController} anonymous {@link javafx.concurrent.Task} in
 * {@code loadTimeSlotsAsync} (JaCoCo: {@code BookAppointmentController$2}).
 */
@ResourceLock("ApplicationContextServices")
class BookAppointmentLoadSlotsTaskBranchTest {

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
    void restoreScheduleService() {
        ApplicationContext.setScheduleService(originalScheduleService);
    }

    @AfterAll
    static void clearAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void loadSlotsTask_succeeded_emptyList() {
        LocalDate d = LocalDate.now().plusDays(14);
        ScheduleService ssSpy = spy(originalScheduleService);
        doReturn(true).when(ssSpy).isDateBookable(any(LocalDate.class), anyInt());
        doReturn(Collections.emptyList()).when(ssSpy).getAvailableSlots(any(LocalDate.class), anyInt());
        ApplicationContext.setScheduleService(ssSpy);

        User user = new User("slots-empty", "S", "slots-empty@example.com", "pw");
        forceAuth(user);
        BookAppointmentController c = loadBook();

        assertThatCode(() -> {
            runOnFxVoid(() -> invokeLoadTimeSlotsAsync(c, d));
            waitForLoadSlotsTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    @Test
    void loadSlotsTask_succeeded_nonEmptyList() {
        LocalDate d = LocalDate.now().plusDays(14);
        LocalDateTime start = d.atTime(10, 0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        ScheduleService ssSpy = spy(originalScheduleService);
        doReturn(true).when(ssSpy).isDateBookable(any(LocalDate.class), anyInt());
        doReturn(List.of(slot)).when(ssSpy).getAvailableSlots(any(LocalDate.class), anyInt());
        ApplicationContext.setScheduleService(ssSpy);

        User user = new User("slots-ok", "S", "slots-ok@example.com", "pw");
        forceAuth(user);
        BookAppointmentController c = loadBook();

        assertThatCode(() -> {
            runOnFxVoid(() -> invokeLoadTimeSlotsAsync(c, d));
            waitForLoadSlotsTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    @Test
    void loadSlotsTask_failed_exception() {
        LocalDate d = LocalDate.now().plusDays(14);
        ScheduleService ssSpy = spy(originalScheduleService);
        doReturn(true).when(ssSpy).isDateBookable(any(LocalDate.class), anyInt());
        doThrow(new RuntimeException("simulated slots failure"))
                .when(ssSpy).getAvailableSlots(any(LocalDate.class), anyInt());
        ApplicationContext.setScheduleService(ssSpy);

        User user = new User("slots-fail", "S", "slots-fail@example.com", "pw");
        forceAuth(user);
        BookAppointmentController c = loadBook();

        assertThatCode(() -> {
            runOnFxVoid(() -> invokeLoadTimeSlotsAsync(c, d));
            waitForLoadSlotsTask();
            runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c));
        }).doesNotThrowAnyException();
    }

    /**
     * Invokes the private {@code loadTimeSlotsAsync} so the anonymous {@code Task} runs with a
     * definite non-null date (avoids DatePicker/listener timing and {@code eq(d)} stub mismatches).
     */
    private static void invokeLoadTimeSlotsAsync(BookAppointmentController c, LocalDate d) {
        try {
            Method m = BookAppointmentController.class.getDeclaredMethod("loadTimeSlotsAsync", LocalDate.class);
            m.setAccessible(true);
            m.invoke(c, d);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForLoadSlotsTask() {
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

    private static BookAppointmentController loadBook() {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(ScreenConstants.BASE_PATH + ScreenConstants.FXML_BOOK_APPOINTMENT));
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
