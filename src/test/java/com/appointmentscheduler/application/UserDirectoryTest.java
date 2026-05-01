package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDirectoryTest {

    @Test
    void nullUserIsNeitherStaffNorPatient() {
        assertThat(UserDirectory.isStaff(null)).isFalse();
        assertThat(UserDirectory.isPatient(null)).isFalse();
    }

    @Test
    void staffRoles() {
        assertThat(UserDirectory.isStaff(new Administrator("a", "A", "a@x.com", "p"))).isTrue();
        assertThat(UserDirectory.isStaff(new ReceptionistUser("r", "R", "r@x.com", "p"))).isTrue();
        assertThat(UserDirectory.isStaff(new DoctorUser("d", "D", "d@x.com", "p"))).isTrue();
        assertThat(UserDirectory.isPatient(new Administrator("a", "A", "a@x.com", "p"))).isFalse();
    }

    @Test
    void regularUserIsPatientNotStaff() {
        User u = new User("u", "U", "u@x.com", "p");
        assertThat(UserDirectory.isStaff(u)).isFalse();
        assertThat(UserDirectory.isPatient(u)).isTrue();
    }
}
