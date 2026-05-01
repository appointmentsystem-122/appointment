package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke + branch tests for in-memory persistence used in demos and tests (not JDBC).
 */
@DisplayName("Enterprise in-memory repositories")
class EnterpriseInMemoryRepositoriesTest {

    @Nested
    @DisplayName("Users")
    class Users {

        @Test
        void findByEmail_isCaseInsensitive() {
            InMemoryUserRepository repo = new InMemoryUserRepository();
            User u = new User("uid-e", "Name", "MixedCase@Example.com", "p");
            repo.save(u);
            assertThat(repo.findByEmail("mixedcase@example.com")).contains(u);
            assertThat(repo.findById("uid-e")).contains(u);
            assertThat(repo.findById("missing")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Appointments")
    class Appointments {

        @Test
        void deleteById_removesAndToleratesMissing() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("u1", "N", "e@x.com", "h");
            LocalDateTime s = LocalDateTime.now().plusDays(1);
            InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L");
            repo.save(a);
            String id = a.getId();
            repo.deleteById(id);
            assertThat(repo.findById(id)).isEmpty();
            repo.deleteById("missing");
        }

        @Test
        void saveNull_noop() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            repo.save(null);
            assertThat(repo.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Doctors / Rooms / Clinics")
    class ReferenceData {

        @Test
        void doctorRoundTrip_andNullSaveIgnored() {
            InMemoryDoctorRepository repo = new InMemoryDoctorRepository();
            repo.save(null);
            Doctor d = new Doctor("d1", "Dr", "d@x.com", "Spec", 5);
            repo.save(d);
            assertThat(repo.findById("d1")).contains(d);
            assertThat(repo.findAll()).containsExactly(d);
        }

        @Test
        void roomRoundTrip_andNullSaveIgnored() {
            InMemoryRoomRepository repo = new InMemoryRoomRepository();
            repo.save(null);
            Room r = new Room("r1", "R1");
            repo.save(r);
            assertThat(repo.findById("r1")).contains(r);
            assertThat(repo.findAll()).containsExactly(r);
        }

        @Test
        void clinicRoundTrip_andNullSaveIgnored() {
            InMemoryClinicRepository repo = new InMemoryClinicRepository();
            repo.save(null);
            Clinic c = new Clinic("c1", "Main", "addr", "UTC");
            repo.save(c);
            assertThat(repo.findById("c1")).contains(c);
            assertThat(repo.findAll()).containsExactly(c);
        }
    }
}
