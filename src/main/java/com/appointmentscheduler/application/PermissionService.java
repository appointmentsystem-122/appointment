package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.authorization.Permission;
import com.appointmentscheduler.domain.authorization.Role;

/**
 * Central authorization service.
 * Enforces permissions before critical actions.
 * Clean separation between authorization logic and UI.
 */
public class PermissionService {

    /**
     * Checks if the user has the specified permission.
     *
     * @param user       the user (may be null for anonymous)
     * @param permission the required permission
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(User user, Permission permission) {
        if (user == null) {
            return false;
        }
        Role role = resolveRole(user);
        return role.hasPermission(permission);
    }

    /**
     * Resolves the role for a given user.
     */
    public Role resolveRole(User user) {
        if (user instanceof Administrator) {
            return Role.ADMINISTRATOR;
        }
        if (user instanceof DoctorUser) {
            return Role.DOCTOR;
        }
        if (user instanceof ReceptionistUser) {
            return Role.RECEPTIONIST;
        }
        return Role.PATIENT;
    }

    /**
     * Throws if the user lacks the permission.
     *
     * @param user       the user
     * @param permission the required permission
     * @throws SecurityException if permission is denied
     */
    public void requirePermission(User user, Permission permission) {
        if (!hasPermission(user, permission)) {
            throw new SecurityException("Permission denied: " + permission + " for user " + (user != null ? user.getId() : "anonymous"));
        }
    }
}
