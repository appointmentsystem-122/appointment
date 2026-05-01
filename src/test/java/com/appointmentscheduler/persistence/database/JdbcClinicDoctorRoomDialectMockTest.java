package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.Room;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers MySQL {@code ON DUPLICATE KEY} vs H2 {@code MERGE} branches in clinic/doctor/room saves.
 */
class JdbcClinicDoctorRoomDialectMockTest {

    @Test
    void clinic_save_mysqlUsesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcClinicRepository(ds).save(new Clinic("c1", "N", "A", "UTC"));
        verify(ps).executeUpdate();
    }

    @Test
    void clinic_save_h2UsesMerge() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcClinicRepository(ds).save(new Clinic("c2", "N", "A", "UTC"));
        verify(ps).executeUpdate();
    }

    @Test
    void doctor_save_mysqlUsesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcDoctorRepository(ds).save(new Doctor("d1", "Dr", "e@e.com", "S", 8, "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void doctor_save_h2UsesMerge() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcDoctorRepository(ds).save(new Doctor("d2", "Dr", "e@e.com", "S", 8, "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void room_save_mysqlUsesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcRoomRepository(ds).save(new Room("r1", "Room", "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void room_save_h2UsesMerge() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcRoomRepository(ds).save(new Room("r2", "Room", "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void clinic_save_postgres_mergeUsesQualifiedTable() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO") && sql.contains("appointment.clinic"))))
                .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcClinicRepository(ds).save(new Clinic("cpg", "PG Clinic", "addr", "UTC"));
        verify(ps).executeUpdate();
    }

    @Test
    void doctor_save_postgres_mergeUsesQualifiedTable() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO") && sql.contains("appointment.doctor"))))
                .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcDoctorRepository(ds).save(new Doctor("dpg", "Dr PG", "dpg@e.com", "Spec", 5, "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void room_save_postgres_mergeUsesQualifiedTable() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO") && sql.contains("appointment.room"))))
                .thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcRoomRepository(ds).save(new Room("rpg", "Room PG", "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void clinic_save_mariaDbUsesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcClinicRepository(ds).save(new Clinic("cmar", "Maria", "a", "UTC"));
        verify(ps).executeUpdate();
    }

    @Test
    void doctor_save_mariaDbUsesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcDoctorRepository(ds).save(new Doctor("dmar", "Dr M", "m@e.com", "S", 6, "cl"));
        verify(ps).executeUpdate();
    }

    @Test
    void room_save_mariaDbUsesOnDuplicateKey() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcRoomRepository(ds).save(new Room("rmar", "Room M", "cl"));
        verify(ps).executeUpdate();
    }
}
