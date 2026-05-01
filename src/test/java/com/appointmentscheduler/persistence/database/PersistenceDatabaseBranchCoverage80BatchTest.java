package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.RecurrencePattern;
import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Single batch of high-branch-value JDBC tests targeting ~80% branch coverage for
 * {@code com.appointmentscheduler.persistence.database} (multi-row loops, success paths, mapRow edges).
 */
class PersistenceDatabaseBranchCoverage80BatchTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 1, 9, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final User PAT = new User("p-batch", "P", "p@batch.com", "h");

    private static Connection h2Conn() throws SQLException {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        return c;
    }

    @Test
    void clinic_findAll_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("clinic") && sql.contains("ORDER BY name"))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("id")).thenReturn("c1", "c2");
        when(rs.getString("name")).thenReturn("A", "B");
        when(rs.getString("address")).thenReturn("a1", "a2");
        when(rs.getString("time_zone")).thenReturn("UTC", "Europe/Berlin");

        List<Clinic> list = new JdbcClinicRepository(ds).findAll();
        assertThat(list).hasSize(2);
    }

    @Test
    void doctor_findAll_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("doctor") && sql.contains("ORDER BY name"))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("id")).thenReturn("d1", "d2");
        when(rs.getString("name")).thenReturn("Dr1", "Dr2");
        when(rs.getString("email")).thenReturn("d1@x.com", "d2@x.com");
        when(rs.getString("specialty")).thenReturn("S1", "S2");
        when(rs.getInt("max_appointments_per_day")).thenReturn(5, 6);
        when(rs.getString("clinic_id")).thenReturn("cl1", "cl2");

        assertThat(new JdbcDoctorRepository(ds).findAll()).hasSize(2);
    }

    @Test
    void room_findAll_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("room") && sql.contains("ORDER BY name"))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("id")).thenReturn("r1", "r2");
        when(rs.getString("name")).thenReturn("Room1", "Room2");
        when(rs.getString("clinic_id")).thenReturn("c1", "c1");

        assertThat(new JdbcRoomRepository(ds).findAll()).hasSize(2);
    }

    @Test
    void audit_findRecent_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LIMIT")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 12, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("u1", "u2");
        when(rs.getString("user_name")).thenReturn("a", "b");
        when(rs.getString("action")).thenReturn("X", "Y");
        when(rs.getString("details")).thenReturn("d", "d");
        when(rs.getString("entity_type")).thenReturn("E", "E");
        when(rs.getString("entity_id")).thenReturn("e1", "e2");
        when(rs.getString("old_value")).thenReturn(null, null);
        when(rs.getString("new_value")).thenReturn(null, null);

        assertThat(new JdbcAuditEntryRepository(ds).findRecent(10)).hasSize(2);
    }

    @Test
    void audit_findAll_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY id DESC")
                && !sql.contains("WHERE")
                && !sql.contains("LIMIT")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 2, 1, 8, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("u1", "u2");
        when(rs.getString("user_name")).thenReturn("n1", "n2");
        when(rs.getString("action")).thenReturn("A1", "A2");
        when(rs.getString("details")).thenReturn("x", "y");
        when(rs.getString("entity_type")).thenReturn("T", "T");
        when(rs.getString("entity_id")).thenReturn("e1", "e2");
        when(rs.getString("old_value")).thenReturn(null, null);
        when(rs.getString("new_value")).thenReturn(null, null);

        assertThat(new JdbcAuditEntryRepository(ds).findAll()).hasSize(2);
    }

    @Test
    void audit_findByUserId_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE user_id = ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 3, 1, 9, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("uid", "uid");
        when(rs.getString("user_name")).thenReturn("n", "n");
        when(rs.getString("action")).thenReturn("A", "B");
        when(rs.getString("details")).thenReturn("d", "d");
        when(rs.getString("entity_type")).thenReturn("E", "E");
        when(rs.getString("entity_id")).thenReturn("e1", "e2");
        when(rs.getString("old_value")).thenReturn(null, null);
        when(rs.getString("new_value")).thenReturn(null, null);

        assertThat(new JdbcAuditEntryRepository(ds).findByUserId("uid")).hasSize(2);
    }

    @Test
    void audit_findByEntityType_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE entity_type = ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 4, 1, 10, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("u1", "u2");
        when(rs.getString("user_name")).thenReturn("n", "n");
        when(rs.getString("action")).thenReturn("A", "A");
        when(rs.getString("details")).thenReturn("d", "d");
        when(rs.getString("entity_type")).thenReturn("BOOK", "BOOK");
        when(rs.getString("entity_id")).thenReturn("e1", "e2");
        when(rs.getString("old_value")).thenReturn(null, null);
        when(rs.getString("new_value")).thenReturn(null, null);

        assertThat(new JdbcAuditEntryRepository(ds).findByEntityType("BOOK")).hasSize(2);
    }

    @Test
    void user_findAll_twoRows_mixedTypes() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("id")).thenReturn("u1", "u2");
        when(rs.getString("name")).thenReturn("N1", "N2");
        when(rs.getString("email")).thenReturn("a@x.com", "b@x.com");
        when(rs.getString("password_hash")).thenReturn("h", "h");
        when(rs.getString("user_type")).thenReturn("PATIENT", "ADMINISTRATOR");

        List<User> users = new JdbcUserRepository(ds).findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    void user_findByEmail_unknownUserType_mapsDefaultUser() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("ux");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("unk@x.com");
        when(rs.getString("password_hash")).thenReturn("h");
        when(rs.getString("user_type")).thenReturn("UNKNOWN_CUSTOM_ROLE");

        Optional<User> u = new JdbcUserRepository(ds).findByEmail("unk@x.com");
        assertThat(u).isPresent();
        assertThat(u.get()).isExactlyInstanceOf(User.class);
    }

    @Test
    void appointment_findById_noRow_returnsEmpty() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcAppointmentRepository(ds, users).findById("missing")).isEmpty();
    }

    @Test
    void appointment_deleteById_success() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("DELETE FROM")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).deleteById("del-ok");
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_findAll_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY start_time")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("id")).thenReturn("a1", "a2");
        when(rs.getString("patient_id")).thenReturn("p-batch", "p-batch");
        when(users.findById("p-batch")).thenReturn(Optional.of(PAT));
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(T0, T0.plusHours(2));
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(T1, T1.plusHours(2));
        when(rs.getString("appointment_type")).thenReturn("IN_PERSON", "IN_PERSON");
        when(rs.getString("location")).thenReturn("L1", "L2");
        when(rs.getString("status")).thenReturn("CONFIRMED", "PENDING");
        when(rs.getInt("participant_count")).thenReturn(1, 2);
        when(rs.getBoolean("deleted")).thenReturn(false, false);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null, null);
        when(rs.getString("deleted_by")).thenReturn(null, null);
        when(rs.getString("doctor_id")).thenReturn("d1", "d2");
        when(rs.getString("room_id")).thenReturn("r1", "r2");
        when(rs.getString("clinic_id")).thenReturn("c1", "c1");
        when(rs.getBoolean("urgent")).thenReturn(false, false);
        when(rs.getString("customer_notes")).thenReturn(null, null);
        when(rs.getString("contact_phone")).thenReturn(null, null);
        when(rs.getString("reminder_channel")).thenReturn(null, null);
        when(rs.getString("accessibility_needs")).thenReturn(null, null);
        when(rs.getString("preferred_language")).thenReturn(null, null);
        when(rs.wasNull()).thenReturn(true, true);

        assertThat(new JdbcAppointmentRepository(ds, users).findAll()).hasSize(2);
    }

    @Test
    void appointment_findAll_threeRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY start_time")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getString("id")).thenReturn("a1", "a2", "a3");
        when(rs.getString("patient_id")).thenReturn("p-batch", "p-batch", "p-batch");
        when(users.findById("p-batch")).thenReturn(Optional.of(PAT));
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(T0, T0.plusHours(2), T0.plusHours(4));
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(T1, T1.plusHours(2), T1.plusHours(4));
        when(rs.getString("appointment_type")).thenReturn("INDIVIDUAL", "INDIVIDUAL", "INDIVIDUAL");
        when(rs.getString("status")).thenReturn("CONFIRMED", "PENDING", "COMPLETED");
        when(rs.getInt("participant_count")).thenReturn(1, 2, 3);
        when(rs.getBoolean("deleted")).thenReturn(false, false, false);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null, null, null);
        when(rs.getString("deleted_by")).thenReturn(null, null, null);
        when(rs.getString("doctor_id")).thenReturn("d1", "d2", "d3");
        when(rs.getString("room_id")).thenReturn("r1", "r2", "r3");
        when(rs.getString("clinic_id")).thenReturn("c1", "c1", "c1");
        when(rs.getBoolean("urgent")).thenReturn(false, false, true);
        when(rs.getString("customer_notes")).thenReturn(null, null, null);
        when(rs.getString("contact_phone")).thenReturn(null, null, null);
        when(rs.getString("reminder_channel")).thenReturn(null, null, null);
        when(rs.getString("accessibility_needs")).thenReturn(null, null, null);
        when(rs.getString("preferred_language")).thenReturn(null, null, null);
        when(rs.wasNull()).thenReturn(true, true, true);

        assertThat(new JdbcAppointmentRepository(ds, users).findAll()).hasSize(3);
    }

    @Test
    void user_findByEmail_noRow_returnsEmpty() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcUserRepository(ds).findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void appointment_findById_group_negativeMaxCapacity_defaultsToTen() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("g-neg");
        when(rs.getString("patient_id")).thenReturn("p-batch");
        when(users.findById("p-batch")).thenReturn(Optional.of(PAT));
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(T0);
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(T1);
        when(rs.getString("appointment_type")).thenReturn("GROUP");
        when(rs.getInt("max_capacity")).thenReturn(-3);
        when(rs.getString("status")).thenReturn("CONFIRMED");
        when(rs.getInt("participant_count")).thenReturn(2);
        when(rs.getBoolean("deleted")).thenReturn(false);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null);
        when(rs.getString("deleted_by")).thenReturn(null);
        when(rs.getString("doctor_id")).thenReturn("d");
        when(rs.getString("room_id")).thenReturn("r");
        when(rs.getString("clinic_id")).thenReturn("c");
        when(rs.getBoolean("urgent")).thenReturn(false);
        when(rs.getString("customer_notes")).thenReturn(null);
        when(rs.getString("contact_phone")).thenReturn(null);
        when(rs.getString("reminder_channel")).thenReturn(null);
        when(rs.getString("accessibility_needs")).thenReturn(null);
        when(rs.getString("preferred_language")).thenReturn(null);
        when(rs.wasNull()).thenReturn(true);

        var a = new JdbcAppointmentRepository(ds, users).findById("g-neg").orElseThrow();
        assertThat(a).isInstanceOf(GroupAppointment.class);
        assertThat(((GroupAppointment) a).getMaxCapacity()).isEqualTo(10);
    }

    @Test
    void appointment_findBlocking_twoRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("patient_id = ?") && sql.contains("PENDING"))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("id")).thenReturn("b1", "b2");
        when(rs.getString("patient_id")).thenReturn("p-batch", "p-batch");
        when(users.findById("p-batch")).thenReturn(Optional.of(PAT));
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(T0, T0.plusHours(3));
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(T1, T1.plusHours(3));
        when(rs.getString("appointment_type")).thenReturn("IN_PERSON", "IN_PERSON");
        when(rs.getString("location")).thenReturn("L1", "L2");
        when(rs.getString("status")).thenReturn("PENDING", "CONFIRMED");
        when(rs.getInt("participant_count")).thenReturn(1, 1);
        when(rs.getBoolean("deleted")).thenReturn(false, false);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null, null);
        when(rs.getString("deleted_by")).thenReturn(null, null);
        when(rs.getString("doctor_id")).thenReturn("d", "d");
        when(rs.getString("room_id")).thenReturn("r", "r");
        when(rs.getString("clinic_id")).thenReturn("c", "c");
        when(rs.getBoolean("urgent")).thenReturn(false, false);
        when(rs.getString("customer_notes")).thenReturn(null, null);
        when(rs.getString("contact_phone")).thenReturn(null, null);
        when(rs.getString("reminder_channel")).thenReturn(null, null);
        when(rs.getString("accessibility_needs")).thenReturn(null, null);
        when(rs.getString("preferred_language")).thenReturn(null, null);
        when(rs.wasNull()).thenReturn(true, true);

        assertThat(new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient("p-batch")).hasSize(2);
    }

    @Test
    void clinic_findById_success_nonNullTimeZone() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("c-ok");
        when(rs.getString("name")).thenReturn("Clinic");
        when(rs.getString("address")).thenReturn("Addr");
        when(rs.getString("time_zone")).thenReturn("Asia/Tokyo");

        Clinic cl = new JdbcClinicRepository(ds).findById("c-ok").orElseThrow();
        assertThat(cl.getTimeZone()).isEqualTo("Asia/Tokyo");
    }

    @Test
    void jdbcPostgresHelper_table_oracleUnqualified() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("Oracle");
        assertThat(JdbcPostgresHelper.table(c, "clinic")).isEqualTo("clinic");
    }

    @Test
    void user_findById_unknownUserType_mapsDefaultUser() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("id-unk");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("h");
        when(rs.getString("user_type")).thenReturn("WEIRD_TYPE");

        User u = new JdbcUserRepository(ds).findById("id-unk").orElseThrow();
        assertThat(u.getClass()).isEqualTo(User.class);
    }

    @Test
    void appointment_save_mysql_skipsTimestampParam() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("a-my-ts", PAT, new TimeSlot(T0, T1), "Loc"));
        verify(ps, never()).setTimestamp(anyInt(), any(Timestamp.class));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_postgres_setsTimestamp() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")
                && sql.contains("appointment.appointment")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("a-pg-ts", PAT, new TimeSlot(T0, T1), "Loc"));
        verify(ps).setTimestamp(anyInt(), any(Timestamp.class));
        verify(ps).executeUpdate();
    }

    @Test
    void audit_append_success_executesInsert() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAuditEntryRepository(ds).append(new AuditEntry(LocalDateTime.now(), "u", "n", "ACT", "details"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_h2_virtual_merges() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new VirtualAppointment("a-virt", PAT, new TimeSlot(T0, T1), "https://meet/x"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_h2_followUp_merges() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new FollowUpAppointment("a-fu", PAT, new TimeSlot(T0, T1), "prior-id"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_h2_group_merges() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new GroupAppointment("a-gr", PAT, new TimeSlot(T0, T1), 12));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_h2_recurring_withPattern_merges() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        RecurrencePattern rp = new RecurrencePattern(
                RecurrencePattern.Frequency.WEEKLY, T0.minusWeeks(1), T1.plusYears(1), 2);
        new JdbcAppointmentRepository(ds, users).save(
                new RecurringAppointment("a-rec", PAT, new TimeSlot(T0, T1), "series-x", rp, "occ-x"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_h2_assessment_and_urgent_merges() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new AssessmentAppointment("a-as", PAT, new TimeSlot(T0, T1)));
        new JdbcAppointmentRepository(ds, users).save(new UrgentAppointment("a-ur", PAT, new TimeSlot(T0, T1)));
        verify(ps, org.mockito.Mockito.times(2)).executeUpdate();
    }

    @Test
    void appointment_save_postgres_virtual_onConflict() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")
                && sql.contains("appointment.appointment")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new VirtualAppointment("v-pg", PAT, new TimeSlot(T0, T1), "https://z"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_postgres_assessment_onConflict() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")
                && sql.contains("appointment.appointment")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new AssessmentAppointment("as-pg", PAT, new TimeSlot(T0, T1)));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_postgres_inPerson_onConflict() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")
                && sql.contains("appointment.appointment")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("ip-pg", PAT, new TimeSlot(T0, T1), "Wing B"));
        verify(ps).executeUpdate();
    }

    @Test
    void doctor_findById_success_mapsRow() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("doctor") && sql.contains("WHERE id ="))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("d-ok");
        when(rs.getString("name")).thenReturn("Dr X");
        when(rs.getString("email")).thenReturn("dx@x.com");
        when(rs.getString("specialty")).thenReturn("S");
        when(rs.getInt("max_appointments_per_day")).thenReturn(8);
        when(rs.getString("clinic_id")).thenReturn("cl1");

        Doctor d = new JdbcDoctorRepository(ds).findById("d-ok").orElseThrow();
        assertThat(d.getName()).isEqualTo("Dr X");
    }

    @Test
    void room_findById_success_mapsRow() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Conn();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("room") && sql.contains("WHERE id ="))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("r-ok");
        when(rs.getString("name")).thenReturn("Room A");
        when(rs.getString("clinic_id")).thenReturn("c1");

        Room r = new JdbcRoomRepository(ds).findById("r-ok").orElseThrow();
        assertThat(r.getName()).isEqualTo("Room A");
    }

    @Test
    void user_save_mysql_administrator_onDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new Administrator("adm-b", "Admin", "a@b.com", "pw"));
        verify(ps).setString(5, "ADMINISTRATOR");
        verify(ps).executeUpdate();
    }

    @Test
    void jdbcPostgresHelper_auroraMysql_detectedAsMySql() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("Amazon Aurora MySQL");
        assertThat(JdbcPostgresHelper.isMySql(c)).isTrue();
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
    }

    @Test
    void jdbcPostgresHelper_crdbPostgres_detectedAsPostgres() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("CockroachDB CCL v23 (postgres wire)");
        assertThat(JdbcPostgresHelper.isPostgres(c)).isTrue();
    }

    @Test
    void jdbcPostgresHelper_mariadbSubstring_caseInsensitive() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MARIADB");
        assertThat(JdbcPostgresHelper.isMySql(c)).isTrue();
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.table(c, "appointment")).isEqualTo("appointment");
    }

    @Test
    void jdbcPostgresHelper_nullMetaData_treatsProductAsEmpty() throws Exception {
        Connection c = mock(Connection.class);
        when(c.getMetaData()).thenReturn(null);
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
        assertThat(JdbcPostgresHelper.table(c, "clinic")).isEqualTo("clinic");
    }

    @Test
    void jdbcPostgresHelper_nullProductName_fromMetaData_isNeitherPostgresNorMySql() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn(null);
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
    }
}
