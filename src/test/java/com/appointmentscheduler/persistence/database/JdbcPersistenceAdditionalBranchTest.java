package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional JDBC dialect and error-path coverage for persistence.database (MySQL / PostgreSQL
 * upsert branches not taken by the H2-only save tests).
 */
class JdbcPersistenceAdditionalBranchTest {

    private static final LocalDateTime S = LocalDateTime.of(2026, 9, 1, 10, 0);
    private static final TimeSlot SLOT = new TimeSlot(S, S.plusHours(1));
    private static final User P = new User("p-jdbc", "P", "p@x.com", "h");

    @Test
    void room_save_whenMySql_usesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcRoomRepository(ds).save(new Room("r1", "R1", "c1"));
        verify(ps).executeUpdate();
    }

    @Test
    void doctor_save_whenMySql_usesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcDoctorRepository(ds).save(new Doctor("d1", "Dr", "d@x.com", "Sp", 5, "c1"));
        verify(ps).executeUpdate();
    }

    @Test
    void clinic_save_whenMySql_usesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcClinicRepository(ds).save(new Clinic("c1", "Main", "addr", "UTC"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_whenMySql_usesOnDuplicateKey() throws Exception {
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

        new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("a-mysql", P, SLOT, "L"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_whenPostgres_usesOnConflictUpsert() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("a-pg", P, SLOT, "L"));
        verify(ps).executeUpdate();
    }

    @Test
    void appointment_save_sqlException_chainsNextExceptionMessage() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        SQLException root = new SQLException("root");
        SQLException next = new SQLException("next detail");
        root.setNextException(next);
        when(ps.executeUpdate()).thenThrow(root);

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("a-err", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("a-err")
                .hasMessageContaining("next detail");
    }

    @Test
    void user_findById_postgres_invalidUuid36Chars_fallsBackToString() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        String bad36 = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz";
        assertThat(new JdbcUserRepository(ds).findById(bad36)).isEmpty();
        verify(ps).setString(1, bad36);
    }

    @Test
    void user_findByEmail_mapsRow_nullUserType_defaultsToPatient() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LOWER(email)")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("u1");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("h");
        when(rs.getString("user_type")).thenReturn(null);

        assertThat(new JdbcUserRepository(ds).findByEmail("e@e.com"))
                .isPresent()
                .get()
                .isInstanceOf(User.class);
    }

    @Test
    void audit_findRecent_zeroMax_usesAtLeastOne() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LIMIT")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        new JdbcAuditEntryRepository(ds).findRecent(0);
        verify(ps).setInt(1, 1);
    }

    @Test
    void appointment_mapRow_optionalColumn_getStringThrows_skipsSetters() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("aid-x");
        when(rs.getString("patient_id")).thenReturn("pid-mock");
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(S);
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(S.plusHours(1));
        when(rs.getString("appointment_type")).thenReturn("IN_PERSON");
        when(rs.getString("location")).thenReturn("Hall");
        when(rs.getString("status")).thenReturn("CONFIRMED");
        when(rs.getInt("participant_count")).thenReturn(1);
        when(rs.getBoolean("deleted")).thenReturn(false);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null);
        when(rs.getString("deleted_by")).thenReturn(null);
        when(rs.getString("doctor_id")).thenReturn("d");
        when(rs.getString("room_id")).thenReturn("r");
        when(rs.getString("clinic_id")).thenReturn("c");
        when(rs.getBoolean("urgent")).thenReturn(false);
        when(rs.getString("customer_notes")).thenThrow(new SQLException("no column"));
        when(rs.getString("contact_phone")).thenReturn(null);
        when(rs.getString("reminder_channel")).thenReturn(null);
        when(rs.getString("accessibility_needs")).thenReturn(null);
        when(rs.getString("preferred_language")).thenReturn(null);
        when(rs.wasNull()).thenReturn(true);

        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(users.findById("pid-mock")).thenReturn(Optional.of(P));

        Appointment a = new JdbcAppointmentRepository(ds, users).findById("aid-x").orElseThrow();
        assertThat(a.getCustomerNotes()).isNull();
    }

    @Test
    void recurring_mapRow_intervalZero_usesFallbackPattern() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("rec-fb");
        when(rs.getString("patient_id")).thenReturn("pid-mock");
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(S);
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(S.plusHours(1));
        when(rs.getString("appointment_type")).thenReturn("RECURRING");
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");
        when(rs.getString("rec_frequency")).thenReturn("WEEKLY");
        when(rs.getTimestamp("rec_series_start")).thenReturn(Timestamp.valueOf(S));
        when(rs.getTimestamp("rec_series_end")).thenReturn(Timestamp.valueOf(S.plusYears(1)));
        when(rs.getInt("rec_interval")).thenReturn(0);
        stubRest(rs);

        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(users.findById("pid-mock")).thenReturn(Optional.of(P));

        Appointment a = new JdbcAppointmentRepository(ds, users).findById("rec-fb").orElseThrow();
        assertThat(a).isInstanceOf(RecurringAppointment.class);
    }

    private static void stubRest(ResultSet rs) throws SQLException {
        when(rs.getString("status")).thenReturn("CONFIRMED");
        when(rs.getInt("participant_count")).thenReturn(1);
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
    }

    @Test
    void user_save_postgres_invalidUuidId_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")))).thenReturn(ps);

        assertThatThrownBy(() -> new JdbcUserRepository(ds).save(new User("not-a-uuid", "N", "e@e.com", "p")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not-a-uuid");
    }

    @Test
    void clinic_save_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).save(new Clinic("c", "N", "a", "UTC")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save clinic");
    }

    @Test
    void doctor_save_executeUpdateFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException("dup"));

        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).save(new Doctor("d", "N", "e@e.com", "S", 1, "cl")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save doctor");
    }

    @Test
    void room_save_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).save(new Room("r", "R", "cl")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save room");
    }

    @Test
    void appointment_save_h2_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-h2", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save appointment");
    }

    @Test
    void appointment_save_h2_bindRequestFieldSetStringFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        doThrow(new SQLException("bind notes")).when(ps).setString(eq(25), any());

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-br", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save appointment");
    }

    @Test
    void appointment_save_postgres_setTimestampFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")))).thenReturn(ps);
        doThrow(new SQLException("ts")).when(ps).setTimestamp(eq(30), any(Timestamp.class));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-pg", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save appointment");
    }

    @Test
    void appointment_save_mysql_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-my", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save appointment");
    }

    @Test
    void appointment_save_postgres_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-pg2", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save appointment");
    }

    @Test
    void appointment_save_chainedSqlException_appendsNextMessage() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        SQLException root = new SQLException("root cause");
        root.setNextException(new SQLException("underlying"));
        doThrow(root).when(ps).executeUpdate();

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-chain", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("root cause")
                .hasMessageContaining("underlying")
                .hasMessageContaining(" — ");
    }

    @Test
    void appointment_save_nextExceptionWithoutMessage_doesNotAppendSeparator() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        SQLException root = new SQLException("visible");
        root.setNextException(new SQLException((String) null));
        doThrow(root).when(ps).executeUpdate();

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(new InPersonAppointment("e-next-null", P, SLOT, "L")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("visible")
                .hasMessageNotContaining(" — ");
    }
}
