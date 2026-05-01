package com.appointmentscheduler.presentation.notification;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the notification bell + badge and popover list for the header.
 * Call install() from the dashboard controller with the placeholder HBox.
 */
public final class NotificationCenterView {

    private static final String BELL_PATH = "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z";
    private static final int POPOVER_MAX = 50;
    private static final double POPOVER_WIDTH = 380;
    private static final double POPOVER_MAX_HEIGHT = 400;

    /**
     * Adds the notification bell and badge to the given container and wires the popover.
     * The container should be in the header (e.g. an HBox before the logout button).
     */
    public static void install(NotificationCenter center, HBox container) {
        if (center == null || container == null) return;

        Button bell = new Button();
        bell.getStyleClass().addAll("notification-bell-btn", "header-icon-btn", "icon-button");
        SVGPath icon = new SVGPath();
        icon.setContent(BELL_PATH);
        icon.getStyleClass().add("icon");
        icon.setScaleX(1.1);
        icon.setScaleY(1.1);
        bell.setGraphic(icon);
        bell.setFocusTraversable(false);

        Label badge = new Label();
        badge.getStyleClass().add("notification-badge");
        badge.setVisible(false);
        badge.setManaged(false);
        badge.setAlignment(Pos.CENTER);
        badge.setMinWidth(18);
        badge.setPrefWidth(18);
        badge.setMaxWidth(18);

        StackPane bellPane = new StackPane();
        bellPane.getStyleClass().add("notification-bell-pane");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-2, -2, 0, 0));
        bellPane.getChildren().addAll(bell, badge);

        badge.textProperty().bind(Bindings.createStringBinding(
            () -> {
                int c = center.getUnreadCount();
                if (c <= 0) return "0";
                return c > 99 ? "99+" : String.valueOf(c);
            },
            center.unreadCountProperty()
        ));
        badge.visibleProperty().bind(center.unreadCountProperty().greaterThan(0));
        badge.managedProperty().bind(badge.visibleProperty());

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox popContent = new VBox();
        popContent.getStyleClass().add("notification-popover");
        popContent.setPrefWidth(POPOVER_WIDTH);
        popContent.setMaxHeight(POPOVER_MAX_HEIGHT);

        Label headerLabel = new Label("Notifications");
        headerLabel.getStyleClass().add("notification-popover-header");

        ListView<AppNotification> list = new ListView<>();
        list.getStyleClass().add("notification-list");
        list.setFocusTraversable(false);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AppNotification n, boolean empty) {
                super.updateItem(n, empty);
                notificationPopoverListCellUpdateItem(this, n, empty);
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            AppNotification n = list.getSelectionModel().getSelectedItem();
            if (n != null) center.markAsRead(n.getId());
        });

        HBox actions = new HBox(8);
        actions.getStyleClass().add("notification-popover-actions");
        Button markRead = new Button("Mark all read");
        markRead.getStyleClass().add("button-export");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("button-danger");
        actions.getChildren().addAll(markRead, clear);

        markRead.setOnAction(e -> center.markAllAsRead());
        clear.setOnAction(e -> {
            center.clear();
            popup.hide();
        });

        popContent.getChildren().addAll(headerLabel, list, actions);
        VBox.setVgrow(list, Priority.ALWAYS);

        popup.getContent().add(popContent);

        bell.setOnAction(e -> {
            Window w = bell.getScene() != null ? bell.getScene().getWindow() : null;
            if (w == null) return;
            List<AppNotification> recent = center.getNotifications().stream()
                .limit(POPOVER_MAX)
                .collect(Collectors.toList());
            list.getItems().clear();
            list.getItems().addAll(recent);
            popup.show(w);
            popup.setX(w.getX() + w.getWidth() - POPOVER_WIDTH - 24);
            popup.setY(w.getY() + 56);
        });

        container.getChildren().add(0, bellPane);
    }

    /**
     * Popover row rendering (empty vs unread vs read). Package-private for tests without reflecting into {@link ListCell}.
     */
    static void notificationPopoverListCellUpdateItem(ListCell<AppNotification> cell, AppNotification n, boolean empty) {
        if (empty || n == null) {
            cell.setText(null);
            cell.setGraphic(null);
            return;
        }
        Label title = new Label(n.getTitle());
        title.getStyleClass().add("notification-item-title");
        if (!n.isRead()) {
            title.getStyleClass().add("notification-unread");
        }
        Label sub = new Label(n.getMessage() + " · " + n.getTimeFormatted());
        sub.getStyleClass().add("notification-item-sub");
        VBox box = new VBox(2, title, sub);
        box.getStyleClass().add("notification-item-content");
        cell.setGraphic(box);
        cell.setText(null);
    }
}
