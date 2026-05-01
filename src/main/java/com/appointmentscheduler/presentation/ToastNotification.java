package com.appointmentscheduler.presentation;

import com.appointmentscheduler.presentation.notification.NotificationType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Utility for displaying animated Toast notifications with type-based styling.
 */
public class ToastNotification {

    public static void show(String message, Window ownerWindow) {
        show(message, ownerWindow, false);
    }

    public static void show(String message, Window ownerWindow, boolean isError) {
        show(ownerWindow, isError ? NotificationType.ERROR : NotificationType.INFO, null, message);
    }

    /**
     * Show a toast with a specific notification type (INFO, SUCCESS, WARNING, ERROR).
     */
    public static void show(Window ownerWindow, NotificationType type, String title, String message) {
        if (ownerWindow == null) return;
        NotificationType t = type != null ? type : NotificationType.INFO;
        String text = (title != null && !title.isEmpty()) ? title + ": " + message : message;
        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Label label = new Label(text);
            label.getStyleClass().add("toast-text");

            VBox box = new VBox(label);
            box.getStyleClass().add("toast");
            switch (t) {
                case SUCCESS -> box.getStyleClass().add("toast-success");
                case WARNING -> box.getStyleClass().add("toast-warning");
                case ERROR -> box.getStyleClass().add("toast-error");
                default -> box.getStyleClass().add("toast-info");
            }

            popup.getContent().add(box);

            popup.show(ownerWindow);
            popup.setX(ownerWindow.getX() + ownerWindow.getWidth() / 2 - 150);
            popup.setY(ownerWindow.getY() + ownerWindow.getHeight() - 100);

            box.setTranslateY(20);
            box.setOpacity(0.0);
            Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.millis(300),
                    new KeyValue(box.translateYProperty(), 0),
                    new KeyValue(box.opacityProperty(), 1.0)
                )
            );
            Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.millis(300),
                    new KeyValue(box.opacityProperty(), 0.0)
                )
            );
            fadeOut.setDelay(Duration.seconds(3));
            fadeOut.setOnFinished(e -> popup.hide());
            fadeIn.setOnFinished(e -> fadeOut.play());
            fadeIn.play();
        });
    }
}
