package com.appointmentscheduler.domain.notifiers;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object for a single outbound notification (content and when it was sent).
 */
public final class NotificationMessage {
    private final String content;
    private final LocalDateTime sendTime;

    public NotificationMessage(String content, LocalDateTime sendTime) {
        this.content = Objects.requireNonNull(content, "content");
        this.sendTime = Objects.requireNonNull(sendTime, "sendTime");
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }
}
