package com.appointmentscheduler.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Inbound message from a customer to staff (contact form), shown in the admin messaging hub.
 */
public final class StaffContactMessage {

    private static final DateTimeFormatter META_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String id;
    private final String subject;
    private final String body;
    private final LocalDateTime receivedAt;
    private final String customerId;
    private final String customerName;
    private final String customerEmail;

    public StaffContactMessage(String subject, String body, LocalDateTime receivedAt,
                               String customerId, String customerName, String customerEmail) {
        this.id = UUID.randomUUID().toString();
        this.subject = subject != null ? subject : "";
        this.body = body != null ? body : "";
        this.receivedAt = receivedAt != null ? receivedAt : LocalDateTime.now();
        this.customerId = customerId != null ? customerId : "";
        this.customerName = customerName != null ? customerName : "";
        this.customerEmail = customerEmail != null ? customerEmail : "";
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String metaLine() {
        return receivedAt.format(META_TIME) + " · " + customerName + " <" + customerEmail + ">";
    }

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
        StaffContactMessage that = (StaffContactMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
