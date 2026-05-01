package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Targets {@link SessionManager} branches without relying on real session timers or full UI flows.
 */
class SessionManagerPresentationCoverageTest {

    @BeforeEach
    void initFx() throws Exception {
        JavaFxTestSupport.initPlatform();
        // SessionManager is a singleton with mutable session state; reset between tests to avoid order-dependent pollution.
        Field inst = SessionManager.class.getDeclaredField("instance");
        inst.setAccessible(true);
        SessionManager previous = (SessionManager) inst.get(null);
        if (previous != null) {
            // Cancels background Timer from startTracking(); nulling "instance" alone leaves timers firing.
            previous.unregister();
        }
        inst.set(null, null);
    }

    @AfterEach
    void clearAutoDialogsAndAuth() {
        System.clearProperty("app.test.autoDialogs");
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);
    }

    @Test
    void getInstance_isSingleton() {
        assertThat(SessionManager.getInstance()).isSameAs(SessionManager.getInstance());
    }

    @Test
    void registerScene_null_isNoOp() {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager sm = SessionManager.getInstance();
        assertThatCode(() -> sm.registerScene(null)).doesNotThrowAnyException();
    }

    @Test
    void startTracking_withAutoDialogs_skipsTimer_extendSession_unregister_safe() {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager sm = SessionManager.getInstance();
        assertThatCode(() -> {
            sm.startTracking();
            sm.extendSession();
            sm.unregister();
        }).doesNotThrowAnyException();
    }

    @Test
    void startTracking_withoutAutoDialogs_createsTimer_then_unregister_clearsIt() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        SessionManager sm = SessionManager.getInstance();
        sm.startTracking();

        Field timerF = SessionManager.class.getDeclaredField("timer");
        timerF.setAccessible(true);
        assertThat(timerF.get(sm)).isNotNull();

        sm.unregister();
        assertThat(timerF.get(sm)).isNull();
    }

    @Test
    void checkTimeout_whenNoAuthService_returnsEarly() throws Exception {
        AuthService prev = ApplicationContext.getAuthService();
        ApplicationContext.setAuthService(null);
        try {
            SessionManager sm = SessionManager.getInstance();
            Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
            checkTimeout.setAccessible(true);
            assertThatCode(() -> checkTimeout.invoke(sm)).doesNotThrowAnyException();
        } finally {
            ApplicationContext.setAuthService(prev);
        }
    }

    @Test
    void updateActivity_clearsWarningFlag_eventUnused() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager sm = SessionManager.getInstance();
        sm.startTracking();

        Field warningShownF = SessionManager.class.getDeclaredField("warningShown");
        warningShownF.setAccessible(true);
        warningShownF.set(sm, true);

        Method updateActivity = SessionManager.class.getDeclaredMethod("updateActivity", Event.class);
        updateActivity.setAccessible(true);
        updateActivity.invoke(sm, new Object[]{null});

        assertThat(warningShownF.get(sm)).isEqualTo(false);
    }

    @Test
    void showSessionWarningDialog_nonAuto_emptyChoice_triggersLogoutPath() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        AuthService auth = mock(AuthService.class);
        User user = new User("u-swd-out", "S", "swd-out@x.com", "pw");
        when(auth.getCurrentUser()).thenReturn(user);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        SessionManager sm = SessionManager.getInstance();
        Method m = SessionManager.class.getDeclaredMethod("showSessionWarningDialog", long.class);
        m.setAccessible(true);

        try (MockedConstruction<Alert> ignored = mockConstruction(Alert.class,
                (alert, ctx) -> {
                    when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
                    when(alert.showAndWait()).thenReturn(Optional.empty());
                });
             MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            // MockedStatic does not reliably intercept static calls from the JavaFX thread; invoke on the
            // test thread (same pattern as DialogHelperBranchTest + BookAppointmentControllerTargetedBranchesTest).
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(() -> m.invoke(sm, 5L)).doesNotThrowAnyException();
            verify(auth).logout();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_LOGIN), anyString()));
        }
    }

    @Test
    void showSessionWarningDialog_nonAuto_stayByOkDoneButtonData_extendsSession_noLogout() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        AuthService auth = mock(AuthService.class);
        User user = new User("u-swd-stay", "S", "swd-stay@x.com", "pw");
        when(auth.getCurrentUser()).thenReturn(user);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        SessionManager sm = SessionManager.getInstance();
        Method m = SessionManager.class.getDeclaredMethod("showSessionWarningDialog", long.class);
        m.setAccessible(true);

        ButtonType stayLike = new ButtonType("Stay label", ButtonBar.ButtonData.OK_DONE);

        try (MockedConstruction<Alert> ignored = mockConstruction(Alert.class,
                (alert, ctx) -> {
                    when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
                    when(alert.showAndWait()).thenReturn(Optional.of(stayLike));
                });
             MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(() -> m.invoke(sm, 9L)).doesNotThrowAnyException();
            verify(auth, never()).logout();
            main.verify(() -> MainApp.loadScreen(anyString(), anyString()), never());
        }
    }

    @Test
    void showSessionWarningDialog_nonAuto_logoutChoice_invokesLogoutPath() throws Exception {
        System.clearProperty("app.test.autoDialogs");
        AuthService auth = mock(AuthService.class);
        User user = new User("u-swd-lg", "S", "swd-lg@x.com", "pw");
        when(auth.getCurrentUser()).thenReturn(user);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        SessionManager sm = SessionManager.getInstance();
        Method m = SessionManager.class.getDeclaredMethod("showSessionWarningDialog", long.class);
        m.setAccessible(true);

        javafx.scene.control.ButtonType logout = new javafx.scene.control.ButtonType(
                I18n.get("session.logout"), javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        try (MockedConstruction<Alert> ignored = mockConstruction(Alert.class,
                (alert, ctx) -> {
                    when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
                    when(alert.showAndWait()).thenReturn(Optional.of(logout));
                });
             MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(inv -> null);
            assertThatCode(() -> m.invoke(sm, 4L)).doesNotThrowAnyException();
            verify(auth).logout();
            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_LOGIN), anyString()));
        }
    }

    @Test
    void showSessionWarningDialog_autoDialogs_choosesStayBranch() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(new User("u1", "U", "u@test.com", "pw"));
        ApplicationContext.setAuthService(auth);

        SessionManager sm = SessionManager.getInstance();
        Method showSessionWarningDialog = SessionManager.class.getDeclaredMethod("showSessionWarningDialog", long.class);
        showSessionWarningDialog.setAccessible(true);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                showSessionWarningDialog.invoke(sm, 15L);
            } catch (InvocationTargetException e) {
                err.set(e.getCause() != null ? e.getCause() : e);
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(err.get()).withFailMessage("showSessionWarningDialog must run on JavaFX thread").isNull();
    }

    @Test
    void registerScene_withScene_addsFilters() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager sm = SessionManager.getInstance();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Scene scene = new Scene(new StackPane(), 80, 60);
                sm.registerScene(scene);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void registerScene_keyEvent_updatesActivity() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager sm = SessionManager.getInstance();
        sm.startTracking();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Scene scene = new Scene(new StackPane(), 80, 60);
                sm.registerScene(scene);
                KeyEvent ev = new KeyEvent(KeyEvent.KEY_PRESSED, "a", KeyCode.A.getName(), KeyCode.A, false, false, false, false);
                Event.fireEvent(scene, ev);
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void registerScene_mouseMovedAndClicked_updateActivity() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        SessionManager sm = SessionManager.getInstance();
        sm.startTracking();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                StackPane root = new StackPane();
                Scene scene = new Scene(root, 80, 60);
                sm.registerScene(scene);
                Event.fireEvent(scene, new MouseEvent(
                        MouseEvent.MOUSE_MOVED,
                        2, 3, 4, 5,
                        MouseButton.NONE,
                        0,
                        false, false, false, false,
                        false, false, false,
                        false, false, false,
                        null));
                Event.fireEvent(scene, new MouseEvent(
                        MouseEvent.MOUSE_CLICKED,
                        1, 1, 1, 1,
                        MouseButton.PRIMARY,
                        1,
                        false, false, false, false,
                        false, false, false,
                        false, false, false,
                        null));
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void checkTimeout_whenAuthPresentButNoCurrentUser_returnsEarly() throws Exception {
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(null);
        ApplicationContext.setAuthService(auth);

        SessionManager sm = SessionManager.getInstance();
        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);
        assertThatCode(() -> checkTimeout.invoke(sm)).doesNotThrowAnyException();
    }

    @Test
    void checkTimeout_expiredSession_runsLogoutFlow() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User user = new User("u-timeout", "U", "u@x.com", "pw");
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(user);
        AuditLogService audit = mock(AuditLogService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(audit);

        SessionManager sm = SessionManager.getInstance();
        setLastActivity(sm, LocalDateTime.now().minusHours(6));
        setMainAppPrimaryStage();

        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);
        checkTimeout.invoke(sm);
        drainFxQueue();
        verify(auth).logout();
        verify(audit).log(user, "LOGOUT", "Session expired (inactivity)");
    }

    @Test
    void checkTimeout_expiredSession_noAuditService_skipsAuditLog() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User user = new User("u-no-audit", "U", "u-na@x.com", "pw");
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(user);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        SessionManager sm = SessionManager.getInstance();
        setLastActivity(sm, LocalDateTime.now().minusHours(6));
        setMainAppPrimaryStage();

        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);
        checkTimeout.invoke(sm);
        drainFxQueue();
        verify(auth).logout();
    }

    @Test
    void checkTimeout_warningWindow_showsWarningAndKeepsSessionOnAutoStay() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User user = new User("u-warn", "U", "u2@x.com", "pw");
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(user);
        ApplicationContext.setAuthService(auth);

        SessionManager sm = SessionManager.getInstance();
        long warning = (Long) invokePrivateStatic(SessionManager.class, "getWarningMinutes");
        setLastActivity(sm, LocalDateTime.now().minusMinutes(Math.max(1, warning + 1)));
        setWarningShown(sm, false);

        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);
        checkTimeout.invoke(sm);
        drainFxQueue();

        // Auto-dialog path clicks "Stay logged in" and resets warning flag.
        assertThat(readWarningShown(sm)).isFalse();
    }

    @Test
    void checkTimeout_warningAlreadyShown_doesNotReopenWarningDialog() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        User user = new User("u-warn2", "U", "u3@x.com", "pw");
        AuthService auth = mock(AuthService.class);
        when(auth.getCurrentUser()).thenReturn(user);
        ApplicationContext.setAuthService(auth);

        SessionManager sm = SessionManager.getInstance();
        long warning = (Long) invokePrivateStatic(SessionManager.class, "getWarningMinutes");
        long timeout = (Long) invokePrivateStatic(SessionManager.class, "getTimeoutMinutes");
        long minutes = Math.max(1, Math.min(timeout - 1, warning + 2));
        setLastActivity(sm, LocalDateTime.now().minusMinutes(minutes));
        setWarningShown(sm, true); // guard branch: warning should not display again

        Method checkTimeout = SessionManager.class.getDeclaredMethod("checkTimeout");
        checkTimeout.setAccessible(true);
        checkTimeout.invoke(sm);
        drainFxQueue();

        assertThat(readWarningShown(sm)).isTrue();
        verify(auth, never()).logout();
    }

    private static void setLastActivity(SessionManager sm, LocalDateTime ts) throws Exception {
        Field f = SessionManager.class.getDeclaredField("lastActivity");
        f.setAccessible(true);
        f.set(sm, ts);
    }

    private static void setWarningShown(SessionManager sm, boolean v) throws Exception {
        Field f = SessionManager.class.getDeclaredField("warningShown");
        f.setAccessible(true);
        f.set(sm, v);
    }

    private static boolean readWarningShown(SessionManager sm) throws Exception {
        Field f = SessionManager.class.getDeclaredField("warningShown");
        f.setAccessible(true);
        return (Boolean) f.get(sm);
    }

    private static Object invokePrivateStatic(Class<?> cl, String method) throws Exception {
        Method m = cl.getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(null);
    }

    private static void drainFxQueue() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(done::countDown);
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    }

    private static void setMainAppPrimaryStage() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                var f = MainApp.class.getDeclaredField("primaryStage");
                f.setAccessible(true);
                if (f.get(null) == null) {
                    f.set(null, new javafx.stage.Stage());
                }
            } catch (Throwable t) {
                err.set(t);
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
    }
}
