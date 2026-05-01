package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers PostgreSQL / MySQL JDBC branches in {@link JdbcUserRepository} using mocked connections
 * (real H2 uses unqualified tables; Postgres code targets {@code appointment.app_user}).
 */
class JdbcUserRepositoryDialectMockTest {

    @Test
    void save_null_user_noOp() throws Exception {
        DataSource ds = mock(DataSource.class);
        new JdbcUserRepository(ds).save(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void save_getConnectionFails_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool"));
        assertThatThrownBy(() -> new JdbcUserRepository(ds).save(new User("u1", "N", "e@e.com", "p")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save user");
    }

    @Test
    void findById_null_returnsEmpty() {
        assertThat(new JdbcUserRepository(mock(DataSource.class)).findById(null)).isEmpty();
    }

    @Test
    void findByEmail_null_returnsEmpty() {
        assertThat(new JdbcUserRepository(mock(DataSource.class)).findByEmail(null)).isEmpty();
    }

    @Test
    void save_whenMariaDb_usesOnDuplicateKeyLikeMysql() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new User("mid-1", "n", "e@e.com", "pwd"));
        verify(ps).setString(1, "mid-1");
        verify(ps).executeUpdate();
    }

    @Test
    void findById_whenPostgresAndMalformedUuidLength36_usesStringBinding() throws Exception {
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

        String malformed36 = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz";
        assertThat(new JdbcUserRepository(ds).findById(malformed36)).isEmpty();
        verify(ps).setString(1, malformed36);
    }

    @Test
    void findByEmail_whenUserTypeNull_defaultsToPatientUser() throws Exception {
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
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("u1");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn(null);

        Optional<User> out = new JdbcUserRepository(ds).findByEmail("e@e.com");
        assertThat(out).isPresent();
        assertThat(out.get()).isInstanceOf(User.class);
    }

    @Test
    void findAll_whenPostgres_usesQualifiedTableName() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("appointment.app_user")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcUserRepository(ds).findAll()).isEmpty();
    }

    @Test
    void getAllUsers_delegatesToFindAll() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false);
        when(rs.getString("id")).thenReturn("u-row");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn("PATIENT");

        JdbcUserRepository repo = new JdbcUserRepository(ds);
        List<User> a = repo.getAllUsers();
        List<User> b = repo.findAll();
        assertThat(a).hasSize(1);
        assertThat(b).hasSize(1);
        assertThat(a.get(0).getId()).isEqualTo(b.get(0).getId());
    }

    @Test
    void findAll_whenOneRow_returnsUser() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY name")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("id")).thenReturn("u-row");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn("PATIENT");

        assertThat(new JdbcUserRepository(ds).findAll()).hasSize(1);
    }

    @Test
    void findByEmail_whenRowSaysAdministrator_mapsAdministrator() throws Exception {
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
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("adm1");
        when(rs.getString("name")).thenReturn("Admin");
        when(rs.getString("email")).thenReturn("a@a.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn("ADMINISTRATOR");

        Optional<User> out = new JdbcUserRepository(ds).findByEmail("a@a.com");
        assertThat(out).isPresent();
        assertThat(out.get()).isInstanceOf(Administrator.class);
    }

    @Test
    void findByEmail_whenRowSaysDoctor_mapsDoctorUser() throws Exception {
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
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("d1");
        when(rs.getString("name")).thenReturn("Dr");
        when(rs.getString("email")).thenReturn("d@d.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn("DOCTOR");

        Optional<User> out = new JdbcUserRepository(ds).findByEmail("d@d.com");
        assertThat(out).isPresent();
        assertThat(out.get()).isInstanceOf(DoctorUser.class);
    }

    @Test
    void findByEmail_whenRowSaysReceptionist_mapsReceptionistUser() throws Exception {
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
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("r1");
        when(rs.getString("name")).thenReturn("Rec");
        when(rs.getString("email")).thenReturn("r@r.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn("RECEPTIONIST");

        Optional<User> out = new JdbcUserRepository(ds).findByEmail("r@r.com");
        assertThat(out).isPresent();
        assertThat(out.get()).isInstanceOf(ReceptionistUser.class);
    }

    @Test
    void findById_whenMetaDataNull_usesUnqualifiedTable() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(null);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM app_user WHERE")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcUserRepository(ds).findById("any-id")).isEmpty();
    }

    @Test
    void save_whenPostgres_usesOnConflictAndUuidBinding() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON CONFLICT")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        String id = UUID.randomUUID().toString();
        new JdbcUserRepository(ds).save(new User(id, "n", "e@e.com", "pwd"));

        verify(ps).setObject(eq(1), eq(UUID.fromString(id)), eq(Types.OTHER));
        verify(ps).executeUpdate();
    }

    @Test
    void save_whenMysql_usesOnDuplicateKeyAndStringId() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new User("plain-id-1", "n", "e@e.com", "pwd"));

        verify(ps).setString(1, "plain-id-1");
        verify(ps).executeUpdate();
    }

    @Test
    void save_whenH2_usesMergeAndStringId() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new User("h2-id", "n", "e@e.com", "pwd"));

        verify(ps).setString(1, "h2-id");
        verify(ps).setString(5, "PATIENT");
        verify(ps).executeUpdate();
    }

    @Test
    void save_administrator_setsUserTypeColumn() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new Administrator("a1", "Admin", "a@a.com", "pw"));

        verify(ps).setString(5, "ADMINISTRATOR");
    }

    @Test
    void save_doctorUser_setsUserTypeColumn() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new DoctorUser("d1", "Dr", "d@d.com", "pw"));

        verify(ps).setString(5, "DOCTOR");
    }

    @Test
    void save_receptionist_setsUserTypeColumn() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new ReceptionistUser("r1", "Rec", "r@r.com", "pw"));

        verify(ps).setString(5, "RECEPTIONIST");
    }

    @Test
    void findById_whenPostgresAndValidUuid_bindsUuid() throws Exception {
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

        String id = UUID.randomUUID().toString();
        assertThat(new JdbcUserRepository(ds).findById(id)).isEmpty();
        verify(ps).setObject(eq(1), eq(UUID.fromString(id)), eq(Types.OTHER));
    }

    @Test
    void findById_whenPostgresAndInvalidUuid_fallsBackToStringBinding() throws Exception {
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

        assertThat(new JdbcUserRepository(ds).findById("not-a-uuid")).isEmpty();
        verify(ps).setString(1, "not-a-uuid");
    }

    @Test
    void findById_whenPostgresValidUuid_rowFound_returnsMappedUser() throws Exception {
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
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenReturn("u1");
        when(rs.getString("name")).thenReturn("N");
        when(rs.getString("email")).thenReturn("e@e.com");
        when(rs.getString("password_hash")).thenReturn("pw");
        when(rs.getString("user_type")).thenReturn("PATIENT");

        String id = UUID.randomUUID().toString();
        Optional<User> out = new JdbcUserRepository(ds).findById(id);
        assertThat(out).isPresent();
        assertThat(out.get().getEmail()).isEqualTo("e@e.com");
        verify(ps).setObject(eq(1), eq(UUID.fromString(id)), eq(Types.OTHER));
    }

    @Test
    void save_whenPrepareStatementFails_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO"))))
                .thenThrow(new SQLException("prep"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).save(new User("id1", "n", "e@e.com", "p")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("id1")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void save_whenExecuteUpdateFails_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException("boom"));

        assertThatThrownBy(() -> new JdbcUserRepository(ds).save(new User("id1", "n", "e@e.com", "p")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("id1")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void findByEmail_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));
        assertThatThrownBy(() -> new JdbcUserRepository(ds).findByEmail("a@a.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user by email");
    }

    @Test
    void findAll_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));
        assertThatThrownBy(() -> new JdbcUserRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list users");
    }

    @Test
    void findById_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool"));
        assertThatThrownBy(() -> new JdbcUserRepository(ds).findById("id-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user");
    }

    @Test
    void findById_whenPrepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));
        assertThatThrownBy(() -> new JdbcUserRepository(ds).findById("id-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find user");
    }

    @Test
    void save_whenMetaDataReturnsNullProductName_usesMergePath() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn(null);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new User("merge-id", "n", "e@e.com", "p"));
        verify(ps).setString(1, "merge-id");
    }

    @Test
    void save_whenMetaDataObjectIsNull_usesMergePath() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(null);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcUserRepository(ds).save(new User("merge-id-2", "n", "e2@e.com", "p"));
        verify(ps).setString(1, "merge-id-2");
    }

    @Test
    void findById_whenDatabaseProductNameNull_usesUnqualifiedTable() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn(null);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("FROM app_user WHERE")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThat(new JdbcUserRepository(ds).findById("id-1")).isEmpty();
    }

    @Test
    void isValidUUID_null_returnsFalse() throws Exception {
        Method m = JdbcUserRepository.class.getDeclaredMethod("isValidUUID", String.class);
        m.setAccessible(true);
        assertThat((Boolean) m.invoke(null, new Object[]{null})).isFalse();
    }
}
