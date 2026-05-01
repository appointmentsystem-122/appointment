package com.appointmentscheduler.domain.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionSmokeTest {

    @Test
    void allPermissionsListed() {
        assertThat(Permission.values().length).isGreaterThan(10);
        assertThat(Permission.BOOK_APPOINTMENT.name()).contains("BOOK");
    }
}
