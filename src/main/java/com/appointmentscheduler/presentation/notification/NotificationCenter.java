package com.appointmentscheduler.presentation.notification;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Central notification hub: in-memory store with observable list and unread count.
 * Thread-safe; use from any thread; UI updates run on JavaFX thread.
 */
public final class NotificationCenter {
    private static final Logger log = LoggerFactory.getLogger(NotificationCenter.class);
    private static final int MAX_ITEMS = 200;
    private static NotificationCenter instance;

    private final ObservableList<AppNotification> allNotifications = FXCollections.observableArrayList();
    private final CopyOnWriteArrayList<Runnable> unreadCountListeners = new CopyOnWriteArrayList<>();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    public static synchronized NotificationCenter getInstance() {
        if (instance == null) {
            instance = new NotificationCenter();
        }
        return instance;
    }

    private NotificationCenter() {
        allNotifications.addListener((ListChangeListener<AppNotification>) c -> updateUnreadCount());
    }

    /**
     * Publish a new notification. Safe to call from background threads.
     */
    public void notify(NotificationType type, String title, String message) {
        notify(type, NotificationPriority.NORMAL, title, message, null, null);
    }

    public void notify(NotificationType type, NotificationPriority priority, String title, String message) {
        notify(type, priority, title, message, null, null);
    }

    public void notify(NotificationType type, NotificationPriority priority, String title, String message,
                       String entityType, String entityId) {
        AppNotification n = new AppNotification(type, priority, title, message, entityType, entityId);
        Platform.runLater(() -> {
            allNotifications.add(n);
            allNotifications.sort(Comparator.comparing(AppNotification::getAt).reversed());
            while (allNotifications.size() > MAX_ITEMS) {
                allNotifications.remove(allNotifications.size() - 1);
            }
        });
    }

    public ObservableList<AppNotification> getNotifications() {
        return FXCollections.unmodifiableObservableList(allNotifications);
    }

    /**
     * Returns notifications for display (newest first), optionally only unread.
     */
    public ObservableList<AppNotification> getRecent(int max, boolean unreadOnly) {
        List<AppNotification> list = allNotifications.stream()
                .filter(unreadOnly ? n -> !n.isRead() : n -> true)
                .limit(max)
                .collect(Collectors.toList());
        return FXCollections.observableArrayList(list);
    }

    public void markAsRead(String id) {
        Platform.runLater(() -> {
            allNotifications.stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .ifPresent(n -> n.setRead(true));
            updateUnreadCount();
        });
    }

    public void markAllAsRead() {
        Platform.runLater(() -> {
            allNotifications.forEach(n -> n.setRead(true));
            updateUnreadCount();
        });
    }

    public void clear() {
        Platform.runLater(() -> {
            allNotifications.clear();
            updateUnreadCount();
        });
    }

    public int getUnreadCount() {
        return (int) allNotifications.stream().filter(n -> !n.isRead()).count();
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    private void updateUnreadCount() {
        int count = getUnreadCount();
        unreadCount.set(count);
        for (Runnable r : unreadCountListeners) {
            try {
                r.run();
            } catch (RuntimeException ex) {
                // Listener failures must not break notification state propagation.
                log.warn("Unread-count listener failed", ex);
            }
        }
    }

    public void addUnreadCountListener(Runnable listener) {
        if (listener != null) unreadCountListeners.add(listener);
    }
}