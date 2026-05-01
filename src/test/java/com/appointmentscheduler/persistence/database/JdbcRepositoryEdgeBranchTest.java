package com.appointmentscheduler.persistence.database;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers early-return and empty-result JDBC branches (null entity/id, no row).
 */
class JdbcRepositoryEdgeBranchTest {

    @Test
    void room_save_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        new JdbcRoomRepository(ds).save(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void doctor_save_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        new JdbcDoctorRepository(ds).save(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void clinic_save_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        new JdbcClinicRepository(ds).save(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void user_save_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        new JdbcUserRepository(ds).save(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void room_findById_null_returnsEmpty() {
        assertThat(new JdbcRoomRepository(mock(DataSource.class)).findById(null)).isEmpty();
    }

    @Test
    void doctor_findById_null_returnsEmpty() {
        assertThat(new JdbcDoctorRepository(mock(DataSource.class)).findById(null)).isEmpty();
    }

    @Test
    void clinic_findById_null_returnsEmpty() {
        assertThat(new JdbcClinicRepository(mock(DataSource.class)).findById(null)).isEmpty();
    }

    @Test
    void user_findByEmail_null_returnsEmpty() {
        assertThat(new JdbcUserRepository(mock(DataSource.class)).findByEmail(null)).isEmpty();
    }

    @Test
    void room_findById_noRow_returnsEmpty() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("room") && sql.contains("WHERE id ="))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcRoomRepository(ds).findById("r1")).isEmpty();
    }

    @Test
    void doctor_findById_noRow_returnsEmpty() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("doctor") && sql.contains("WHERE id ="))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcDoctorRepository(ds).findById("d1")).isEmpty();
    }

    @Test
    void user_findByEmail_noRow_returnsEmpty() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LOWER(email)")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcUserRepository(ds).findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void room_findAll_emptyList() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcRoomRepository(ds).findAll()).isEmpty();
    }
}
