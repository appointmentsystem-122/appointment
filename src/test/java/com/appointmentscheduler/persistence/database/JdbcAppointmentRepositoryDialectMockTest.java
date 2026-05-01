package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcAppointmentRepositoryDialectMockTest {

    @Test
    void save_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        new JdbcAppointmentRepository(ds, users).save(null);
        verify(ds, never()).getConnection();
    }

    private static InPersonAppointment sampleAppointment() {
        User p = new User("pid-mock", "N", "e@e.com", "pwd");
        LocalDateTime s = LocalDateTime.of(2026, 6, 1, 10, 0);
        return new InPersonAppointment("aid-mock", p, new TimeSlot(s, s.plusHours(1)), "Loc");
    }

    @Test
    void save_whenPostgres_usesOnConflictUpsert() throws Exception {
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

        new JdbcAppointmentRepository(ds, users).save(sampleAppointment());
        verify(ps).executeUpdate();
    }

    @Test
    void save_whenMysql_usesOnDuplicateKeyUpsert() throws Exception {
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

        new JdbcAppointmentRepository(ds, users).save(sampleAppointment());
        verify(ps).executeUpdate();
        verify(ps, never()).setTimestamp(anyInt(), any(Timestamp.class));
    }

    @Test
    void save_whenMariaDb_usesOnDuplicateKeyLikeMysql() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ON DUPLICATE KEY")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(sampleAppointment());
        verify(ps).executeUpdate();
        verify(ps, never()).setTimestamp(anyInt(), any(Timestamp.class));
    }

    @Test
    void save_whenH2_usesMergeAndTimestamp() throws Exception {
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

        new JdbcAppointmentRepository(ds, users).save(sampleAppointment());
        verify(ps).setTimestamp(anyInt(), any(Timestamp.class));
        verify(ps).executeUpdate();
    }

    @Test
    void save_whenSqlExceptionChains_nextMessageIsAppended() throws Exception {
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
        root.setNextException(new SQLException("chained"));
        when(ps.executeUpdate()).thenThrow(root);

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(sampleAppointment()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aid-mock")
                .hasMessageContaining("root")
                .hasMessageContaining("chained");
    }

    @Test
    void save_whenSqlExceptionChains_nextWithNullMessage_skipsChainedSuffix() throws Exception {
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
        root.setNextException(new SQLException((String) null));
        when(ps.executeUpdate()).thenThrow(root);

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(sampleAppointment()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aid-mock")
                .hasMessageContaining("root")
                .hasMessageNotContaining(" — ");
    }

    @Test
    void save_whenSqlExceptionRootMessageNull_nextMessageStillAppended() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        SQLException root = new SQLException((String) null);
        root.setNextException(new SQLException("chained-detail"));
        when(ps.executeUpdate()).thenThrow(root);

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(sampleAppointment()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aid-mock")
                .hasMessageContaining("SQLException")
                .hasMessageContaining("chained-detail")
                .hasMessageContaining(" — ");
    }

    @Test
    void save_whenSqlExceptionHasNoMessage_usesExceptionSimpleName() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException());

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(sampleAppointment()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aid-mock")
                .hasMessageContaining("SQLException");
    }

    @Test
    void findById_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));
        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findById("a1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find appointment");
    }

    @Test
    void deleteById_whenExecuteFails_wraps() throws Exception {
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
        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).deleteById("x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("delete appointment");
    }

    @Test
    void save_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool down"));
        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(sampleAppointment()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aid-mock");
    }

    @Test
    void findAll_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));
        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list appointments");
    }

    @Test
    void findBlockingBookings_whenGetConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));
        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient("pid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("blocking appointments");
    }

    @Test
    void findById_whenPrepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));
        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findById("a1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find appointment");
    }

    @Test
    void deleteById_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        new JdbcAppointmentRepository(ds, users).deleteById(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void save_whenSqlExceptionChainedHasNoMessage_doesNotAppendBlankChain() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        SQLException root = new SQLException("root-msg");
        root.setNextException(new SQLException());
        when(ps.executeUpdate()).thenThrow(root);

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).save(sampleAppointment()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("root-msg")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("—"));
    }
}
