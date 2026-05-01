package com.appointmentscheduler.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Single audit log entry for enterprise activity trail.
 */
public class AuditEntry {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final String userId;
    private final String userName;
    private final String action;
    private final String details;
    private final String entityType;
    private final String entityId;
    private final String oldValue;
    private final String newValue;

    public AuditEntry(LocalDateTime timestamp, String userId, String userName, String action, String details) {
        this(timestamp, userId, userName, action, details, null, null, null, null);
    }

    public AuditEntry(LocalDateTime timestamp, String userId, String userName, String action, String details,
                      String entityType, String entityId, String oldValue, String newValue) {
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.userId = userId != null ? userId : "";
        this.userName = userName != null ? userName : "";
        this.action = action != null ? action : "";
        this.details = details != null ? details : "";
        this.entityType = entityType != null ? entityType : "";
        this.entityId = entityId != null ? entityId : "";
        this.oldValue = oldValue != null ? oldValue : "";
        this.newValue = newValue != null ? newValue : "";
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }

    public String getTimestampFormatted() {
        return timestamp.format(FMT);
    }
}
