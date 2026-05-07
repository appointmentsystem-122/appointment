package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Focused tests for MainApp branches visible in the SonarCloud new-code coverage list. */
@ResourceLock("MainAppStatics")
class MainAppScreenshotCoverageBoostTest {

    @BeforeEach
    void initFx() throws Exception {
        JavaFxTestSupport.initPlatform();
        System.setProperty("app.test.autoDialogs", "true");
        setPrimaryStage(null);
        setLoadScreenInProgress(false);
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);
    }

    @AfterEach
    void cleanup() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        setLoadScreenInProgress(false);
        setPrimaryStage(null);
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);
    }

    @Test
    void looksLikeBcryptHash_allBranches_areCovered() throws Exception {
        Method method = MainApp.class.getDeclaredMethod("looksLikeBcryptHash", String.class);
        method.setAccessible(true);

        assertThat((boolean) method.invoke(null, new Object[]{null})).isFalse();
        assertThat((boolean) method.invoke(null, "short")).isFalse();
        assertThat((boolean) method.invoke(null, "$2a$10$abcdefghijklmnopqrstuv012345678901234567890123456789")).isTrue();
        assertThat((boolean) method.invoke(null, "$2b$10$abcdefghijklmnopqrstuv012345678901234567890123456789")).isTrue();
        assertThat((boolean) method.invoke(null, "$2y$10$abcdefghijklmnopqrstuv012345678901234567890123456789")).isTrue();
        assertThat((boolean) method.invoke(null, "$2x$10$abcdefghijklmnopqrstuv012345678901234567890123456789")).isFalse();
    }

    @Test
    void performLogout_withNullOwnerAndCurrentUser_auditsLogsOutAndLoadsLogin() throws Exception {
        AuthService auth = mock(AuthService.class);
        AuditLogService audit = mock(AuditLogService.class);
        User user = new User("u-logout", "Logout User", "logout@example.com", "pw");
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(audit);

        Stage stage = runOnFx(() -> {
            Stage s = new Stage();
            s.setScene(new Scene(new StackPane(), 80, 60));
            return s;
        });
        setPrimaryStage(stage);

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);

            MainApp.performLogout(null, user);

            verify(audit).log(user, "LOGOUT", "User logged out from application");
            verify(auth).logout();
            main.verify(() -> MainApp.loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin()));
        }
    }

    @Test
    void performLogout_nullUserSkipsAuditButStillLogsOut() {
        AuthService auth = mock(AuthService.class);
        AuditLogService audit = mock(AuditLogService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(audit);

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);

            MainApp.performLogout(null, null);

            verify(auth).logout();
            main.verify(() -> MainApp.loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin()));
        }
    }

    @Test
    void loadScreen_reentryGuardAndMissingFxmlErrorPath_areCovered() throws Exception {
        setLoadScreenInProgress(true);
        assertThatCode(() -> MainApp.loadScreen("anything.fxml", "Ignored")).doesNotThrowAnyException();
        setLoadScreenInProgress(false);

        Stage stage = runOnFx(Stage::new);
        setPrimaryStage(stage);

        assertThatCode(() -> runOnFxVoid(() -> MainApp.loadScreen("missing-screen-for-coverage.fxml", "Missing")))
                .doesNotThrowAnyException();
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);
        assertThat(stage.getScene()).isNotNull();
        assertThat(stage.getTitle()).contains("Error");
    }

    @Test
    void showErrorScene_nullStageAndValidStageBranches_areCovered() throws Exception {
        Method showError = MainApp.class.getDeclaredMethod("showErrorScene", String.class);
        showError.setAccessible(true);

        setPrimaryStage(null);
        assertThatCode(() -> showError.invoke(null, "no stage available")).doesNotThrowAnyException();

        Stage stage = runOnFx(Stage::new);
        setPrimaryStage(stage);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                showError.invoke(null, "visible error");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();
        assertThat(stage.getScene()).isNotNull();
        assertThat(stage.getTitle()).contains("Error");
    }

    @Test
    void defaultUncaughtExceptionHandlerInstalledByStart_canRenderErrorScene() throws Exception {
        Stage stage = runOnFx(Stage::new);
        MainApp app = new MainApp();

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenThrow(new IllegalStateException("forced load failure"));
            assertThatCode(() -> runOnFxVoid(() -> {
                try {
                    app.start(stage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })).doesNotThrowAnyException();
        }

        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        assertThat(handler).isNotNull();
        handler.uncaughtException(Thread.currentThread(), new RuntimeException("boom from test"));
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);
        assertThat(stage.getScene()).isNotNull();
    }

    private static void setPrimaryStage(Stage stage) throws Exception {
        Field field = MainApp.class.getDeclaredField("primaryStage");
        field.setAccessible(true);
        field.set(null, stage);
    }

    private static void setLoadScreenInProgress(boolean value) throws Exception {
        Field field = MainApp.class.getDeclaredField("loadScreenInProgress");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static <T> T runOnFx(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void runOnFxVoid(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX task timed out");
        }
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }
}
