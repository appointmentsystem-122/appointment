package com.appointmentscheduler.presentation;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Utility class for displaying styled JavaFX Alerts.
 */
public class DialogHelper {
    private static final Logger log = LoggerFactory.getLogger(DialogHelper.class);

    static boolean isAutoDialogs() {
        return Boolean.getBoolean("app.test.autoDialogs");
    }

    /** Logout confirmation: professional bilingual dialog with Confirm/Cancel. */
    public static boolean showLogoutConfirmation(String appTitle) {
        String header = I18n.get("logout.dialog.header");
        String content = I18n.get("logout.confirm.bilingual");
        ButtonType confirmBtn = new ButtonType(I18n.get("dialog.confirm"), ButtonType.OK.getButtonData());
        ButtonType cancelBtn = new ButtonType(I18n.get("cancel"), ButtonType.CANCEL.getButtonData());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", confirmBtn, cancelBtn);
        alert.setTitle(appTitle);
        alert.setHeaderText(header);
        alert.setContentText(content);

        applyDialogStyles(alert);

        if (isAutoDialogs()) {
            // Still consider the dialog "confirmed" for automated tests.
            return true;
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmBtn;
    }

    /**
     * Shows a confirmation dialog and returns true if the user clicks OK/Yes.
     */
    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        applyDialogStyles(alert);

        if (isAutoDialogs()) {
            return true;
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Shows a simple information dialog.
     */
    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        applyDialogStyles(alert);
        if (!isAutoDialogs()) {
            alert.showAndWait();
        }
    }

    /**
     * Shows an error dialog.
     */
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        applyDialogStyles(alert);
        if (!isAutoDialogs()) {
            alert.showAndWait();
        }
    }

    /** Shows a professional keyboard shortcuts reference dialog (client portal). */
    public static void showKeyboardShortcutsClient(Window owner) {
        String content = "F5  —  " + I18n.get("shortcuts.refresh") + "\n"
                + "Ctrl+Enter  —  " + I18n.get("shortcuts.book_confirm") + "\n"
                + "Ctrl+Q  —  " + I18n.get("shortcuts.logout");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("shortcuts.title"));
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.OK);
        if (owner != null) alert.initOwner(owner);
        applyDialogStyles(alert);
        if (!isAutoDialogs()) {
            alert.showAndWait();
        }
    }

    /** Shows keyboard shortcuts reference for admin portal. */
    public static void showKeyboardShortcutsAdmin(Window owner) {
        String content = "F5  —  " + I18n.get("shortcuts.refresh") + "\n"
                + "Ctrl+F  —  " + I18n.get("shortcuts.search") + "\n"
                + "Ctrl+Q  —  " + I18n.get("shortcuts.logout");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("shortcuts.title"));
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.OK);
        if (owner != null) alert.initOwner(owner);
        applyDialogStyles(alert);
        if (!isAutoDialogs()) {
            alert.showAndWait();
        }
    }

    private static void applyDialogStyles(Alert alert) {
        try {
            DialogPane pane = alert.getDialogPane();
            pane.getStyleClass().add("dialog-pane");
            java.net.URL minimal = DialogHelper.class.getResource("/com/appointmentscheduler/presentation/application-minimal.css");
            if (minimal != null) {
                pane.getStylesheets().add(minimal.toExternalForm());
            }
        } catch (Exception e) {
            log.debug("Could not apply dialog stylesheet", e);
        }
    }
}
