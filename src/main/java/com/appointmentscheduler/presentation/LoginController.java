package com.appointmentscheduler.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.PasswordHasher;
import com.appointmentscheduler.domain.User;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for the login screen.
 * It validates credentials, routes users to the appropriate dashboard, and exposes a compact
 * self-registration dialog for new client accounts.
 */
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button passwordToggleButton;
    @FXML private Label messageLabel;
    @FXML private Label loginSubtitleLabel;
    @FXML private VBox devCredentialsBox;

    /**
     * Initializes static labels, optional developer hints, and password-visibility helpers after FXML injection.
     */
    @FXML
    public void initialize() {
        try {
            if (loginSubtitleLabel != null) {
                loginSubtitleLabel.setText(AppConfig.getBrandName() + " · " + AppConfig.getSystemType());
            }
            if (devCredentialsBox != null) {
                boolean show = AppConfig.isShowDevCredentials();
                devCredentialsBox.setVisible(show);
                devCredentialsBox.setManaged(show);
            }
            setupLoginPasswordToggle();
        } catch (Throwable t) {
            log.warn("initialize: could not set dev credentials visibility", t);
        }
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN =
        java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Validates the entered credentials and attempts authentication through {@link ApplicationContext}.
     * Successful authentication routes administrators and patients to different dashboards.
     */
    @FXML
    public void handleLogin() {
        String email = emailField != null ? emailField.getText() : null;
        String password = passwordField != null ? passwordField.getText() : null;
        if (email != null) email = email.trim();

        clearFieldErrors();
        if (email == null || email.isBlank()) {
            messageLabel.setText(I18n.get("login.error.email_required"));
            messageLabel.getStyleClass().setAll("error-label");
            if (emailField != null) emailField.getStyleClass().add("error-input");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            messageLabel.setText(I18n.get("login.error.email_invalid"));
            messageLabel.getStyleClass().setAll("error-label");
            if (emailField != null) emailField.getStyleClass().add("error-input");
            return;
        }
        if (password == null || password.isEmpty()) {
            messageLabel.setText(I18n.get("login.error.password_required"));
            messageLabel.getStyleClass().setAll("error-label");
            if (passwordField != null) passwordField.getStyleClass().add("error-input");
            return;
        }

        if (ApplicationContext.getAuthService().isAccountLocked(email)) {
            int mins = ApplicationContext.getAuthService().getRemainingLockMinutes(email);
            messageLabel.setText(I18n.get("login.error.locked", mins));
            messageLabel.getStyleClass().setAll("error-label");
            return;
        }

        boolean success = ApplicationContext.getAuthService().login(email.trim(), password);

        if (success) {
            clearFieldErrors();
            messageLabel.setText("");
            log.info("Login successful for user: {}", email);
            if (ApplicationContext.getAuthService().isCurrentUserAdmin()) {
                MainApp.loadScreen(ScreenConstants.FXML_ADMIN_DASHBOARD, ScreenConstants.titleAdminDashboard());
            } else {
                MainApp.loadScreen(ScreenConstants.FXML_PATIENT_DASHBOARD, ScreenConstants.titlePatientDashboard());
            }
        } else {
            if (ApplicationContext.getAuthService().isAccountLocked(email)) {
                messageLabel.setText(I18n.get("login.error.locked", ApplicationContext.getAuthService().getRemainingLockMinutes(email)));
            } else {
                messageLabel.setText(I18n.get("login.error.invalid"));
            }
            messageLabel.getStyleClass().setAll("error-label");
        }
    }

    /**
     * Opens a small dialog that lets the user create a new client account.
     */
    @FXML
    public void handleOpenRegistration() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(I18n.get("register.dialog.title"));
        dialog.setHeaderText(I18n.get("register.dialog.header"));

        DialogPane pane = dialog.getDialogPane();
        pane.setPrefWidth(420);
        pane.setPrefHeight(280);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("John Doe");

        TextField emailFieldLocal = new TextField();
        emailFieldLocal.setPromptText("you@company.com");

        PasswordField passwordFieldLocal = new PasswordField();
        passwordFieldLocal.setPromptText("********");

        // Visible text field used when the user chooses to show the password
        TextField passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("********");
        passwordVisibleField.textProperty().bindBidirectional(passwordFieldLocal.textProperty());

        // Eye icon button for toggling visibility
        Button toggleVisibility = new Button();
        toggleVisibility.getStyleClass().add("password-toggle-button");
        javafx.scene.shape.SVGPath eyeIcon = new javafx.scene.shape.SVGPath();
        eyeIcon.setContent("M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12zm11 3a3 3 0 100-6 3 3 0 000 6z");
        eyeIcon.setScaleX(0.7);
        eyeIcon.setScaleY(0.7);
        toggleVisibility.setGraphic(eyeIcon);

        // Row containing password field and eye icon
        HBox passwordRow = new HBox(8);
        HBox.setHgrow(passwordFieldLocal, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(passwordVisibleField, javafx.scene.layout.Priority.ALWAYS);
        passwordRow.getChildren().addAll(passwordFieldLocal, toggleVisibility);

        // Toggle logic: swap between PasswordField and TextField while keeping layout tidy
        toggleVisibility.setOnAction(e -> {
            int index = passwordRow.getChildren().indexOf(passwordFieldLocal);
            if (index >= 0) {
                passwordRow.getChildren().set(index, passwordVisibleField);
            } else {
                index = passwordRow.getChildren().indexOf(passwordVisibleField);
                if (index >= 0) {
                    passwordRow.getChildren().set(index, passwordFieldLocal);
                }
            }
        });

        VBox content = new VBox(10,
            new Label(I18n.get("register.name")), nameField,
            new Label(I18n.get("login.email")), emailFieldLocal,
            new Label(I18n.get("login.password")), passwordRow
        );
        content.setPadding(new Insets(10, 0, 0, 0));

        pane.setContent(content);

        // Apply the same minimal dialog styling used elsewhere
        try {
            pane.getStyleClass().add("dialog-pane");
            java.net.URL minimal = DialogHelper.class.getResource("/com/appointmentscheduler/presentation/application-minimal.css");
            if (minimal != null && !pane.getStylesheets().contains(minimal.toExternalForm())) {
                pane.getStylesheets().add(minimal.toExternalForm());
            }
        } catch (Exception ex) {
            // Styling is optional for behavior; keep registration flow usable.
            log.debug("Could not apply registration dialog stylesheet", ex);
        }

        // Localize button texts to match the rest of the app
        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText(I18n.get("dialog.confirm"));
        }
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setText(I18n.get("cancel"));
        }

        if (DialogHelper.isAutoDialogs()) {
            // Auto-submit registration for test coverage.
            if (nameField.getText() == null || nameField.getText().isBlank()) nameField.setText("Auto User");
            if (emailFieldLocal.getText() == null || emailFieldLocal.getText().isBlank()) {
                emailFieldLocal.setText("auto." + System.nanoTime() + "@test.com");
            }
            if (passwordFieldLocal.getText() == null || passwordFieldLocal.getText().isBlank()) {
                passwordFieldLocal.setText("AutoPass123!");
            }

            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            String email = emailFieldLocal.getText() != null ? emailFieldLocal.getText().trim() : "";
            String password = passwordFieldLocal.getText() != null ? passwordFieldLocal.getText() : "";

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                DialogHelper.showError(I18n.get("error.title"), I18n.get("register.error.invalid"));
                return;
            }

            if (ApplicationContext.getAuthService().getUserRepository().findByEmail(email).isPresent()) {
                DialogHelper.showError(I18n.get("error.title"), I18n.get("register.error.email_exists"));
                return;
            }

            String id = java.util.UUID.randomUUID().toString();
            String hashed = PasswordHasher.hash(password);
            User user = new User(id, name, email.toLowerCase(), hashed);
            try {
                ApplicationContext.getAuthService().getUserRepository().save(user);
                if (messageLabel != null) {
                    messageLabel.setText(I18n.get("register.success"));
                    messageLabel.getStyleClass().setAll("success-label");
                }
            } catch (Exception e) {
                log.error("Registration save failed for email {}", email, e);
                String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                DialogHelper.showError(I18n.get("error.title"),
                        I18n.get("register.error.save_failed") + (msg != null ? " " + msg : ""));
            }
            return;
        }

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String name = nameField.getText() != null ? nameField.getText().trim() : "";
                String email = emailFieldLocal.getText() != null ? emailFieldLocal.getText().trim() : "";
                String password = passwordFieldLocal.getText() != null ? passwordFieldLocal.getText() : "";

                if (name.isEmpty() || email.isEmpty() || password.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                    DialogHelper.showError(I18n.get("error.title"), I18n.get("register.error.invalid"));
                    return;
                }

                if (ApplicationContext.getAuthService().getUserRepository().findByEmail(email).isPresent()) {
                    DialogHelper.showError(I18n.get("error.title"), I18n.get("register.error.email_exists"));
                    return;
                }

                String id = java.util.UUID.randomUUID().toString();
                String hashed = PasswordHasher.hash(password);
                User user = new User(id, name, email.toLowerCase(), hashed);
                try {
                    ApplicationContext.getAuthService().getUserRepository().save(user);
                    messageLabel.setText(I18n.get("register.success"));
                    messageLabel.getStyleClass().setAll("success-label");
                } catch (Exception e) {
                    log.error("Registration save failed for email {}", email, e);
                    String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    DialogHelper.showError(I18n.get("error.title"),
                            I18n.get("register.error.save_failed") + (msg != null ? " " + msg : ""));
                }
            }
        });
    }

    private void clearFieldErrors() {
        if (emailField != null) emailField.getStyleClass().remove("error-input");
        if (passwordField != null) passwordField.getStyleClass().remove("error-input");
    }

    /**
     * Simple password strength evaluation used by registration dialog.
     */
    private void updatePasswordStrength(String password, ProgressBar bar, Label label) {
        if (bar == null || label == null) return;
        if (password == null || password.isEmpty()) {
            bar.setProgress(0);
            label.setText("");
            return;
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialCharacter = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isUpperCase(ch)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(ch)) {
                hasLowercase = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else {
                hasSpecialCharacter = true;
            }
        }
        if (hasUppercase && hasLowercase) score++;
        if (hasDigit) score++;
        if (hasSpecialCharacter) score++;

        double normalized = Math.min(1.0, score / 4.0);
        bar.setProgress(normalized);

        if (score <= 2) {
            bar.setStyle("-fx-accent: #ef4444;");
            label.setText(I18n.get("password.strength.weak") + " — " + I18n.get("password.requirements"));
        } else if (score == 3) {
            bar.setStyle("-fx-accent: #f97316;");
            label.setText(I18n.get("password.strength.medium") + " — " + I18n.get("password.requirements"));
        } else {
            bar.setStyle("-fx-accent: #16a34a;");
            label.setText(I18n.get("password.strength.strong"));
        }
    }

    // -------- Password utilities --------

    private void setupLoginPasswordToggle() {
        if (passwordField == null || passwordToggleButton == null) return;

        TextField visibleField = new TextField();
        visibleField.getStyleClass().add("password-field");
        visibleField.setPromptText(passwordField.getPromptText());
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleField.setVisible(false);

        if (passwordField.getParent() instanceof javafx.scene.layout.StackPane parent) {
            parent.getChildren().add(0, visibleField);

            passwordToggleButton.setOnAction(e -> {
                boolean show = !visibleField.isVisible();
                visibleField.setVisible(show);
                passwordField.setVisible(!show);
            });
        }
    }
}
