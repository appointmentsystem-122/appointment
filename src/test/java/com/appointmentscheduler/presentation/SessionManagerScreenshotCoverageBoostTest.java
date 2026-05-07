package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Focused tests for SessionManager branches shown as uncovered in SonarCloud. */
@ResourceLock("SessionManagerSingleton")
class SessionManagerScreenshotCoverageBoostTest {

    @BeforeEach
    void init() throws Exception {
        JavaFxTestSupport.initPlatform();
        System.setProperty("app.test.autoDialogs", "true");
        applyAppConfig(15, 13);
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);
        SessionManager.getInstance().unregister();
    }

    @AfterEach
    void cleanup() throws Exception {
        SessionManager.getInstance().unregister();
        System.clearProperty("app.test.autoDialogs");
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);
        reloadAppConfig();
    }

    @Test
    void lifecycleRegisterNullAndExplicitActivityBranches_areCovered() throws Exception {
        SessionManager manager = SessionManager.getInstance();

        assertThatCode(() -> manager.registerScene(null)).doesNotThrowAnyException();
        assertThatCode(manager::startTracking).doesNotThrowAnyException();

        Field timer = SessionManager.class.getDeclaredField("timer");
        timer.setAccessible(true);
        assertThat(timer.get(manager)).isNull();

        Field lastActivity = SessionManager.class.getDeclaredField("lastActivity");
        Field warningShown = SessionManager.class.getDeclaredField("warningShown");
        lastActivity.setAccessible(true);
        warningShown.setAccessible(true);

        LocalDateTime old = LocalDateTime.now().minusHours(2);
        lastActivity.set(manager, old);
        warningShown.set(manager, true);

        Method updateActivity = SessionManager.class.getDeclaredMethod("updateActivity", Event.class);
        updateActivity.setAccessible(true);
        updateActivity.invoke(manager, new Event(Event.ANY));

        assertThat((LocalDateTime) lastActivity.get(manager)).isAfter(old);
        assertThat((boolean) warningShown.get(manager)).isFalse();

        Scene scene = runOnFx(() -> new Scene(new StackPane(), 100, 80));
        assertThatCode(() -> manager.registerScene(scene)).doesNotThrowAnyException();
        manager.extendSession();
        assertThat((boolean) warningShown.get(manager)).isFalse();
    }

    @Test
    void checkTimeout_warningActionUsesAutoStayAndExtendsSession() throws Exception {
        applyAppConfig(10, 1);
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u-warn", "Warn User", "warn@example.com", "pw"));
        ApplicationContext.setAuthService(auth);

        SessionManager manager = SessionManager.getInstance();
        setPrivate(manager, "lastActivity", LocalDateTime.now().minusMinutes(2));
        setPrivate(manager, "warningShown", false);

        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);
        assertThatCode(() -> checkTimeout.invoke(manager)).doesNotThrowAnyException();
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);

        Field warningShown = SessionManager.class.getDeclaredField("warningShown");
        warningShown.setAccessible(true);
        assertThat((boolean) warningShown.get(manager)).isFalse();
    }

    @Test
    void checkTimeout_logoutActionAuditsLogsOutAndReturnsToLogin() throws Exception {
        applyAppConfig(0, 0);
        User user = new User("u-expired", "Expired User", "expired@example.com", "pw");
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(user);
        AuditLogService audit = mock(AuditLogService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(audit);

        SessionManager manager = SessionManager.getInstance();
        setPrivate(manager, "lastActivity", LocalDateTime.now().minusMinutes(30));
        setPrivate(manager, "warningShown", true);

        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(() -> checkTimeout.invoke(manager)).doesNotThrowAnyException();
            JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);

            verify(audit).log(user, "LOGOUT", "Session expired (inactivity)");
            verify(auth).logout();
            main.verify(() -> MainApp.loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin()));
        }
    }

    private static <T> T runOnFx(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        javafx.application.Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    private static void setPrivate(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void applyAppConfig(int timeout, int warning) throws Exception {
        Properties p = new Properties();
        p.setProperty("session.timeoutMinutes", String.valueOf(timeout));
        p.setProperty("session.warningMinutes", String.valueOf(warning));
        Method apply = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        apply.setAccessible(true);
        apply.invoke(null, p);
    }

    private static void reloadAppConfig() throws Exception {
        Method reload = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("reloadClasspathPropertiesForTest");
        reload.setAccessible(true);
        reload.invoke(null);
    }
}
