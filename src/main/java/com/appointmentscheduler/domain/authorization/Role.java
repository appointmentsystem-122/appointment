package com.appointmentscheduler.domain.authorization;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Defines roles and their associated permissions.
 * Roles are immutable; permissions are assigned per role.
 */
public enum Role {
    PATIENT(
        Permission.BOOK_APPOINTMENT,
        Permission.MODIFY_OWN_APPOINTMENT,
        Permission.CANCEL_OWN_APPOINTMENT,
        Permission.VIEW_OWN_APPOINTMENTS
    ),
    ADMINISTRATOR(
        Permission.BOOK_APPOINTMENT,
        Permission.MODIFY_OWN_APPOINTMENT,
        Permission.MODIFY_ANY_APPOINTMENT,
        Permission.CANCEL_OWN_APPOINTMENT,
        Permission.CANCEL_ANY_APPOINTMENT,
        Permission.VIEW_ALL_APPOINTMENTS,
        Permission.VIEW_OWN_APPOINTMENTS,
        Permission.VIEW_REPORTS,
        Permission.VIEW_AUDIT_LOG,
        Permission.MANAGE_USERS,
        Permission.EXPORT_DATA,
        Permission.MANAGE_SETTINGS,
        Permission.MANAGE_DOCTORS,
        Permission.MANAGE_ROOMS,
        Permission.VIEW_ANALYTICS
    ),
    DOCTOR(
        Permission.BOOK_APPOINTMENT,
        Permission.VIEW_OWN_APPOINTMENTS,
        Permission.VIEW_ALL_APPOINTMENTS,
        Permission.MODIFY_OWN_APPOINTMENT,
        Permission.VIEW_ANALYTICS
    ),
    RECEPTIONIST(
        Permission.BOOK_APPOINTMENT,
        Permission.MODIFY_OWN_APPOINTMENT,
        Permission.MODIFY_ANY_APPOINTMENT,
        Permission.CANCEL_OWN_APPOINTMENT,
        Permission.CANCEL_ANY_APPOINTMENT,
        Permission.VIEW_ALL_APPOINTMENTS,
        Permission.VIEW_OWN_APPOINTMENTS,
        Permission.EXPORT_DATA,
        Permission.VIEW_ANALYTICS
    );

    private final Set<Permission> permissions;

    Role(Permission... perms) {
        this.permissions = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(perms)));
    }

    /**
     * Returns the set of permissions granted to this role.
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Checks if this role includes the given permission.
     */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
