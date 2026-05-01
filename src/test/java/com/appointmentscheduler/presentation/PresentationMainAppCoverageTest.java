package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;

class PresentationMainAppCoverageTest {

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

        forceAuthCurrentUser(new Administrator(
                "admin-" + System.nanoTime(),
                "Admin",
                "admin@admin.com",
                "pw"
        ));
    }

    @Test
    void mainApp_loadScreen_and_errorHandling_executeCompletely() {
        assertThatCode(() -> runOnFxVoid(() -> {
            // Admin
            forceAuthCurrentUser(new Administrator(
                    "admin-" + System.nanoTime(),
                    "Admin",
                    "admin@admin.com",
                    "pw"
            ));
            MainApp.loadScreen(ScreenConstants.FXML_ADMIN_DASHBOARD, ScreenConstants.titleAdminDashboard());
        })).doesNotThrowAnyException();
        waitForLoadScreenFinished();

        assertThatCode(() -> runOnFxVoid(() -> {
            // Patient
            forceAuthCurrentUser(new User(
                    "user-" + System.nanoTime(),
                    "Patient",
                    "customer@example.com",
                    "pw"
            ));
            MainApp.loadScreen(ScreenConstants.FXML_PATIENT_DASHBOARD, ScreenConstants.titlePatientDashboard());
        })).doesNotThrowAnyException();
        waitForLoadScreenFinished();

        // Invalid fxml => covers messageOf() + showErrorScene() + error handling path.
        assertThatCode(() -> runOnFxVoid(() -> MainApp.loadScreen("ThisDoesNotExist.fxml", "Broken"))).doesNotThrowAnyException();
        waitForLoadScreenFinished();

        // Perform logout => covers performLogout() + dialog helper + redirect to login screen.
        assertThatCode(() -> runOnFxVoid(() -> {
            com.appointmentscheduler.domain.User currentUser = ApplicationContext.getAuthService().getCurrentUser();
            MainApp.performLogout(stage.getScene() != null ? stage.getScene().getWindow() : null, currentUser);
        })).doesNotThrowAnyException();
        waitForLoadScreenFinished();
    }

    private static void waitForLoadScreenFinished() {
        try {
            Field f = MainApp.class.getDeclaredField("loadScreenInProgress");
            f.setAccessible(true);
            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                Object v = f.get(null);
                if (v instanceof Boolean b && !b) return;
                Thread.sleep(50);
            }
        } catch (Throwable ignored) {
            // ignore
        }
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

