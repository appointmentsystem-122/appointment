package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.AuditLogService;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.PasswordHasher;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.events.AppointmentEvent;
import com.appointmentscheduler.persistence.UserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

class MainAppInternalsCoverageTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void resetAppConfig() throws Exception {
        Method reload = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("reloadClasspathPropertiesForTest");
        reload.setAccessible(true);
        reload.invoke(null);
        ApplicationContext.setAuthService(null);
        ApplicationContext.setAuditLogService(null);
    }

    @Test
    void initializeServices_withInMemoryBranch_executesCoreSetup() throws Exception {
        // Force database off to drive the in-memory branch deterministically.
        Properties p = new Properties();
        p.setProperty("database.enabled", "false");
        Method apply = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        apply.setAccessible(true);
        apply.invoke(null, p);

        MainApp app = new MainApp();
        Method init = MainApp.class.getDeclaredMethod("initializeServices");
        init.setAccessible(true);

        assertThatCode(() -> init.invoke(app)).doesNotThrowAnyException();
        assertThat(ApplicationContext.getAuthService()).isNotNull();
        assertThat(ApplicationContext.getScheduleService()).isNotNull();
        assertThat(ApplicationContext.getBookingService()).isNotNull();
    }

    @Test
    void messageOf_and_dbWarning_paths_are_callable() throws Exception {
        Method messageOf = MainApp.class.getDeclaredMethod("messageOf", Throwable.class);
        messageOf.setAccessible(true);

        String m1 = (String) messageOf.invoke(null, new IllegalArgumentException("x"));
        String m2 = (String) messageOf.invoke(null, new RuntimeException(new IllegalStateException("cause-msg")));
        String m3 = (String) messageOf.invoke(null, new RuntimeException((String) null));
        String m4 = (String) messageOf.invoke(null, new Object[]{null});

        assertThat(m1).isEqualTo("x");
        assertThat(m2).contains("cause-msg");
        assertThat(m3).isEqualTo("RuntimeException");
        assertThat(m4).isEqualTo("Unknown error");

        // Alert must be built on the FX thread; skip showAndWait so the FX runnable can finish (DialogHelper test mode).
        Method warn = MainApp.class.getDeclaredMethod("showDatabaseNotConnectedWarning", String.class);
        warn.setAccessible(true);
        MainApp app = new MainApp();
        String prevAuto = System.getProperty("app.test.autoDialogs");
        try {
            System.setProperty("app.test.autoDialogs", "true");
            assertThatCode(() -> runOnFxVoid(() -> {
                try {
                    warn.invoke(app, "db down");
                    warn.invoke(app, (Object) null);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            })).doesNotThrowAnyException();
        } finally {
            if (prevAuto == null) {
                System.clearProperty("app.test.autoDialogs");
            } else {
                System.setProperty("app.test.autoDialogs", prevAuto);
            }
        }
    }

    @Test
    void staticUiHelpers_showErrorAndStylesheetPaths_areCovered() throws Exception {
        Stage stage = runOnFx(Stage::new);
        var f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        f.set(null, stage);

        Method showError = MainApp.class.getDeclaredMethod("showErrorScene", String.class);
        showError.setAccessible(true);
        assertThatCode(() -> runOnFxVoid(() -> {
            try {
                showError.invoke(null, "test error");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).doesNotThrowAnyException();

        Method addStylesheet = MainApp.class.getDeclaredMethod("addStylesheetIfPresent", Scene.class, String.class);
        addStylesheet.setAccessible(true);
        Method addStylesheetsSafely = MainApp.class.getDeclaredMethod("addStylesheetsSafely", Scene.class);
        addStylesheetsSafely.setAccessible(true);

        Scene scene = runOnFx(() -> new Scene(new VBox(), 320, 200));
        assertThatCode(() -> addStylesheet.invoke(null, scene, "/com/appointmentscheduler/presentation/application-minimal.css"))
                .doesNotThrowAnyException();
        assertThatCode(() -> addStylesheet.invoke(null, scene, "/missing/not-found.css"))
                .doesNotThrowAnyException();
        assertThatCode(() -> addStylesheetsSafely.invoke(null, scene)).doesNotThrowAnyException();
    }

    @Test
    void ensureDefaultAdminUser_branches_areCovered() throws Exception {
        MainApp app = new MainApp();
        Method m = MainApp.class.getDeclaredMethod("ensureDefaultAdminUser", UserRepository.class);
        m.setAccessible(true);

        // Missing admin -> create default.
        UserRepository repo1 = mock(UserRepository.class);
        when(repo1.findByEmail("admin@admin.com")).thenReturn(Optional.empty());
        try (var ph = mockStatic(PasswordHasher.class)) {
            ph.when(() -> PasswordHasher.hash(anyString())).thenReturn("$2a$mock");
            m.invoke(app, repo1);
            verify(repo1).save(any(User.class));
        }

        // Existing non-admin -> convert to Administrator.
        UserRepository repo2 = mock(UserRepository.class);
        when(repo2.findByEmail("admin@admin.com")).thenReturn(Optional.of(new User("u1", "N", "admin@admin.com", "pw")));
        try (var ph = mockStatic(PasswordHasher.class)) {
            ph.when(() -> PasswordHasher.hash(anyString())).thenReturn("$2a$mock");
            m.invoke(app, repo2);
            verify(repo2).save(any(Administrator.class));
        }

        // Existing admin with matching password -> no save.
        UserRepository repo3 = mock(UserRepository.class);
        when(repo3.findByEmail("admin@admin.com"))
                .thenReturn(Optional.of(new Administrator("a1", "A", "admin@admin.com", "$2a$good")));
        try (var ph = mockStatic(PasswordHasher.class)) {
            ph.when(() -> PasswordHasher.verify(anyString(), anyString())).thenReturn(true);
            m.invoke(app, repo3);
            verify(repo3, never()).save(any(User.class));
        }

        // Existing admin, verify false, force reset true -> save.
        UserRepository repo4 = mock(UserRepository.class);
        when(repo4.findByEmail("admin@admin.com"))
                .thenReturn(Optional.of(new Administrator("a2", "A", "admin@admin.com", "$2a$hash")));
        try (var ph = mockStatic(PasswordHasher.class); var cfg = mockStatic(AppConfig.class)) {
            ph.when(() -> PasswordHasher.verify(anyString(), anyString())).thenReturn(false);
            ph.when(() -> PasswordHasher.hash(anyString())).thenReturn("$2a$reset");
            cfg.when(AppConfig::isForceDefaultAdminPasswordOnStartup).thenReturn(true);
            m.invoke(app, repo4);
            verify(repo4).save(any(Administrator.class));
        }

        // Existing admin, verify false, non-bcrypt stored -> save via nonBcrypt branch.
        UserRepository repo5 = mock(UserRepository.class);
        when(repo5.findByEmail("admin@admin.com"))
                .thenReturn(Optional.of(new Administrator("a3", "A", "admin@admin.com", "plain-text")));
        try (var ph = mockStatic(PasswordHasher.class); var cfg = mockStatic(AppConfig.class)) {
            ph.when(() -> PasswordHasher.verify(anyString(), anyString())).thenReturn(false);
            ph.when(() -> PasswordHasher.hash(anyString())).thenReturn("$2a$reset");
            cfg.when(AppConfig::isForceDefaultAdminPasswordOnStartup).thenReturn(false);
            m.invoke(app, repo5);
            verify(repo5).save(any(Administrator.class));
        }

        // Exception path from repository call is swallowed.
        UserRepository repo6 = mock(UserRepository.class);
        when(repo6.findByEmail("admin@admin.com")).thenThrow(new RuntimeException("repo down"));
        assertThatCode(() -> m.invoke(app, repo6)).doesNotThrowAnyException();
    }

    @Test
    void performLogout_confirmFalseAndTrue_branches() {
        AuthService auth = mock(AuthService.class);
        AuditLogService audit = mock(AuditLogService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(audit);
        User user = new User("u-logout", "U", "u@x.com", "pw");

        try (var dlg = mockStatic(DialogHelper.class); var main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
            dlg.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(false);
            MainApp.performLogout(null, user);
            verify(auth, never()).logout();

            dlg.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(true);
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(i -> null);
            MainApp.performLogout(null, user);
            verify(auth).logout();
            verify(audit).log(user, "LOGOUT", "User logged out from application");
        }
    }

    @Test
    void performLogout_withNullCurrentUser_and_loadScreenReentryGuard() throws Exception {
        AuthService auth = mock(AuthService.class);
        AuditLogService audit = mock(AuditLogService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(audit);

        // currentUser == null branch: should logout + no audit log call.
        try (var dlg = mockStatic(DialogHelper.class); var main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
            dlg.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(true);
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(i -> null);
            MainApp.performLogout(null, null);
            verify(auth).logout();
            verify(audit, never()).log(any(User.class), anyString(), anyString());
        }

        // loadScreen re-entry guard branch: return immediately when in progress=true.
        var inProgress = MainApp.class.getDeclaredField("loadScreenInProgress");
        inProgress.setAccessible(true);
        inProgress.setBoolean(null, true);
        try {
            assertThatCode(() -> MainApp.loadScreen("Missing.fxml", "Ignored")).doesNotThrowAnyException();
        } finally {
            inProgress.setBoolean(null, false);
        }
    }

    @Test
    void loadScreen_missingFxml_hitsCatch_and_resetsInProgressFlag() throws Exception {
        Stage stage = runOnFx(Stage::new);
        var primary = MainApp.class.getDeclaredField("primaryStage");
        primary.setAccessible(true);
        primary.set(null, stage);

        var inProgress = MainApp.class.getDeclaredField("loadScreenInProgress");
        inProgress.setAccessible(true);
        inProgress.setBoolean(null, false);

        assertThatCode(() -> MainApp.loadScreen("DefinitelyMissing.fxml", "Missing")).doesNotThrowAnyException();
        // catch path should always reset guard
        assertThat(inProgress.getBoolean(null)).isFalse();
    }

    @Test
    void looksLikeBcryptHash_branches_are_covered() throws Exception {
        Method m = MainApp.class.getDeclaredMethod("looksLikeBcryptHash", String.class);
        m.setAccessible(true);

        assertThat((Boolean) m.invoke(null, new Object[]{null})).isFalse();
        assertThat((Boolean) m.invoke(null, "short")).isFalse();
        assertThat((Boolean) m.invoke(null, "$2a$10$abcdefghijklmnopqrstuv")).isTrue();
        assertThat((Boolean) m.invoke(null, "$2b$10$abcdefghijklmnopqrstuv")).isTrue();
        assertThat((Boolean) m.invoke(null, "$2y$10$abcdefghijklmnopqrstuv")).isTrue();
        assertThat((Boolean) m.invoke(null, "$9z$10$abcdefghijklmnopqrstuv")).isFalse();
    }

    @Test
    void messageOf_blankMessageAndBlankCause_returnsSimpleNameBranch() throws Exception {
        Method messageOf = MainApp.class.getDeclaredMethod("messageOf", Throwable.class);
        messageOf.setAccessible(true);

        class BlankMsgRuntime extends RuntimeException {
            @Override
            public String getMessage() {
                return "   ";
            }

            @Override
            public synchronized Throwable getCause() {
                return new IllegalStateException("   ");
            }
        }
        Throwable t = new BlankMsgRuntime();
        String out = (String) messageOf.invoke(null, t);
        assertThat(out).isEqualTo("BlankMsgRuntime");
    }

    @Test
    void showDatabaseWarning_offFxThread_hitsCatchBranch() throws Exception {
        Method warn = MainApp.class.getDeclaredMethod("showDatabaseNotConnectedWarning", String.class);
        warn.setAccessible(true);
        MainApp app = new MainApp();
        assertThatCode(() -> warn.invoke(app, "detail")).doesNotThrowAnyException();
    }

    @Test
    void performLogout_whenAuditServiceNull_confirmTrue_stillLogsOut() {
        AuthService auth = mock(AuthService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        try (var dlg = mockStatic(DialogHelper.class);
             var main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
            dlg.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(true);
            main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(i -> null);
            MainApp.performLogout(null, new User("u-null-audit", "U", "u@x.com", "pw"));
            verify(auth).logout();
        }
    }

    @Test
    void performLogout_nullOwner_primaryStageWithoutScene_coversOwnerFallbackTernary() throws Exception {
        AuthService auth = mock(AuthService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        Field f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        Object prev = f.get(null);
        try {
            Stage bare = runOnFx(Stage::new);
            f.set(null, bare);
            try (var dlg = mockStatic(DialogHelper.class);
                 var main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
                dlg.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(true);
                main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(i -> null);
                MainApp.performLogout(null, new User("u-bare-stage", "U", "u@x.com", "pw"));
                verify(auth).logout();
            }
        } finally {
            f.set(null, prev);
        }
    }

    @Test
    void performLogout_nullOwner_primaryStageWithScene_coversOwnerFallbackWindowPath() throws Exception {
        AuthService auth = mock(AuthService.class);
        ApplicationContext.setAuthService(auth);
        ApplicationContext.setAuditLogService(null);

        Field f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        Object prev = f.get(null);
        try {
            Stage stage = runOnFx(Stage::new);
            runOnFxVoid(() -> stage.setScene(new Scene(new VBox(), 200, 120)));
            f.set(null, stage);
            try (var dlg = mockStatic(DialogHelper.class);
                 var main = mockStatic(MainApp.class, CALLS_REAL_METHODS)) {
                dlg.when(() -> DialogHelper.showLogoutConfirmation(anyString())).thenReturn(true);
                main.when(() -> MainApp.loadScreen(anyString(), anyString())).thenAnswer(i -> null);
                MainApp.performLogout(null, new User("u-scene-stage", "U", "u2@x.com", "pw"));
                verify(auth).logout();
            }
        } finally {
            f.set(null, prev);
        }
    }

    @Test
    void messageOf_nullMessageAndNullCause_returnsClassSimpleNameBranch() throws Exception {
        Method messageOf = MainApp.class.getDeclaredMethod("messageOf", Throwable.class);
        messageOf.setAccessible(true);
        class NullMsgRuntime extends RuntimeException {
            NullMsgRuntime() { super((String) null); }

            @Override
            public synchronized Throwable getCause() {
                return null;
            }
        }
        Throwable t = new NullMsgRuntime();
        String out = (String) messageOf.invoke(null, t);
        assertThat(out).isEqualTo("NullMsgRuntime");
    }

    @Test
    void warningDialog_ownerInitAndAutoDialogBranches() throws Exception {
        Method warn = MainApp.class.getDeclaredMethod("showDatabaseNotConnectedWarning", String.class);
        warn.setAccessible(true);

        Stage stage = runOnFx(Stage::new);
        runOnFxVoid(() -> stage.setScene(new Scene(new VBox(), 240, 120)));
        var f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        f.set(null, stage);

        String prevAuto = System.getProperty("app.test.autoDialogs");
        try {
            System.setProperty("app.test.autoDialogs", "true");
            assertThatCode(() -> runOnFxVoid(() -> {
                try {
                    warn.invoke(new MainApp(), "db detail");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })).doesNotThrowAnyException();
        } finally {
            if (prevAuto == null) {
                System.clearProperty("app.test.autoDialogs");
            } else {
                System.setProperty("app.test.autoDialogs", prevAuto);
            }
        }
    }

    @Test
    void syntheticRunLaterErrorHandlers_invocable_forCoverage() throws Exception {
        // javac may emit lambda$2 or lambda$start$2 depending on version; match by parameter types (see MainApp.start catch blocks).
        List<Method> throwableRunnables = mainAppLambdasWithParams(Throwable.class);
        List<Method> stringThrowableRunnables = mainAppLambdasWithParams(String.class, Throwable.class);
        assertThat(throwableRunnables).isNotEmpty();
        assertThat(stringThrowableRunnables).isNotEmpty();

        Stage stage = runOnFx(Stage::new);
        runOnFxVoid(() -> stage.setScene(new Scene(new VBox(), 240, 120)));
        var f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        Object prev = f.get(null);
        try {
            f.set(null, stage);
            for (Method m : throwableRunnables) {
                assertThatCode(() -> m.invoke(receiverFor(m), new RuntimeException("init failed")))
                        .doesNotThrowAnyException();
            }
            for (Method m : stringThrowableRunnables) {
                assertThatCode(() -> m.invoke(receiverFor(m), "load failed", new RuntimeException("x")))
                        .doesNotThrowAnyException();
            }
        } finally {
            f.set(null, prev);
        }
    }

    /**
     * Covers {@code eventPublisher.addListener(e -> { ... })} in {@link MainApp#initializeServices()}
     * (synthetic {@code lambda$7} or {@code lambda$initializeServices$N}): early returns, admin/receptionist vs other actors,
     * all event types, and optional {@link AppNotificationStore}.
     */
    @Test
    void lambda7_appointmentEventListener_coversBranches() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "false");
        Method apply = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        apply.setAccessible(true);
        apply.invoke(null, p);

        MainApp app = new MainApp();
        Method init = MainApp.class.getDeclaredMethod("initializeServices");
        init.setAccessible(true);
        init.invoke(app);

        List<Method> listeners = mainAppLambdasWithParams(AppointmentEvent.class);
        assertThat(listeners).isNotEmpty();

        User patient = new User("p-l7", "Patient", "p-l7@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        IndividualAppointment appt = new IndividualAppointment(patient, slot);
        Administrator admin = new Administrator("a-l7", "Admin", "a-l7@x.com", "pw");
        ReceptionistUser receptionist = new ReceptionistUser("r-l7", "Rec", "r-l7@x.com", "pw");
        DoctorUser doctor = new DoctorUser("d-l7", "Doc", "d-l7@x.com", "pw");

        for (Method listener : listeners) {
            assertThatCode(() -> listener.invoke(receiverFor(listener), (Object) null)).doesNotThrowAnyException();
            assertThatCode(() -> listener.invoke(receiverFor(listener), new AppointmentEvent(AppointmentEvent.Type.CREATED, null, admin, "x")))
                    .doesNotThrowAnyException();
            assertThatCode(() -> listener.invoke(receiverFor(listener), new AppointmentEvent(AppointmentEvent.Type.CREATED, appt, doctor, "x")))
                    .doesNotThrowAnyException();
            assertThatCode(() -> listener.invoke(receiverFor(listener), new AppointmentEvent(AppointmentEvent.Type.CREATED, appt, null, "x")))
                    .doesNotThrowAnyException();

            for (AppointmentEvent.Type evType : AppointmentEvent.Type.values()) {
                assertThatCode(() -> listener.invoke(receiverFor(listener), new AppointmentEvent(evType, appt, admin, "details")))
                        .doesNotThrowAnyException();
            }
            assertThatCode(() -> listener.invoke(receiverFor(listener), new AppointmentEvent(AppointmentEvent.Type.CREATED, appt, receptionist, "r")))
                    .doesNotThrowAnyException();

            AppNotificationStore prev = ApplicationContext.getAppNotificationStore();
            try {
                ApplicationContext.setAppNotificationStore(null);
                assertThatCode(() -> listener.invoke(receiverFor(listener), new AppointmentEvent(AppointmentEvent.Type.CREATED, appt, admin, "n")))
                        .doesNotThrowAnyException();
            } finally {
                ApplicationContext.setAppNotificationStore(prev);
            }
        }
    }

    @Test
    void showErrorScene_whenPrimaryStageNull_returnsWithoutNpe() throws Exception {
        Method showErrorScene = MainApp.class.getDeclaredMethod("showErrorScene", String.class);
        showErrorScene.setAccessible(true);
        Field f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        Object prev = f.get(null);
        try {
            f.set(null, null);
            assertThatCode(() -> showErrorScene.invoke(null, "boom")).doesNotThrowAnyException();
        } finally {
            f.set(null, prev);
        }
    }

    @Test
    void ensureDefaultAdminUser_repositoryThrowsException_isSwallowed() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        when(repo.findByEmail("admin@admin.com")).thenThrow(new RuntimeException("db read failed"));
        MainApp app = new MainApp();
        Method m = MainApp.class.getDeclaredMethod("ensureDefaultAdminUser", UserRepository.class);
        m.setAccessible(true);
        assertThatCode(() -> m.invoke(app, repo)).doesNotThrowAnyException();
    }

    /**
     * {@code ensureDefaultAdminUser}: BCrypt-looking hash, wrong password, {@code force=false}, {@code nonBcrypt=false}
     * → else branch (warn only, no save).
     */
    @Test
    void ensureDefaultAdminUser_bcryptMismatchWithoutForce_logsWarnBranch_noSave() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        when(repo.findByEmail("admin@admin.com"))
                .thenReturn(Optional.of(new Administrator("a-warn", "A", "admin@admin.com", "$2a$10$abcdefghijklmnopqrstuv")));
        MainApp app = new MainApp();
        Method m = MainApp.class.getDeclaredMethod("ensureDefaultAdminUser", UserRepository.class);
        m.setAccessible(true);
        try (var ph = mockStatic(PasswordHasher.class); var cfg = mockStatic(AppConfig.class)) {
            ph.when(() -> PasswordHasher.verify(anyString(), anyString())).thenReturn(false);
            cfg.when(AppConfig::isForceDefaultAdminPasswordOnStartup).thenReturn(false);
            m.invoke(app, repo);
            verify(repo, never()).save(any(User.class));
        }
    }

    @Test
    void start_uncaughtExceptionHandler_nullMessage_usesThrowableClassName() throws Exception {
        // javac/ecj name the handler {@code lambda$start$0} or {@code lambda$0} etc.; match by signature only.
        List<Method> handlers = new ArrayList<>();
        for (Method m : MainApp.class.getDeclaredMethods()) {
            if (!m.getName().startsWith("lambda$")) continue;
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!Arrays.equals(m.getParameterTypes(), new Class<?>[]{Thread.class, Throwable.class})) continue;
            m.setAccessible(true);
            handlers.add(m);
        }
        assertThat(handlers).as("static synthetic (Thread, Throwable) lambdas on MainApp").isNotEmpty();
        Method h = handlers.stream()
                .filter(m -> m.getName().contains("start"))
                .findFirst()
                .orElse(handlers.get(0));

        Stage stage = runOnFx(Stage::new);
        runOnFxVoid(() -> stage.setScene(new Scene(new VBox(), 200, 120)));
        Field f = MainApp.class.getDeclaredField("primaryStage");
        f.setAccessible(true);
        Object prev = f.get(null);
        try {
            f.set(null, stage);
            class MsgNull extends RuntimeException {
                @Override
                public String getMessage() {
                    return null;
                }
            }
            Thread dummy = new Thread("coverage-thread");
            Throwable ex = new MsgNull();
            assertThatCode(() -> h.invoke(null, dummy, ex)).doesNotThrowAnyException();
            CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(done::countDown);
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            f.set(null, prev);
        }
    }

    private static List<Method> mainAppLambdasWithParams(Class<?>... parameterTypes) {
        List<Method> matches = new ArrayList<>();
        for (Method m : MainApp.class.getDeclaredMethods()) {
            if (!m.getName().startsWith("lambda$")) {
                continue;
            }
            if (Arrays.equals(m.getParameterTypes(), parameterTypes)) {
                m.setAccessible(true);
                matches.add(m);
            }
        }
        matches.sort(Comparator.comparing(Method::getName));
        return matches;
    }

    private static Object receiverFor(Method m) {
        return Modifier.isStatic(m.getModifiers()) ? null : new MainApp();
    }

    private static <T> T runOnFx(java.util.concurrent.Callable<T> task) {
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
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("FX task timed out");
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
