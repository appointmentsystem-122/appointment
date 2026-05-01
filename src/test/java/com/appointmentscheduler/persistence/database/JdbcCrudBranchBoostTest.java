package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcCrudBranchBoostTest {

    @Test
    void room_findById_and_findAll_successPaths() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement byId = mock(PreparedStatement.class);
        PreparedStatement all = mock(PreparedStatement.class);
        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(byId);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(all);
        when(byId.executeQuery()).thenReturn(rs1);
        when(all.executeQuery()).thenReturn(rs2);

        when(rs1.next()).thenReturn(true);
        when(rs1.getString("id")).thenReturn("r1");
        when(rs1.getString("name")).thenReturn("Room A");
        when(rs1.getString("clinic_id")).thenReturn("c1");

        when(rs2.next()).thenReturn(true, false);
        when(rs2.getString("id")).thenReturn("r2");
        when(rs2.getString("name")).thenReturn("Room B");
        when(rs2.getString("clinic_id")).thenReturn("c2");

        JdbcRoomRepository repo = new JdbcRoomRepository(ds);
        assertThat(repo.findById("r1")).isPresent().get().extracting(Room::getName).isEqualTo("Room A");
        assertThat(repo.findAll()).hasSize(1);
    }

    @Test
    void doctor_findById_and_findAll_successPaths() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement byId = mock(PreparedStatement.class);
        PreparedStatement all = mock(PreparedStatement.class);
        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(byId);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(all);
        when(byId.executeQuery()).thenReturn(rs1);
        when(all.executeQuery()).thenReturn(rs2);

        when(rs1.next()).thenReturn(true);
        when(rs1.getString("id")).thenReturn("d1");
        when(rs1.getString("name")).thenReturn("Dr A");
        when(rs1.getString("email")).thenReturn("d@x.com");
        when(rs1.getString("specialty")).thenReturn("S");
        when(rs1.getInt("max_appointments_per_day")).thenReturn(7);
        when(rs1.getString("clinic_id")).thenReturn("c1");

        when(rs2.next()).thenReturn(true, false);
        when(rs2.getString("id")).thenReturn("d2");
        when(rs2.getString("name")).thenReturn("Dr B");
        when(rs2.getString("email")).thenReturn("d2@x.com");
        when(rs2.getString("specialty")).thenReturn("SP");
        when(rs2.getInt("max_appointments_per_day")).thenReturn(9);
        when(rs2.getString("clinic_id")).thenReturn("c2");

        JdbcDoctorRepository repo = new JdbcDoctorRepository(ds);
        assertThat(repo.findById("d1")).isPresent().get().extracting(Doctor::getName).isEqualTo("Dr A");
        assertThat(repo.findAll()).hasSize(1);
    }

    @Test
    void clinic_findById_and_findAll_successPaths() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement byId = mock(PreparedStatement.class);
        PreparedStatement all = mock(PreparedStatement.class);
        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(byId);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(all);
        when(byId.executeQuery()).thenReturn(rs1);
        when(all.executeQuery()).thenReturn(rs2);

        when(rs1.next()).thenReturn(true);
        when(rs1.getString("id")).thenReturn("c1");
        when(rs1.getString("name")).thenReturn("Main");
        when(rs1.getString("address")).thenReturn("Addr");
        when(rs1.getString("time_zone")).thenReturn("UTC");

        when(rs2.next()).thenReturn(true, false);
        when(rs2.getString("id")).thenReturn("c2");
        when(rs2.getString("name")).thenReturn("North");
        when(rs2.getString("address")).thenReturn("Addr2");
        when(rs2.getString("time_zone")).thenReturn("UTC");

        JdbcClinicRepository repo = new JdbcClinicRepository(ds);
        assertThat(repo.findById("c1")).isPresent().get().extracting(Clinic::getName).isEqualTo("Main");
        assertThat(repo.findAll()).hasSize(1);
    }

    @Test
    void clinic_findById_nullTimeZone_defaultsUtc() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement byId = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.argThat(sql -> sql != null && sql.contains("WHERE id ="))))
                .thenReturn(byId);
        when(byId.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("c-tz");
        when(rs.getString("name")).thenReturn("Clinic TZ");
        when(rs.getString("address")).thenReturn("A");
        when(rs.getString("time_zone")).thenReturn(null);

        assertThat(new JdbcClinicRepository(ds).findById("c-tz")).isPresent().get()
                .extracting(Clinic::getTimeZone).isEqualTo("UTC");
    }

    @Test
    void user_findAll_and_findByEmail_successPaths() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement all = mock(PreparedStatement.class);
        PreparedStatement byEmail = mock(PreparedStatement.class);
        ResultSet rsa = mock(ResultSet.class);
        ResultSet rse = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(all);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LOWER(email)")))).thenReturn(byEmail);
        when(all.executeQuery()).thenReturn(rsa);
        when(byEmail.executeQuery()).thenReturn(rse);

        when(rsa.next()).thenReturn(true, false);
        when(rsa.getString("id")).thenReturn("u1");
        when(rsa.getString("name")).thenReturn("User");
        when(rsa.getString("email")).thenReturn("u@x.com");
        when(rsa.getString("password_hash")).thenReturn("h");
        when(rsa.getString("user_type")).thenReturn("PATIENT");

        when(rse.next()).thenReturn(true);
        when(rse.getString("id")).thenReturn("u2");
        when(rse.getString("name")).thenReturn("Admin");
        when(rse.getString("email")).thenReturn("a@x.com");
        when(rse.getString("password_hash")).thenReturn("h");
        when(rse.getString("user_type")).thenReturn("ADMINISTRATOR");

        JdbcUserRepository repo = new JdbcUserRepository(ds);
        assertThat(repo.findAll()).hasSize(1);
        Optional<User> byEmailFound = repo.findByEmail("a@x.com");
        assertThat(byEmailFound).isPresent();
        assertThat(byEmailFound.get().getClass().getSimpleName()).isEqualTo("Administrator");
    }

    @Test
    void repositories_wrapSqlExceptions() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> new JdbcRoomRepository(ds).findAll()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).findAll()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new JdbcClinicRepository(ds).findAll()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new JdbcUserRepository(ds).findAll()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void clinic_doctor_room_save_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("no pool"));

        assertThatThrownBy(() -> new JdbcClinicRepository(ds).save(new Clinic("c", "N", "a", "UTC")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save clinic");
        assertThatThrownBy(() -> new JdbcDoctorRepository(ds).save(new Doctor("d", "N", "e@e.com", "S", 1, "cl")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save doctor");
        assertThatThrownBy(() -> new JdbcRoomRepository(ds).save(new Room("r", "R", "cl")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save room");
    }

    private static Connection h2Connection() throws SQLException {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        return c;
    }
}

