package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.RecurrencePattern;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JDBC smoke test against the same H2 schema as Flyway (V1 + V2), without relying on Flyway’s
 * classpath scanning (breaks under IntelliJ + JPMS). SQL is loaded from this module’s resources.
 */
class JdbcAppointmentRepositoryIntegrationTest {

    private static final String MIGRATION_PREFIX = "com/appointmentscheduler/persistence/database/migration/";

    private static HikariDataSource dataSource;

    @BeforeAll
    static void startDb() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:jdbc_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(4);
        dataSource = new HikariDataSource(cfg);

        try (Connection c = dataSource.getConnection()) {
            applyMigrationSql(c, "V1__enterprise_schema.sql");
            applyMigrationSql(c, "V2__booking_request_fields.sql");
        }
    }

    /**
     * Resolves migration files the same way the running app does: module layer first, then classpath URL.
     */
    private static InputStream openMigration(String path) throws IOException {
        InputStream s = JdbcUserRepository.class.getModule().getResourceAsStream(path);
        if (s == null) {
            s = JdbcUserRepository.class.getResourceAsStream("/" + path);
        }
        if (s == null) {
            s = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        }
        return s;
    }

    private static void applyMigrationSql(Connection c, String fileName) throws Exception {
        String path = MIGRATION_PREFIX + fileName;
        InputStream stream = openMigration(path);
        if (stream == null) {
            throw new IllegalStateException(
                    "Cannot read migration (build main resources / IntelliJ out path): " + path);
        }
        try (InputStream s = stream;
             InputStreamReader reader = new InputStreamReader(s, StandardCharsets.UTF_8)) {
            RunScript.execute(c, reader);
        }
    }

    @AfterAll
    static void stopDb() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void saveAndFindById_roundTrip() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-int-1", "Integration", "int@test.com", "hash");
        users.save(patient);

        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a = new InPersonAppointment("appt-int-1", patient,
                new TimeSlot(start, start.plusHours(1)), "Clinic desk");
        a.setStatus("CONFIRMED");
        a.setCustomerNotes("integration test");
        appts.save(a);

        var loaded = appts.findById("appt-int-1");
        assertThat(loaded).isPresent();
        assertEquals("CONFIRMED", loaded.get().getStatus());
        assertEquals("integration test", loaded.get().getCustomerNotes());
        assertEquals(patient.getId(), loaded.get().getPatient().getId());
    }

    @Test
    void jdbcClinicDoctorRoom_roundTrip() {
        JdbcClinicRepository clinics = new JdbcClinicRepository(dataSource);
        clinics.save(null);
        assertThat(clinics.findById(null)).isEmpty();
        Clinic clinic = new Clinic("clinic-int-1", "Integration Clinic", "Addr", "UTC");
        clinics.save(clinic);
        assertThat(clinics.findById("clinic-int-1")).isPresent();
        clinics.save(new Clinic("clinic-int-1", "Updated Clinic", "Addr2", "Europe/London"));
        assertThat(clinics.findById("clinic-int-1")).map(Clinic::getName).contains("Updated Clinic");
        assertThat(clinics.findAll()).isNotEmpty();

        JdbcDoctorRepository doctors = new JdbcDoctorRepository(dataSource);
        doctors.save(null);
        Doctor doctor = new Doctor("doc-int-1", "Dr Int", "dr@int.com", "Spec", 8, "clinic-int-1");
        doctors.save(doctor);
        assertThat(doctors.findById("doc-int-1")).isPresent();

        JdbcRoomRepository roomRepo = new JdbcRoomRepository(dataSource);
        roomRepo.save(null);
        Room room = new Room("room-int-1", "Room A", "clinic-int-1");
        roomRepo.save(room);
        assertThat(roomRepo.findById("room-int-1")).isPresent();
    }

    @Test
    void jdbcUser_findById_null_returnsEmpty() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        assertThat(users.findById(null)).isEmpty();
    }

    @Test
    void jdbcUser_findByEmail_findAll_userTypes() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        assertThat(users.findByEmail(null)).isEmpty();
        users.save(new User("u-patient", "P", "patient@int.com", "h"));
        users.save(new Administrator("u-admin", "A", "admin@int.com", "h"));
        users.save(new DoctorUser("u-doc", "D", "doc@int.com", "h"));
        users.save(new ReceptionistUser("u-rec", "R", "rec@int.com", "h"));
        assertThat(users.findByEmail("PATIENT@INT.COM")).isPresent();
        assertThat(users.findById("u-admin")).isPresent();
        assertThat(users.findAll()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void jdbcAuditEntry_appendAndFind() {
        JdbcAuditEntryRepository audit = new JdbcAuditEntryRepository(dataSource);
        audit.append(null);
        LocalDateTime ts = LocalDateTime.of(2026, 3, 1, 12, 0);
        audit.append(new AuditEntry(ts, "u1", "N", "ACT", "details", "EType", "eid", "old", "new"));
        audit.append(new AuditEntry(ts, "u2", "N2", "X", "d"));
        assertThat(audit.findRecent(1)).hasSize(1);
        assertThat(audit.findRecent(0)).hasSize(1);
        assertThat(audit.findAll().size()).isGreaterThanOrEqualTo(2);
        assertThat(audit.findByUserId(null).size()).isGreaterThanOrEqualTo(2);
        assertThat(audit.findByEntityType("EType")).isNotEmpty();
        assertThat(audit.findByEntityType(null).size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void jdbcUser_saveNull_noOp() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        users.save(null);
    }

    @Test
    void jdbcAppointment_deleteById() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-del-1", "Del", "del@test.com", "h");
        users.save(patient);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime start = LocalDateTime.now().plusDays(5).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a = new InPersonAppointment("appt-del-1", patient, new TimeSlot(start, start.plusHours(1)), "L");
        appts.save(a);
        appts.deleteById("appt-del-1");
        assertThat(appts.findById("appt-del-1")).isEmpty();
    }

    @Test
    void jdbcAppointment_nullGuards_noOp() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        appts.save(null);
        assertThat(appts.findById(null)).isEmpty();
        appts.deleteById(null);
        assertThat(appts.findBlockingBookingsForPatient(null)).isEmpty();
    }

    @Test
    void jdbcAppointment_findBlockingForPatient_and_findAll() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-block-int", "Block", "block-int@test.com", "h");
        users.save(patient);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime t0 = LocalDateTime.now().plusDays(8).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a1 = new InPersonAppointment("appt-block-a", patient, new TimeSlot(t0, t0.plusHours(1)), "L");
        a1.setStatus("CONFIRMED");
        appts.save(a1);
        InPersonAppointment a2 = new InPersonAppointment("appt-block-b", patient, new TimeSlot(t0.plusDays(1), t0.plusDays(1).plusHours(1)), "L");
        a2.setStatus("PENDING");
        appts.save(a2);
        assertThat(appts.findBlockingBookingsForPatient("user-block-int")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(appts.findById("appt-block-a")).isPresent();
    }

    @Test
    void jdbcAppointment_virtual_followUp_group_roundTrip() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-multi-type", "Multi", "multi-int@test.com", "h");
        users.save(patient);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime start = LocalDateTime.now().plusDays(9).withHour(14).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));

        VirtualAppointment virt = new VirtualAppointment("appt-virt-int", patient, slot, "https://meet.example/v");
        virt.setStatus("CONFIRMED");
        appts.save(virt);
        var v = appts.findById("appt-virt-int");
        assertThat(v).isPresent();
        assertThat(v.get()).isInstanceOf(VirtualAppointment.class);
        assertThat(((VirtualAppointment) v.get()).getMeetingLink()).contains("meet.example");

        FollowUpAppointment fu = new FollowUpAppointment("appt-fu-int", patient,
                new TimeSlot(start.plusDays(1), start.plusDays(1).plusHours(1)), "appt-virt-int");
        fu.setStatus("PENDING");
        appts.save(fu);
        var f = appts.findById("appt-fu-int");
        assertThat(f).isPresent();
        assertThat(f.get()).isInstanceOf(FollowUpAppointment.class);
        assertThat(((FollowUpAppointment) f.get()).getPriorAppointmentId()).isEqualTo("appt-virt-int");

        GroupAppointment grp = new GroupAppointment("appt-grp-int", patient,
                new TimeSlot(start.plusDays(2), start.plusDays(2).plusHours(1)), 12);
        grp.setStatus("CONFIRMED");
        appts.save(grp);
        var g = appts.findById("appt-grp-int");
        assertThat(g).isPresent();
        assertThat(g.get()).isInstanceOf(GroupAppointment.class);
        assertThat(((GroupAppointment) g.get()).getMaxCapacity()).isEqualTo(12);
    }

    @Test
    void jdbcAppointment_individual_roundTrip_and_findAll() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-indiv", "Indiv", "indiv@test.com", "h");
        users.save(patient);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime start = LocalDateTime.now().plusDays(10).withHour(11).withMinute(0).withSecond(0).withNano(0);
        IndividualAppointment ind = new IndividualAppointment("appt-indiv-int", patient,
                new TimeSlot(start, start.plusHours(1)));
        ind.setStatus("PENDING");
        appts.save(ind);
        var loaded = appts.findById("appt-indiv-int");
        assertThat(loaded).isPresent();
        assertThat(loaded.get()).isInstanceOf(IndividualAppointment.class);
        assertThat(appts.findAll().stream().anyMatch(a -> "appt-indiv-int".equals(a.getId()))).isTrue();
    }

    @Test
    void jdbcAppointment_recurring_assessment_urgent_roundTrip() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-variants", "Variants", "variants-int@test.com", "h");
        users.save(patient);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime start = LocalDateTime.now().plusDays(12).withHour(7).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        RecurrencePattern rp = new RecurrencePattern(
                RecurrencePattern.Frequency.MONTHLY, start, start.plusMonths(6), 2);
        RecurringAppointment recurring = new RecurringAppointment(
                "appt-recur-int", patient, slot, "SER-INT-1", rp, "OCC-INT-1");
        recurring.setStatus("CONFIRMED");
        appts.save(recurring);
        var loadedRecurring = appts.findById("appt-recur-int");
        assertThat(loadedRecurring).isPresent();
        assertThat(loadedRecurring.get()).isInstanceOf(RecurringAppointment.class);
        assertThat(((RecurringAppointment) loadedRecurring.get()).getSeriesId()).isEqualTo("SER-INT-1");

        LocalDateTime t2 = start.plusHours(2);
        AssessmentAppointment asmt = new AssessmentAppointment("appt-asmt-int", patient,
                new TimeSlot(t2, t2.plusHours(1)));
        asmt.setStatus("PENDING");
        appts.save(asmt);
        var loadedAsmt = appts.findById("appt-asmt-int");
        assertThat(loadedAsmt).isPresent();
        assertThat(loadedAsmt.get()).isInstanceOf(AssessmentAppointment.class);

        LocalDateTime t3 = start.plusHours(4);
        UrgentAppointment urgent = new UrgentAppointment("appt-urg-int", patient,
                new TimeSlot(t3, t3.plusHours(1)));
        urgent.setStatus("CONFIRMED");
        urgent.setUrgent(true);
        appts.save(urgent);
        var loadedUrgent = appts.findById("appt-urg-int");
        assertThat(loadedUrgent).isPresent();
        assertThat(loadedUrgent.get()).isInstanceOf(UrgentAppointment.class);
        assertThat(loadedUrgent.get().isUrgent()).isTrue();
    }

    /**
     * Forces {@link JdbcPostgresHelper#isMySql(Connection)} so repositories use MySQL upsert SQL
     * (H2 in MODE=MySQL accepts ON DUPLICATE KEY UPDATE for the Flyway schema).
     */
    @Test
    void jdbcMysqlDialect_userUpsert_clinicDoctorRoom_andAppointmentSave() {
        DataSource ds = TestJdbcDataSources.withProductName(dataSource, "MySQL");

        JdbcUserRepository users = new JdbcUserRepository(ds);
        User patient = new User("mysql-path-user", "MySqlPath", "mysql-path@test.com", "h");
        users.save(patient);
        users.save(new User("mysql-path-user", "MySqlPathUpdated", "mysql-path@test.com", "h2"));
        assertThat(users.findById("mysql-path-user")).map(User::getName).contains("MySqlPathUpdated");

        JdbcClinicRepository clinics = new JdbcClinicRepository(ds);
        Clinic clinic = new Clinic("mysql-clinic-1", "MySQL Clinic", "Addr", "UTC");
        clinics.save(clinic);

        JdbcDoctorRepository doctors = new JdbcDoctorRepository(ds);
        Doctor doctor = new Doctor("mysql-doc-1", "Dr MySQL", "dr-mysql@test.com", "Spec", 8, "mysql-clinic-1");
        doctors.save(doctor);

        JdbcRoomRepository rooms = new JdbcRoomRepository(ds);
        rooms.save(new Room("mysql-room-1", "Room MySQL", "mysql-clinic-1"));

        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(ds, users);
        LocalDateTime start = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a = new InPersonAppointment("mysql-appt-1", patient,
                new TimeSlot(start, start.plusHours(1)), "Desk");
        a.setStatus("CONFIRMED");
        appts.save(a);
        appts.save(new InPersonAppointment("mysql-appt-1", patient,
                new TimeSlot(start, start.plusHours(1)), "Desk updated"));
        assertThat(appts.findById("mysql-appt-1")).map(InPersonAppointment.class::cast)
                .map(InPersonAppointment::getLocation).contains("Desk updated");
    }

    /**
     * Reports non-MySQL / non-PostgreSQL product name so {@link JdbcAppointmentRepository#save}
     * uses the H2 {@code MERGE INTO ... KEY(id)} path (still backed by the same in-memory H2 DB).
     */
    @Test
    void jdbcMergeDialect_appointmentUpsert_updatesRow() {
        DataSource ds = TestJdbcDataSources.withProductName(dataSource, "H2");

        JdbcUserRepository users = new JdbcUserRepository(ds);
        User patient = new User("merge-path-user", "MergePath", "merge-path@test.com", "h");
        users.save(patient);

        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(ds, users);
        LocalDateTime start = LocalDateTime.now().plusDays(22).withHour(10).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment a = new InPersonAppointment("merge-appt-1", patient,
                new TimeSlot(start, start.plusHours(1)), "First desk");
        a.setStatus("CONFIRMED");
        appts.save(a);
        appts.save(new InPersonAppointment("merge-appt-1", patient,
                new TimeSlot(start, start.plusHours(1)), "Second desk"));
        assertThat(appts.findById("merge-appt-1")).map(InPersonAppointment.class::cast)
                .map(InPersonAppointment::getLocation).contains("Second desk");
    }

    @Test
    void jdbcMergeDialect_clinicUpsert_updatesRow() {
        DataSource ds = TestJdbcDataSources.withProductName(dataSource, "H2");
        JdbcClinicRepository clinics = new JdbcClinicRepository(ds);
        clinics.save(new Clinic("merge-clinic-1", "First name", "Addr1", "UTC"));
        clinics.save(new Clinic("merge-clinic-1", "Second name", "Addr2", "Europe/Paris"));
        assertThat(clinics.findById("merge-clinic-1")).map(Clinic::getName).contains("Second name");
        assertThat(clinics.findById("merge-clinic-1")).map(Clinic::getTimeZone).contains("Europe/Paris");
    }

    @Test
    void jdbcMergeDialect_userUpsert_updatesRow() {
        DataSource ds = TestJdbcDataSources.withProductName(dataSource, "H2");
        JdbcUserRepository users = new JdbcUserRepository(ds);
        users.save(new User("merge-user-h2-1", "FirstName", "merge-user-h2@test.com", "hash1"));
        users.save(new User("merge-user-h2-1", "SecondName", "merge-user-h2@test.com", "hash2"));
        assertThat(users.findById("merge-user-h2-1")).map(User::getName).contains("SecondName");
        assertThat(users.findById("merge-user-h2-1")).map(User::getPassword).contains("hash2");
    }

    @Test
    void jdbcMergeDialect_doctorUpsert_updatesRow() {
        DataSource ds = TestJdbcDataSources.withProductName(dataSource, "H2");
        JdbcClinicRepository clinics = new JdbcClinicRepository(ds);
        clinics.save(new Clinic("merge-dr-clinic", "ClinicForDr", "Addr", "UTC"));
        JdbcDoctorRepository doctors = new JdbcDoctorRepository(ds);
        doctors.save(new Doctor("merge-dr-1", "Dr First", "dr-merge-1@test.com", "Cardio", 8, "merge-dr-clinic"));
        doctors.save(new Doctor("merge-dr-1", "Dr Second", "dr-merge-2@test.com", "Neuro", 9, "merge-dr-clinic"));
        assertThat(doctors.findById("merge-dr-1")).map(Doctor::getName).contains("Dr Second");
        assertThat(doctors.findById("merge-dr-1")).map(Doctor::getSpecialty).contains("Neuro");
        assertThat(doctors.findById("merge-dr-1")).map(Doctor::getMaxAppointmentsPerDay).contains(9);
    }

    @Test
    void jdbcMergeDialect_roomUpsert_updatesRow() {
        DataSource ds = TestJdbcDataSources.withProductName(dataSource, "H2");
        JdbcClinicRepository clinics = new JdbcClinicRepository(ds);
        clinics.save(new Clinic("merge-rm-clinic", "ClinicForRoom", "Addr", "UTC"));
        JdbcRoomRepository rooms = new JdbcRoomRepository(ds);
        rooms.save(new Room("merge-rm-1", "Room First", "merge-rm-clinic"));
        rooms.save(new Room("merge-rm-1", "Room Second", "merge-rm-clinic"));
        assertThat(rooms.findById("merge-rm-1")).map(Room::getName).contains("Room Second");
    }

    @Test
    void jdbcGroupAppointment_maxCapacityZero_mapsToTenOnLoad() {
        JdbcUserRepository users = new JdbcUserRepository(dataSource);
        User patient = new User("user-group-zero", "G", "group-zero@test.com", "h");
        users.save(patient);
        JdbcAppointmentRepository appts = new JdbcAppointmentRepository(dataSource, users);
        LocalDateTime start = LocalDateTime.now().plusDays(21).withHour(15).withMinute(0).withSecond(0).withNano(0);
        GroupAppointment g = new GroupAppointment("appt-grp-zero", patient,
                new TimeSlot(start, start.plusHours(1)), 0);
        g.setStatus("CONFIRMED");
        appts.save(g);
        var loaded = appts.findById("appt-grp-zero");
        assertThat(loaded).isPresent();
        assertThat(loaded.get()).isInstanceOf(GroupAppointment.class);
        assertThat(((GroupAppointment) loaded.get()).getMaxCapacity()).isEqualTo(10);
    }
}
