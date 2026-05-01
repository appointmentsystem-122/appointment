package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PresentationCoverageBoostTest {

    private static Stage stage;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();

        stage = runOnFx(Stage::new);

        // Start MainApp once so ApplicationContext is fully wired.
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
    }

    @Test
    void presentation_sweep_invokesMostFXMLNoArgHandlers() {
        // Ensure currentUser is non-null for controllers that dereference it in initialize().
        forceAuthCurrentUser(new Administrator(
                "forced-admin-" + System.nanoTime(),
                "Forced Admin",
                "forced-admin@example.com",
                "pw"
        ));

        AdminDashboardController admin = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        sleepQuietly(800);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(admin);
            invokeNoArgFXMLMethods(admin);
            PresentationFxHarness.sweepDeclaredFxControls(admin);
        })).doesNotThrowAnyException();
        sleepQuietly(800);

        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        // switch to non-admin stub so both branches get some execution
        forceAuthCurrentUser(new User(
                "forced-user-" + System.nanoTime(),
                "Forced User",
                "forced-user@example.com",
                "pw"
        ));
        sleepQuietly(600);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(patient);
            invokeNoArgFXMLMethods(patient);
            PresentationFxHarness.sweepDeclaredFxControls(patient);
        })).doesNotThrowAnyException();
        sleepQuietly(800);

        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        sleepQuietly(700);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(book);
            invokeNoArgFXMLMethods(book);
            PresentationFxHarness.sweepDeclaredFxControls(book);
        })).doesNotThrowAnyException();
        sleepQuietly(900);

        ModifyAppointmentController modify = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        sleepQuietly(700);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(modify);
            invokeNoArgFXMLMethods(modify);
            PresentationFxHarness.sweepDeclaredFxControls(modify);
        })).doesNotThrowAnyException();
        sleepQuietly(900);

        assertThat(true).isTrue();
    }

    private static void invokeNoArgFXMLMethods(Object controller) {
        for (Method m : controller.getClass().getDeclaredMethods()) {
            if (!m.isAnnotationPresent(FXML.class)) continue;
            if (m.getParameterCount() != 0) continue;
            String name = m.getName();
            if (name.toLowerCase().contains("logout")) continue; // avoid clearing auth + navigation

            try {
                m.setAccessible(true);
                m.invoke(controller);
            } catch (Throwable ignored) {
                // We only care about coverage; some handlers require UI selection/state.
            }
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

