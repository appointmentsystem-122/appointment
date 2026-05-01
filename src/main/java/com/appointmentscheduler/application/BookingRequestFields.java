package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;

/**
 * Sanitizes and applies optional booking request fields from the customer UI onto {@link Appointment}.
 */
public final class BookingRequestFields {

    public static final String REMINDER_APP = "APP";
    public static final String REMINDER_EMAIL = "EMAIL";
    public static final String REMINDER_SMS = "SMS";
    public static final String REMINDER_NONE = "NONE";

    public static final String LANG_AR = "AR";
    public static final String LANG_EN = "EN";
    public static final String LANG_ANY = "ANY";

    private BookingRequestFields() {}

    /**
     * Trims and caps lengths; empty strings become null for persistence.
     */
    public static void applyTo(
            Appointment appointment,
            String customerNotes,
            String contactPhone,
            String reminderChannel,
            String accessibilityNeeds,
            String preferredLanguage,
            int partySize,
            int maxParticipantsAllowed) {
        if (appointment == null) return;
        int cap = Math.max(1, maxParticipantsAllowed);
        int p = Math.min(Math.max(1, partySize), cap);
        appointment.setParticipantCount(p);
        appointment.setCustomerNotes(trimToNull(customerNotes, 2000));
        appointment.setContactPhone(trimToNull(contactPhone, 64));
        appointment.setReminderChannel(normalizeReminder(reminderChannel));
        appointment.setAccessibilityNeeds(trimToNull(accessibilityNeeds, 512));
        appointment.setPreferredLanguage(normalizeLanguage(preferredLanguage));
    }

    private static String trimToNull(String s, int maxLen) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > maxLen) {
            return t.substring(0, maxLen);
        }
        return t;
    }

    private static String normalizeReminder(String raw) {
        if (raw == null || raw.isBlank()) return REMINDER_APP;
        String u = raw.trim().toUpperCase();
        return switch (u) {
            case REMINDER_EMAIL, REMINDER_SMS, REMINDER_NONE, REMINDER_APP -> u;
            default -> REMINDER_APP;
        };
    }

    private static String normalizeLanguage(String raw) {
        if (raw == null || raw.isBlank()) return LANG_ANY;
        String u = raw.trim().toUpperCase();
        if (u.startsWith("AR") || u.contains("العرب")) return LANG_AR;
        if (u.startsWith("EN") || u.contains("ENGL")) return LANG_EN;
        return LANG_ANY;
    }
}
