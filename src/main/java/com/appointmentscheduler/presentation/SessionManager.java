package com.appointmentscheduler.presentation;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.SessionTimeoutPolicy;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class SessionManager {
    private static long getTimeoutMinutes() { return AppConfig.getSessionTimeoutMinutes(); }
    private static long getWarningMinutes() { return AppConfig.getSessionWarningMinutes(); }

    private LocalDateTime lastActivity;
    private Timer timer;
    private boolean warningShown;
    private final SessionTimeoutPolicy timeoutPolicy = new SessionTimeoutPolicy();
    
    // Singleton
    private static SessionManager instance;

    private SessionManager() {
        startTracking();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void startTracking() {
        lastActivity = LocalDateTime.now();
        warningShown = false;
        
        if (timer != null) timer.cancel();

        // In automated UI coverage mode we drive session flows explicitly in tests.
        // Avoid background timer noise that can race with test setup/teardown.
        if (DialogHelper.isAutoDialogs()) {
            timer = null;
            return;
        }
        
        timer = new Timer(true); // daemon
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkTimeout();
            }
        }, 1000L * 60, 1000L * 30); // Check every 30 seconds after 1 min initial delay
    }

    public void registerScene(Scene scene) {
        if (scene == null) return;
        
        // Track obvious user movement
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::updateActivity);
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, this::updateActivity);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::updateActivity);
    }
    
    public void unregister() {
        if (timer != null) timer.cancel();
        timer = null;
    }

    private void updateActivity(Event e) {
        lastActivity = LocalDateTime.now();

        // If they became active after warning but before timeout, reset it
        if (warningShown) {
            warningShown = false;
        }
    }

    private void checkTimeout() {
        var auth = ApplicationContext.getAuthService();
        SessionTimeoutPolicy.Action action = timeoutPolicy.evaluate(
                lastActivity,
                LocalDateTime.now(),
                warningShown,
                getTimeoutMinutes(),
                getWarningMinutes(),
                auth != null && auth.getCurrentUser() != null
        );

        if (action == SessionTimeoutPolicy.Action.LOGOUT) {
            Platform.runLater(() -> {
                var authSvc = ApplicationContext.getAuthService();
                var user = authSvc != null ? authSvc.getCurrentUser() : null;

                if (user != null && ApplicationContext.getAuditLogService() != null) {
                    ApplicationContext.getAuditLogService().log(user, "LOGOUT", "Session expired (inactivity)");
                }

                unregister();

                if (authSvc != null) authSvc.logout();

                DialogHelper.showError(I18n.get("session.expired"), I18n.get("session.expired.message"));
                MainApp.loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin());
            });
        } else if (action == SessionTimeoutPolicy.Action.WARN) {
            warningShown = true;
            long minutesLeft = Math.max(0, getTimeoutMinutes() - getWarningMinutes());
            Platform.runLater(() -> showSessionWarningDialog(minutesLeft));
        }
    }

    /**
     * Call when user explicitly extends session (e.g. "Stay logged in" in dialog).
     */
    public void extendSession() {
        lastActivity = LocalDateTime.now();
        warningShown = false;
    }

    private void showSessionWarningDialog(long minutesLeft) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(AppConfig.getAppName());
        alert.setHeaderText(I18n.get("session.warning"));
        alert.setContentText(I18n.get("session.warning.message", minutesLeft));

        javafx.scene.control.ButtonType stay = new javafx.scene.control.ButtonType(
                I18n.get("session.stay"),
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE
        );
        javafx.scene.control.ButtonType logout = new javafx.scene.control.ButtonType(
                I18n.get("session.logout"),
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
        );

        alert.getButtonTypes().setAll(stay, logout);

        java.util.Optional<javafx.scene.control.ButtonType> result =
                DialogHelper.isAutoDialogs() ? java.util.Optional.of(stay) : alert.showAndWait();

        boolean choseStay = result.isPresent()
                && result.get().getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE;

        if (choseStay) {
            extendSession();
        } else {
            var authSvc = ApplicationContext.getAuthService();
            var user = authSvc != null ? authSvc.getCurrentUser() : null;

            if (user != null && ApplicationContext.getAuditLogService() != null) {
                ApplicationContext.getAuditLogService().log(user, "LOGOUT", "User chose to log out from session warning");
            }

            unregister();

            if (authSvc != null) authSvc.logout();

            MainApp.loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin());
        }
    }
}