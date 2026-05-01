package com.appointmentscheduler.presentation;

import javafx.scene.control.Alert;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized error handling for user-facing messages and logging.
 * Enterprise standard: log server-side, show safe message to user.
 */
public final class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() { }

    /**
     * Logs the throwable and shows an error dialog with a user-friendly message.
     */
    public static void handle(Window owner, String userMessage, Throwable throwable) {
        if (throwable != null) {
            log.error(userMessage, throwable);
        } else {
            log.error(userMessage);
        }
        showError(owner, userMessage);
    }

    /**
     * Shows an error dialog with the given message.
     */
    public static void showError(Window owner, String message) {
        javafx.application.Platform.runLater(() -> showErrorOnFxThread(owner, message));
    }

    /**
     * Must run on the JavaFX application thread. Package-private so tests can open
     * {@code Mockito.mockConstruction(Alert.class,...)} on that thread and then invoke this method.
     */
    static void showErrorOnFxThread(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18n.get("error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : I18n.get("error.generic"));
        if (owner != null) alert.initOwner(owner);
        if (!DialogHelper.isAutoDialogs()) {
            alert.showAndWait();
        }
    }

    /**
     * Logs a warning without showing a dialog.
     */
    public static void logWarning(String message, Throwable throwable) {
        if (throwable != null) {
            log.warn(message, throwable);
        } else {
            log.warn(message);
        }
    }
}
