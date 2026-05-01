package com.appointmentscheduler.presentation.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Single in-app notification with type, priority, read state, and optional entity link.
 */
public class AppNotification {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final String id;
    private final NotificationType type;
    private final NotificationPriority priority;
    private final String title;
    private final String message;
    private final LocalDateTime at;
    private volatile boolean read;
    private final String entityType;
    private final String entityId;

    public AppNotification(NotificationType type, String title, String message) {
        this(type, NotificationPriority.NORMAL, title, message, null, null);
    }

    public AppNotification(NotificationType type, NotificationPriority priority, String title, String message) {
        this(type, priority, title, message, null, null);
    }

    public AppNotification(NotificationType type, NotificationPriority priority, String title, String message,
                           String entityType, String entityId) {
        this.id = UUID.randomUUID().toString();
        this.type = type != null ? type : NotificationType.INFO;
        this.priority = priority != null ? priority : NotificationPriority.NORMAL;
        this.title = title != null ? title : "";
        this.message = message != null ? message : "";
        this.at = LocalDateTime.now();
        this.read = false;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getId() { return id; }
    public NotificationType getType() { return type; }
    public NotificationPriority getPriority() { return priority; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getAt() { return at; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }

    public String getTimeFormatted() {
        return at.format(TIME_FMT);
    }

    public String getDateTimeFormatted() {
        return at.format(DATE_TIME_FMT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppNotification that = (AppNotification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
