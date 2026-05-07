package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused coverage for the presentation classes highlighted in the SonarCloud screenshot.
 */
@ResourceLock("ApplicationContextServices")
class PresentationScreenshotCoverageBoostTest {

    private final AuthService originalAuthService = ApplicationContext.getAuthService();
    private final AuditLogService originalAuditLogService = ApplicationContext.getAuditLogService();

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void restoreGlobalState() throws Exception {
        ApplicationContext.setAuthService(originalAuthService);
        ApplicationContext.setAuditLogService(originalAuditLogService);
        System.clearProperty("app.test.autoDialogs");
        setStaticField(MainApp.class, "primaryStage", null);
        setStaticField(MainApp.class, "loadScreenInProgress", false);
        SessionManager.getInstance().unregister();
    }

    @Test
    void mainApp_privateMessageHelpers_coverNullBlankCauseAndHashBranches() throws Exception {
        Method messageOf = MainApp.class.getDeclaredMethod("messageOf", Throwable.class);
        messageOf.setAccessible(true);
        Method looksLikeBcryptHash = MainApp.class.getDeclaredMethod("looksLikeBcryptHash", String.class);
        looksLikeBcryptHash.setAccessible(true);

        assertThat((String) messageOf.invoke(null, new Object[]{null})).isEqualTo("Unknown error");
        assertThat((String) messageOf.invoke(null, new RuntimeException("direct message"))).isEqualTo("direct message");
        assertThat((String) messageOf.invoke(null, new RuntimeException(" ", new IllegalStateException("cause message"))))
                .isEqualTo("cause message");
        assertThat((String) messageOf.invoke(null, new RuntimeException(" "))).isEqualTo("RuntimeException");

        assertThat((Boolean) looksLikeBcryptHash.invoke(null, new Object[]{null})).isFalse();
        assertThat((Boolean) looksLikeBcryptHash.invoke(null, "short")).isFalse();
        assertThat((Boolean) looksLikeBcryptHash.invoke(null, "$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890")).isTrue();
        assertThat((Boolean) looksLikeBcryptHash.invoke(null, "$2b$10$abcdefghijklmnopqrstuv123456789012345678901234567890")).isTrue();
        assertThat((Boolean) looksLikeBcryptHash.invoke(null, "$2y$10$abcdefghijklmnopqrstuv123456789012345678901234567890")).isTrue();
        assertThat((Boolean) looksLikeBcryptHash.invoke(null, "plain-password-value")).isFalse();
    }

    @Test
    void mainApp_stylesheetsAndErrorScene_coverSafeNoOpPaths() throws Exception {
        Method addStylesheetsSafely = MainApp.class.getDeclaredMethod("addStylesheetsSafely", Scene.class);
        addStylesheetsSafely.setAccessible(true);
        Method addStylesheetIfPresent = MainApp.class.getDeclaredMethod("addStylesheetIfPresent", Scene.class, String.class);
        addStylesheetIfPresent.setAccessible(true);
        Method showErrorScene = MainApp.class.getDeclaredMethod("showErrorScene", String.class);
        showErrorScene.setAccessible(true);

        JavaFxTestSupport.runOnFxThread(() -> {
            Scene scene = new Scene(new StackPane(), 10, 10);
            assertThatCode(() -> invoke(addStylesheetsSafely, null, scene)).doesNotThrowAnyException();
            assertThatCode(() -> invoke(addStylesheetIfPresent, null, scene, "/missing-style-sheet.css"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> invoke(showErrorScene, null, "ignored because primaryStage is null"))
                    .doesNotThrowAnyException();
            assertThat(scene.getStylesheets()).anyMatch(s -> s.contains("application-minimal.css"));
        });
    }

    @Test
    void mainApp_showDatabaseWarning_coversAutoModeWithStageAndErrorDetail() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        Method warning = MainApp.class.getDeclaredMethod("showDatabaseNotConnectedWarning", String.class);
        warning.setAccessible(true);

        JavaFxTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane(), 20, 20));
            setStaticFieldUnchecked(MainApp.class, "primaryStage", stage);

            MainApp app = new MainApp();
            assertThatCode(() -> invoke(warning, app, "Connection refused"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> invoke(warning, app, "   "))
                    .doesNotThrowAnyException();
        });
    }

    @Test
    void mainApp_performLogout_cancelStopsBeforeAuthLogout() {
        AuthService auth = mock(AuthService.class);
        ApplicationContext.setAuthService(auth);

        try (MockedStatic<DialogHelper> dialogs = mockStatic(DialogHelper.class)) {
            dialogs.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(false);

            MainApp.performLogout(null, new User("u-cancel", "Cancel", "cancel@example.com", "pw"));

            verify(auth, never()).logout();
        }
    }

    @Test
    void sessionManager_registerSceneAndSyntheticActivity_updateLastActivity() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager manager = SessionManager.getInstance();
        manager.startTracking();

        Field lastActivity = SessionManager.class.getDeclaredField("lastActivity");
        lastActivity.setAccessible(true);
        LocalDateTime oldTime = LocalDateTime.now().minusHours(3);
        lastActivity.set(manager, oldTime);

        JavaFxTestSupport.runOnFxThread(() -> {
            Scene scene = new Scene(new StackPane(), 10, 10);
            manager.registerScene(scene);
            scene.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "a", "a", KeyCode.A, false, false, false, false));
        });

        assertThat((LocalDateTime) lastActivity.get(manager)).isAfter(oldTime);
    }

    @Test
    void sessionManager_showWarning_autoStayAndLogoutBranchWithNullAuth_areSafe() throws Exception {
        Method warning = SessionManager.class.getDeclaredMethod("showSessionWarningDialog", long.class);
        warning.setAccessible(true);
        SessionManager manager = SessionManager.getInstance();

        System.setProperty("app.test.autoDialogs", "true");
        assertThatCode(() -> warning.invoke(manager, 3L)).doesNotThrowAnyException();

        System.clearProperty("app.test.autoDialogs");
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);

        try (MockedConstruction<Alert> alerts = org.mockito.Mockito.mockConstruction(Alert.class,
                (alert, context) -> {
                    when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
                    when(alert.showAndWait()).thenReturn(java.util.Optional.empty());
                });
             MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(invocation -> null);
            assertThatCode(() -> warning.invoke(manager, 0L)).doesNotThrowAnyException();
        }
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setStaticField(Class<?> type, String fieldName, Object value) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void setStaticFieldUnchecked(Class<?> type, String fieldName, Object value) {
        try {
            setStaticField(type, fieldName, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
