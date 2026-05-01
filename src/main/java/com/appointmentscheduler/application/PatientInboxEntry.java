package com.appointmentscheduler.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * In-app message shown in the customer inbox (e.g. broadcast from staff).
 */
public final class PatientInboxEntry {

    private static final DateTimeFormatter META_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String id;
    private final String title;
    private final String body;
    private final LocalDateTime receivedAt;
    private final String senderLabel;

    public PatientInboxEntry(String title, String body, LocalDateTime receivedAt, String senderLabel) {
        this.id = UUID.randomUUID().toString();
        this.title = title != null ? title : "";
        this.body = body != null ? body : "";
        this.receivedAt = receivedAt != null ? receivedAt : LocalDateTime.now();
        this.senderLabel = senderLabel != null ? senderLabel : "Organization";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public String getSenderLabel() {
        return senderLabel;
    }

    public String summaryLine() {
        return receivedAt.toString().replace('T', ' ') + " · " + title;
    }

    /** Single-line meta for UI: sender and received time. */
    public String metaLine() {
        return "From " + senderLabel + " · " + receivedAt.format(META_TIME);
    }

    /** Truncated body for list preview (full text in tooltip). */
    public String bodyPreview(int maxChars) {
        if (maxChars <= 0 || body.isEmpty()) return body;
        String t = body.replace('\n', ' ').trim();
        if (t.length() <= maxChars) return t;
        return t.substring(0, maxChars).trim() + "…";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientInboxEntry that = (PatientInboxEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
