package com.appointmentscheduler.domain.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionTest {

    @Test
    void patientLimitedAdminBroad() {
        assertThat(Role.PATIENT.hasPermission(Permission.BOOK_APPOINTMENT)).isTrue();
        assertThat(Role.PATIENT.hasPermission(Permission.MANAGE_USERS)).isFalse();
        assertThat(Role.ADMINISTRATOR.hasPermission(Permission.MANAGE_USERS)).isTrue();
        assertThat(Role.DOCTOR.hasPermission(Permission.VIEW_ANALYTICS)).isTrue();
        assertThat(Role.RECEPTIONIST.hasPermission(Permission.CANCEL_ANY_APPOINTMENT)).isTrue();
    }

    @Test
    void permissionsSetNonEmpty() {
        for (Role r : Role.values()) {
            assertThat(r.getPermissions()).isNotEmpty();
        }
    }
}
