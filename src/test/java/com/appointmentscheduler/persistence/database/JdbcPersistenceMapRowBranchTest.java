package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link JdbcUserRepository} / {@link JdbcClinicRepository} row-mapping branches (user_type switch, null time_zone).
 */
class JdbcPersistenceMapRowBranchTest {

    @Test
    void user_findById_mapsAdministrator() throws Exception {
        assertUserTypeMapped("ADMINISTRATOR", Administrator.class);
    }

    @Test
    void user_findById_mapsDoctor() throws Exception {
        assertUserTypeMapped("DOCTOR", DoctorUser.class);
    }

    @Test
    void user_findById_mapsReceptionist() throws Exception {
        assertUserTypeMapped("RECEPTIONIST", ReceptionistUser.class);
    }

    @Test
    void user_findById_mapsPatientDefault() throws Exception {
        assertUserTypeMapped("PATIENT", User.class);
    }

    @Test
    void user_findById_nullUserType_defaultsToPatient() throws Exception {
        assertUserTypeMapped(null, User.class);
    }

    @Test
    void clinic_findById_nullTimeZone_defaultsUtc() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = postgresLikeMeta("H2");
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("clinic") && sql.contains("WHERE id ="))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("c1");
        when(rs.getString("name")).thenReturn("Clinic");
        when(rs.getString("address")).thenReturn("Addr");
        when(rs.getString("time_zone")).thenReturn(null);

        Clinic clinic = new JdbcClinicRepository(ds).findById("c1").orElseThrow();
        assertThat(clinic.getTimeZone()).isEqualTo("UTC");
    }

    private static Connection postgresLikeMeta(String product) throws SQLException {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn(product);
        return c;
    }

    private static void assertUserTypeMapped(String userType, Class<?> expected) throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection c = postgresLikeMeta("H2");
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("app_user") && sql.contains("WHERE id ="))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("id-1");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn(userType);

        var found = new JdbcUserRepository(ds).findById("id-1");
        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(expected);
    }
}
