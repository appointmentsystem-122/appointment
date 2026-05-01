package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Additional JDBC branch coverage for repositories (error paths, edge types) to raise JaCoCo branch %.
 */
class PersistenceJdbcBranchSweepTest {

    private static final LocalDateTime S = LocalDateTime.of(2026, 8, 1, 9, 0);
    private static final TimeSlot SLOT = new TimeSlot(S, S.plusHours(1));
    private static final User PAT = new User("p1", "P", "p@e.com", "x");

    /** Concrete {@link Appointment} that does not match any specialized subtype in {@code appointmentType}. */
    private static final class UnclassifiedAppointment extends Appointment {
        UnclassifiedAppointment(String id, User patient, TimeSlot slot) {
            super(id, patient, slot);
        }
    }

    @Test
    void jdbcAppointment_save_unclassifiedSubtype_usesMergeAndIndividualTypeColumn() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        Appointment a = new UnclassifiedAppointment("ua-1", PAT, SLOT);
        new JdbcAppointmentRepository(ds, users).save(a);
        org.mockito.Mockito.verify(ps).setString(14, "INDIVIDUAL");
    }

    @Test
    void jdbcAppointment_save_setTypeSpecificParamsThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        doThrow(new SQLException("bind")).when(ps).setString(eq(15), anyString());

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(
                new InPersonAppointment("x1", PAT, SLOT, "Loc")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("type-specific");
    }

    @Test
    void jdbcAppointment_findAll_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY start_time"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list appointments");
    }

    @Test
    void jdbcAppointment_findAll_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY start_time"))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list appointments");
    }

    @Test
    void jdbcAppointment_findBlocking_executeQueryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("patient_id = ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("q"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient("pid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("blocking");
    }

    @Test
    void jdbcAppointment_findById_executeQueryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("q"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findById("id1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find appointment");
    }

    @Test
    void jdbcAppointment_findById_recurringInvalidFrequency_propagates() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("rid");
        when(rs.getString("patient_id")).thenReturn("pid");
        when(users.findById("pid")).thenReturn(Optional.of(PAT));
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(S);
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(S.plusHours(1));
        when(rs.getString("appointment_type")).thenReturn("RECURRING");
        when(rs.getString("rec_frequency")).thenReturn("NOT_A_VALID_ENUM");
        when(rs.getTimestamp("rec_series_start")).thenReturn(Timestamp.valueOf(S.minusDays(1)));
        when(rs.getTimestamp("rec_series_end")).thenReturn(Timestamp.valueOf(S.plusYears(1)));
        when(rs.getInt("rec_interval")).thenReturn(1);
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findById("rid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jdbcAppointment_findById_patientNotFound_throwsFromMapRow() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("a1");
        when(rs.getString("patient_id")).thenReturn("missing");
        when(users.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findById("a1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void jdbcUser_findById_executeQueryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("q"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findById("id1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user");
    }

    @Test
    void jdbcUser_findById_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("next"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findById("id1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user");
    }

    @Test
    void jdbcUser_findByEmail_executeQueryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("q"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findByEmail("a@a.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user by email");
    }

    @Test
    void jdbcUser_findAll_executeQueryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list users");
    }

    @Test
    void jdbcAudit_append_executeUpdateFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException("insert"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).append(
                new com.appointmentscheduler.domain.AuditEntry(
                        LocalDateTime.now(), "u", "n", "ACT", "d")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("append audit");
    }

    @Test
    void jdbcAudit_append_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).append(
                new com.appointmentscheduler.domain.AuditEntry(
                        LocalDateTime.now(), "u", "n", "ACT", "d")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("append audit");
    }

    @Test
    void jdbcAudit_findByEntityType_queryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("q"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByEntityType("T"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entity type");
    }

    @Test
    void jdbcAudit_findByUserId_prepareFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByUserId("u1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find by user");
    }

    @Test
    void jdbcAudit_findAll_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list audit");
    }

    @Test
    void jdbcAudit_findRecent_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findRecent(4))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("recent audit");
    }

    @Test
    void jdbcAppointment_deleteById_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).deleteById("d1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("delete appointment");
    }

    @Test
    void jdbcAppointment_deleteById_executeUpdateFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException("del"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).deleteById("d2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("delete appointment");
    }

    @Test
    void jdbcAppointment_findBlocking_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("patient_id = ?"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient("pid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("blocking");
    }

    @Test
    void jdbcClinic_findById_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findById("c1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find clinic");
    }

    @Test
    void jdbcClinic_findById_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findById("c1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find clinic");
    }

    @Test
    void jdbcDoctor_findById_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).findById("d1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find doctor");
    }

    @Test
    void jdbcDoctor_findById_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).findById("d1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find doctor");
    }

    @Test
    void jdbcRoom_findById_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).findById("r1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find room");
    }

    @Test
    void jdbcRoom_findById_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).findById("r1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find room");
    }

    @Test
    void jdbcRoom_findAll_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list rooms");
    }

    @Test
    void jdbcDoctor_findAll_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list doctors");
    }

    @Test
    void jdbcClinic_findAll_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list clinics");
    }

    @Test
    void jdbcUser_findAll_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list users");
    }

    @Test
    void jdbcUser_save_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).save(new User("u1", "N", "e@e.com", "p")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save user");
    }

    @Test
    void jdbcUser_findByEmail_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findByEmail("a@a.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user by email");
    }

    @Test
    void jdbcUser_findByEmail_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).findByEmail("a@a.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user by email");
    }

    @Test
    void jdbcClinic_findAll_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list clinics");
    }

    @Test
    void jdbcDoctor_findAll_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list doctors");
    }

    @Test
    void jdbcRoom_findAll_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list rooms");
    }

    @Test
    void jdbcClinic_findById_executeQueryFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("q"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findById("c1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find clinic");
    }

    @Test
    void jdbcAudit_findByUserId_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByUserId("u1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find by user");
    }

    @Test
    void jdbcAudit_findByEntityType_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByEntityType("T"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entity type");
    }

    @Test
    void jdbcAudit_findAll_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list audit");
    }

    @Test
    void jdbcAppointment_findBlockingBookings_rsNextThrows_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("patient_id = ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenThrow(new SQLException("iter"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient("pid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("blocking");
    }

    @Test
    void jdbcClinic_findAll_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list clinics");
    }

    @Test
    void jdbcDoctor_findAll_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list doctors");
    }

    @Test
    void jdbcRoom_findAll_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list rooms");
    }
}
