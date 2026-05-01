package com.appointmentscheduler.domain;

import java.time.LocalDateTime;

/**
 * Value object representing a notification message.
 */
public class NotificationMessage {
    private final String subject;
    private final String content;
    private final LocalDateTime timestamp;

    public NotificationMessage(String subject, String content) {
        this.subject = subject;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp.toString() + "] " + subject + ": " + content;
    }
}
