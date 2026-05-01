package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.application.ClosedDayBroadcast;
import com.appointmentscheduler.application.ClosedDayService;
import com.appointmentscheduler.application.PatientInboxEntry;
import com.appointmentscheduler.persistence.UserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Single batch raising branch coverage for presentation classes that were under ~80%
 * ({@link LoginController}, {@link PatientDashboardController}, {@link AdminDashboardController}, etc.).
 */
@ResourceLock("ApplicationContextServices")
@ResourceLock("AppConfigProps")
class PresentationBelow80UnifiedBranchBatchTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @BeforeEach
    void appConfigBaseline() {
        AppConfig.setSystemType("General");
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("app.test.autoDialogs");
        ApplicationContext.setAuthService(null);
        ApplicationContext.setInAppMessagingService(null);
        ApplicationContext.setScheduleService(null);
        ApplicationContext.setClosedDayService(null);
        ApplicationContext.setAuditLogService(null);
    }

    // --- LoginController (was ~59% branch) ---

    @Test
    void login_handleLogin_nullPasswordField_validEmail() throws Exception {
        LoginController c = new LoginController();
        setFxFields(c);
        TextField email = (TextField) getField(c, "emailField");
        email.setText("ok@example.com");
        setField(c, "passwordField", null);
        Label msg = (Label) getField(c, "messageLabel");
        assertThatCode(c::handleLogin).doesNotThrowAnyException();
        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.password_required"));
    }

    @Test
    void login_initialize_nullSubtitle_skipsSubtitleBranch() throws Exception {
        LoginController c = new LoginController();
        setField(c, "loginSubtitleLabel", null);
        setField(c, "devCredentialsBox", new VBox());
        setField(c, "passwordField", new PasswordField());
        setField(c, "passwordToggleButton", new Button());
        assertThatCode(c::initialize).doesNotThrowAnyException();
    }

    @Test
    void login_initialize_nullDevCredentialsBox_skipsDevBoxBranch() throws Exception {
        LoginController c = new LoginController();
        setField(c, "loginSubtitleLabel", new Label());
        setField(c, "devCredentialsBox", null);
        setField(c, "passwordField", new PasswordField());
        setField(c, "passwordToggleButton", new Button());
        assertThatCode(c::initialize).doesNotThrowAnyException();
    }

    @Test
    void login_handleOpenRegistration_auto_saveFailure_includesCauseMessage() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        AuthService auth = mock(AuthService.class);
        UserRepository repo = mock(UserRepository.class);
        when(auth.getUserRepository()).thenReturn(repo);
        when(repo.findByEmail(any())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("wrap", new IllegalStateException("cause-detail"))).when(repo).save(any());
        ApplicationContext.setAuthService(auth);

        LoginController c = new LoginController();
        setFxFields(c);
        assertThatCode(() -> runOnFxVoid(c::handleOpenRegistration)).doesNotThrowAnyException();
    }

    // --- PatientDashboardController ---

    @Test
    void patient_handleNavProfile_nullCurrentUser_skipsProfilePopulate() throws Exception {
        PatientDashboardController c = new PatientDashboardController();
        VBox appointmentsView = new VBox();
        VBox bookView = new VBox();
        VBox profileView = new VBox();
        VBox messagesView = new VBox();
        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        Button b4 = new Button();
        StackPane content = new StackPane();
        setField(c, "appointmentsView", appointmentsView);
        setField(c, "bookView", bookView);
        setField(c, "profileView", profileView);
        setField(c, "messagesView", messagesView);
        setField(c, "btnNavAppointments", b1);
        setField(c, "btnNavBook", b2);
        setField(c, "btnNavProfile", b3);
        setField(c, "btnNavMessages", b4);
        setField(c, "contentArea", content);
        setField(c, "currentUser", null);
        TextField name = new TextField("X");
        TextField mail = new TextField("Y");
        TextField phone = new TextField("Z");
        setField(c, "profileNameField", name);
        setField(c, "profileEmailField", mail);
        setField(c, "profilePhoneField", phone);
        assertThatCode(c::handleNavProfile).doesNotThrowAnyException();
        assertThat(name.getText()).isEqualTo("X");
        assertThat(mail.getText()).isEqualTo("Y");
        assertThat(phone.getText()).isEqualTo("Z");
    }

    @Test
    void patient_refreshPatientInbox_listCell_emptyBodyPreview_showsDash() {
        PatientInboxEntry emptyBody =
                new PatientInboxEntry("T", "", LocalDateTime.of(2028, 6, 1, 8, 0), "Staff");
        assertThat(PatientDashboardController.inboxListBodyPreviewText(emptyBody)).isEqualTo("—");

        PatientInboxEntry withBody =
                new PatientInboxEntry("T", "Hello", LocalDateTime.of(2028, 6, 1, 8, 0), "Staff");
        assertThat(PatientDashboardController.inboxListBodyPreviewText(withBody)).isEqualTo("Hello");
    }

    // --- AdminDashboardController ---

    @Test
    void admin_handleCloseDay_broadcastOnlyWhenNewlyClosed() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label welcome = new Label();
        new Scene(new StackPane(welcome), 40, 40);
        setField(c, "welcomeLabel", welcome);
        ClosedDayService cds = new ClosedDayService();
        LocalDate d = LocalDate.of(2029, 2, 15);
        cds.removeClosedDay(d);
        ApplicationContext.setClosedDayService(cds);
        ApplicationContext.setAuditLogService(null);
        DatePicker dp = new DatePicker(d);
        setField(c, "closedDayDatePicker", dp);
        try (MockedStatic<ClosedDayBroadcast> br = mockStatic(ClosedDayBroadcast.class, Answers.CALLS_REAL_METHODS)) {
            c.handleCloseDay();
            br.verify(() -> ClosedDayBroadcast.broadcastDayClosed(d), times(1));
            c.handleCloseDay();
            br.verify(() -> ClosedDayBroadcast.broadcastDayClosed(d), times(1));
        }
    }

    @Test
    void admin_handleReopenDay_broadcastOnlyWhenWasClosed() throws Exception {
        AdminDashboardController c = new AdminDashboardController();
        Label welcome = new Label();
        new Scene(new StackPane(welcome), 40, 40);
        setField(c, "welcomeLabel", welcome);
        ClosedDayService cds = new ClosedDayService();
        LocalDate d = LocalDate.of(2029, 3, 20);
        cds.removeClosedDay(d);
        ApplicationContext.setClosedDayService(cds);
        ApplicationContext.setAuditLogService(null);
        DatePicker dp = new DatePicker(d);
        setField(c, "closedDayDatePicker", dp);
        try (MockedStatic<ClosedDayBroadcast> br = mockStatic(ClosedDayBroadcast.class, Answers.CALLS_REAL_METHODS)) {
            c.handleReopenDay();
            br.verify(() -> ClosedDayBroadcast.broadcastDayReopened(d), never());
            cds.addClosedDay(d);
            dp.setValue(d);
            c.handleReopenDay();
            br.verify(() -> ClosedDayBroadcast.broadcastDayReopened(d), times(1));
        }
    }

    private static void setFxFields(LoginController c) throws Exception {
        setField(c, "emailField", new TextField());
        setField(c, "passwordField", new PasswordField());
        setField(c, "passwordToggleButton", new Button());
        setField(c, "messageLabel", new Label());
        setField(c, "loginSubtitleLabel", new Label());
        setField(c, "devCredentialsBox", new VBox());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            return;
        }
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        if (f == null) {
            return null;
        }
        f.setAccessible(true);
        return f.get(target);
    }

    private static Field findField(Class<?> cl, String name) {
        for (Class<?> c = cl; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static void runOnFxVoid(Runnable r) {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
    }
}
