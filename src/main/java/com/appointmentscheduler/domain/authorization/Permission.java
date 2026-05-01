package com.appointmentscheduler.domain.authorization;

/**
 * Enterprise role-based permission constants.
 * Centralizes all authorization checks.
 */
public enum Permission {
    /** Book a new appointment (patient or admin) */
    BOOK_APPOINTMENT,
    /** Modify own appointments */
    MODIFY_OWN_APPOINTMENT,
    /** Modify any user's appointment (admin only) */
    MODIFY_ANY_APPOINTMENT,
    /** Cancel own appointments */
    CANCEL_OWN_APPOINTMENT,
    /** Cancel any appointment (admin only) */
    CANCEL_ANY_APPOINTMENT,
    /** View all appointments in the system */
    VIEW_ALL_APPOINTMENTS,
    /** View own appointments only */
    VIEW_OWN_APPOINTMENTS,
    /** Access reporting and analytics */
    VIEW_REPORTS,
    /** Access audit log */
    VIEW_AUDIT_LOG,
    /** Manage users and permissions */
    MANAGE_USERS,
    /** Export data (CSV, etc.) */
    EXPORT_DATA,
    /** Access system settings */
    MANAGE_SETTINGS,
    /** Manage doctors and schedules */
    MANAGE_DOCTORS,
    /** Manage rooms and resources */
    MANAGE_ROOMS,
    /** View analytics and dashboards */
    VIEW_ANALYTICS
}
