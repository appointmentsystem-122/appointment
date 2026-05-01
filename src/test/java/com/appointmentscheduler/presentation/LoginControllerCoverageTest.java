package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.AuthService;
import com.appointmentscheduler.persistence.UserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.*;

class LoginControllerCoverageTest {

    private AuthService authService;

    @BeforeEach
    void setUpFx() {
        JavaFxTestSupport.initPlatform();
        // Ensure deterministic static config usage for this controller.
        AppConfig.setSystemType("General");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("app.test.autoDialogs");
        ApplicationContext.setAuthService(null);
    }

    @Test
    void initialize_passwordToggleWithStackPaneParent_swapsPasswordAndVisibleField() throws Exception {
        LoginController controller = new LoginController();
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("pwd");
        StackPane parent = new StackPane();
        parent.getChildren().add(passwordField);
        Button toggleBtn = new Button();

        setField(controller, "loginSubtitleLabel", new Label());
        setField(controller, "devCredentialsBox", new VBox());
        setField(controller, "messageLabel", new Label());
        setField(controller, "passwordField", passwordField);
        setField(controller, "passwordToggleButton", toggleBtn);

        controller.initialize();

        assertThat(parent.getChildren().size()).isGreaterThanOrEqualTo(2);
        assertThat(passwordField.isVisible()).isTrue();

        toggleBtn.fire();
        assertThat(passwordField.isVisible()).isFalse();

        toggleBtn.fire();
        assertThat(passwordField.isVisible()).isTrue();
    }

    @Test
    void initialize_setsBrandAndDevCredentialsVisibility_andSetsPasswordToggleHandler() throws Exception {
        LoginController controller = new LoginController();

        Label subtitle = new Label();
        VBox devBox = new VBox();
        PasswordField passwordField = new PasswordField();
        Button toggleBtn = new Button();
        StackPane parent = new StackPane(passwordField);

        setField(controller, "loginSubtitleLabel", subtitle);
        setField(controller, "devCredentialsBox", devBox);
        setField(controller, "passwordField", passwordField);
        setField(controller, "passwordToggleButton", toggleBtn);

        controller.initialize();

        assertThat(subtitle.getText()).isEqualTo(AppConfig.getBrandName() + " · " + AppConfig.getSystemType());
        assertThat(devBox.isVisible()).isEqualTo(AppConfig.isShowDevCredentials());
        assertThat(devBox.isManaged()).isEqualTo(AppConfig.isShowDevCredentials());

        // If handler is installed, pressing it should swap visibility states without throwing.
        // (We don't assert exact node order, only that handler runs.)
        assertThat(parent.getChildren()).isNotEmpty();
        toggleBtn.fire();
    }

    @Test
    void handleLogin_blankEmail_showsEmailRequired_andStyles() throws Exception {
        LoginController controller = new LoginController();
        setCommonFields(controller);

        TextField emailField = (TextField) getField(controller, "emailField");
        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");

        emailField.setText("   ");
        passwordField.setText("secret");

        // seed "error-input" to verify clearFieldErrors() removes it
        emailField.getStyleClass().add("error-input");
        passwordField.getStyleClass().add("error-input");

        controller.handleLogin();

        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.email_required"));
        assertThat(msg.getStyleClass()).contains("error-label");
        assertThat(emailField.getStyleClass()).contains("error-input");
        assertThat(passwordField.getStyleClass()).doesNotContain("error-input");
    }

    @Test
    void handleLogin_invalidEmail_showsEmailInvalid_andStyles() throws Exception {
        LoginController controller = new LoginController();
        setCommonFields(controller);

        TextField emailField = (TextField) getField(controller, "emailField");
        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");

        emailField.setText("not-an-email");
        passwordField.setText("secret");

        emailField.getStyleClass().add("error-input");
        passwordField.getStyleClass().add("error-input");

        controller.handleLogin();

        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.email_invalid"));
        assertThat(msg.getStyleClass()).contains("error-label");
        assertThat(emailField.getStyleClass()).contains("error-input");
        assertThat(passwordField.getStyleClass()).doesNotContain("error-input");
    }

    @Test
    void handleLogin_emptyPassword_showsPasswordRequired_andStyles() throws Exception {
        LoginController controller = new LoginController();
        setCommonFields(controller);

        TextField emailField = (TextField) getField(controller, "emailField");
        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");

        emailField.setText("john@test.com");
        passwordField.setText("");

        emailField.getStyleClass().add("error-input");
        passwordField.getStyleClass().add("error-input");

        controller.handleLogin();

        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.password_required"));
        assertThat(msg.getStyleClass()).contains("error-label");
        assertThat(emailField.getStyleClass()).doesNotContain("error-input");
        assertThat(passwordField.getStyleClass()).contains("error-input");
    }

    @Test
    void handleLogin_lockedAccount_showsLockedMessage_andDoesNotLogin() throws Exception {
        String email = "john@test.com";
        int mins = 7;

        authService = mock(AuthService.class);
        when(authService.isAccountLocked(eq(email))).thenReturn(true);
        when(authService.getRemainingLockMinutes(eq(email))).thenReturn(mins);
        ApplicationContext.setAuthService(authService);

        LoginController controller = new LoginController();
        setCommonFields(controller);

        TextField emailField = (TextField) getField(controller, "emailField");
        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");

        emailField.setText(email);
        passwordField.setText("secret");

        controller.handleLogin();

        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.locked", mins));
        assertThat(msg.getStyleClass()).contains("error-label");
        verify(authService, never()).login(any(), any());

        assertThat(emailField.getStyleClass()).doesNotContain("error-input");
        assertThat(passwordField.getStyleClass()).doesNotContain("error-input");
    }

    @Test
    void handleLogin_invalidCredentials_showsInvalidMessage() throws Exception {
        String email = "john@test.com";

        authService = mock(AuthService.class);
        when(authService.isAccountLocked(eq(email))).thenReturn(false);
        when(authService.login(eq(email), eq("secret"))).thenReturn(false);
        ApplicationContext.setAuthService(authService);

        LoginController controller = new LoginController();
        setCommonFields(controller);

        TextField emailField = (TextField) getField(controller, "emailField");
        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");

        emailField.setText(email);
        passwordField.setText("secret");

        controller.handleLogin();

        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.invalid"));
        assertThat(msg.getStyleClass()).contains("error-label");
        verify(authService).login(eq(email), eq("secret"));
        verify(authService, times(2)).isAccountLocked(eq(email));
    }

    @Test
    void handleLogin_invalidCredentials_thenLocked_showsLockedMessage() throws Exception {
        String email = "lock-shift@test.com";
        authService = mock(AuthService.class);
        when(authService.isAccountLocked(eq(email))).thenReturn(false, true);
        when(authService.getRemainingLockMinutes(eq(email))).thenReturn(3);
        when(authService.login(eq(email), eq("secret"))).thenReturn(false);
        ApplicationContext.setAuthService(authService);

        LoginController controller = new LoginController();
        setCommonFields(controller);
        TextField emailField = (TextField) getField(controller, "emailField");
        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");

        emailField.setText(email);
        passwordField.setText("secret");
        controller.handleLogin();

        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.locked", 3));
    }

    @Test
    void handleLogin_success_admin_callsLoadScreen() throws Exception {
        String email = "admin-ok@test.com";
        authService = mock(AuthService.class);
        when(authService.isAccountLocked(eq(email))).thenReturn(false);
        when(authService.login(eq(email), eq("secret"))).thenReturn(true);
        when(authService.isCurrentUserAdmin()).thenReturn(true);
        ApplicationContext.setAuthService(authService);

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            LoginController controller = new LoginController();
            setCommonFields(controller);
            TextField emailField = (TextField) getField(controller, "emailField");
            PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
            emailField.setText(email);
            passwordField.setText("secret");

            controller.handleLogin();

            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_ADMIN_DASHBOARD), anyString()));
        }
    }

    @Test
    void handleLogin_success_patient_callsLoadScreen() throws Exception {
        String email = "patient-ok@test.com";
        authService = mock(AuthService.class);
        when(authService.isAccountLocked(eq(email))).thenReturn(false);
        when(authService.login(eq(email), eq("secret"))).thenReturn(true);
        when(authService.isCurrentUserAdmin()).thenReturn(false);
        ApplicationContext.setAuthService(authService);

        try (MockedStatic<MainApp> main = mockStatic(MainApp.class)) {
            LoginController controller = new LoginController();
            setCommonFields(controller);
            TextField emailField = (TextField) getField(controller, "emailField");
            PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
            emailField.setText(email);
            passwordField.setText("secret");

            controller.handleLogin();

            main.verify(() -> MainApp.loadScreen(eq(ScreenConstants.FXML_PATIENT_DASHBOARD), anyString()));
        }
    }

    @Test
    void handleOpenRegistration_autoMode_success_andDuplicate_andSaveFailure_paths() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        authService = mock(AuthService.class);
        UserRepository repo = mock(UserRepository.class);
        when(authService.getUserRepository()).thenReturn(repo);
        ApplicationContext.setAuthService(authService);

        LoginController controller = new LoginController();
        setCommonFields(controller);
        Label msg = (Label) getField(controller, "messageLabel");

        // success path
        when(repo.findByEmail(any())).thenReturn(java.util.Optional.empty());
        runOnFxVoid(controller::handleOpenRegistration);
        assertThat(msg.getText()).isEqualTo(I18n.get("register.success"));
        assertThat(msg.getStyleClass()).contains("success-label");

        // duplicate email path
        when(repo.findByEmail(any())).thenReturn(java.util.Optional.of(new com.appointmentscheduler.domain.User("id", "n", "e@x.com", "pw")));
        runOnFxVoid(controller::handleOpenRegistration);
        verify(repo, times(1)).save(any(com.appointmentscheduler.domain.User.class));

        // save failure path
        when(repo.findByEmail(any())).thenReturn(java.util.Optional.empty());
        doThrow(new RuntimeException("boom")).when(repo).save(any(com.appointmentscheduler.domain.User.class));
        runOnFxVoid(controller::handleOpenRegistration);
    }

    @Test
    void updatePasswordStrength_coversEmptyWeakMediumStrong() throws Exception {
        LoginController controller = new LoginController();
        ProgressBar bar = new ProgressBar(0);
        Label lbl = new Label();

        java.lang.reflect.Method m = LoginController.class.getDeclaredMethod(
                "updatePasswordStrength", String.class, ProgressBar.class, Label.class);
        m.setAccessible(true);

        m.invoke(controller, "", bar, lbl);
        assertThat(bar.getProgress()).isEqualTo(0.0);
        assertThat(lbl.getText()).isEmpty();

        m.invoke(controller, "abc", bar, lbl);
        assertThat(lbl.getText()).contains(I18n.get("password.strength.weak"));

        m.invoke(controller, "Abcdef12", bar, lbl);
        assertThat(lbl.getText()).contains(I18n.get("password.strength.medium"));

        m.invoke(controller, "Abcdef12!", bar, lbl);
        assertThat(lbl.getText()).contains(I18n.get("password.strength.strong"));
    }

    @Test
    void setupLoginPasswordToggle_branches_nonStackParent_and_nullGuards() throws Exception {
        LoginController controller = new LoginController();

        // Non-StackPane parent branch: handler should not be installed and no throw.
        PasswordField pf = new PasswordField();
        javafx.scene.layout.VBox parent = new javafx.scene.layout.VBox();
        parent.getChildren().add(pf);
        Button toggle = new Button();
        setField(controller, "passwordField", pf);
        setField(controller, "passwordToggleButton", toggle);
        assertThatCode(controller::initialize).doesNotThrowAnyException();

        // Null-guard branch in setupLoginPasswordToggle.
        LoginController c2 = new LoginController();
        setField(c2, "passwordField", null);
        setField(c2, "passwordToggleButton", new Button());
        assertThatCode(c2::initialize).doesNotThrowAnyException();
    }

    @Test
    void initialize_catchesThrowable_fromStaticConfig() throws Exception {
        LoginController controller = new LoginController();
        setField(controller, "loginSubtitleLabel", new Label());
        setField(controller, "devCredentialsBox", new VBox());
        setField(controller, "passwordField", new PasswordField());
        setField(controller, "passwordToggleButton", new Button());
        setField(controller, "messageLabel", new Label());

        try (MockedStatic<AppConfig> cfg = mockStatic(AppConfig.class, CALLS_REAL_METHODS)) {
            cfg.when(AppConfig::getBrandName).thenThrow(new RuntimeException("boom-config"));
            assertThatCode(controller::initialize).doesNotThrowAnyException();
        }
    }

    @Test
    void handleLogin_withNullEmailField_hitsNullGuardWithoutStylingCrash() throws Exception {
        LoginController controller = new LoginController();
        setField(controller, "emailField", null);
        setField(controller, "passwordField", new PasswordField());
        setField(controller, "passwordToggleButton", new Button());
        setField(controller, "messageLabel", new Label());
        setField(controller, "loginSubtitleLabel", new Label());
        setField(controller, "devCredentialsBox", new VBox());

        PasswordField passwordField = (PasswordField) getField(controller, "passwordField");
        Label msg = (Label) getField(controller, "messageLabel");
        passwordField.setText("secret");

        assertThatCode(controller::handleLogin).doesNotThrowAnyException();
        assertThat(msg.getText()).isEqualTo(I18n.get("login.error.email_required"));
    }

    @Test
    void updatePasswordStrength_nullBarOrLabel_returnsEarly() throws Exception {
        LoginController controller = new LoginController();
        java.lang.reflect.Method m = LoginController.class.getDeclaredMethod(
                "updatePasswordStrength", String.class, ProgressBar.class, Label.class);
        m.setAccessible(true);

        assertThatCode(() -> m.invoke(controller, "Abcdef12!", null, new Label())).doesNotThrowAnyException();
        assertThatCode(() -> m.invoke(controller, "Abcdef12!", new ProgressBar(), null)).doesNotThrowAnyException();
    }

    private void setCommonFields(LoginController controller) throws Exception {
        setField(controller, "emailField", new TextField());
        setField(controller, "passwordField", new PasswordField());
        setField(controller, "passwordToggleButton", new Button());
        setField(controller, "messageLabel", new Label());
        setField(controller, "loginSubtitleLabel", new Label());
        setField(controller, "devCredentialsBox", new VBox());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void runOnFxVoid(Runnable r) {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
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

