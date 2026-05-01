package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.AuditEntry;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JDBC error-path coverage for {@link JdbcAuditEntryRepository} (append-only table; unqualified name).
 */
class JdbcAuditEntryRepositoryMockTest {

    @Test
    void append_null_skipsDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        new JdbcAuditEntryRepository(ds).append(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void findByUserId_null_usesUnfilteredListSql() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(JdbcAuditEntryRepositoryMockTest::isFindAllSql))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        assertThat(new JdbcAuditEntryRepository(ds).findByUserId(null)).isEmpty();
    }

    @Test
    void findRecent_maxZero_usesLimitOne() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LIMIT ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        assertThat(new JdbcAuditEntryRepository(ds).findRecent(0)).isEmpty();
        verify(ps).setInt(1, 1);
    }

    @Test
    void findRecent_positiveMax_passesThrough() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LIMIT ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        assertThat(new JdbcAuditEntryRepository(ds).findRecent(7)).isEmpty();
        verify(ps).setInt(1, 7);
    }

    @Test
    void findRecent_whenOneRow_mapsEntry() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LIMIT ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 3, 1, 12, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("u1");
        when(rs.getString("user_name")).thenReturn("n");
        when(rs.getString("action")).thenReturn("LOGIN");
        when(rs.getString("details")).thenReturn("d");
        when(rs.getString("entity_type")).thenReturn("USER");
        when(rs.getString("entity_id")).thenReturn("e1");
        when(rs.getString("old_value")).thenReturn(null);
        when(rs.getString("new_value")).thenReturn(null);

        assertThat(new JdbcAuditEntryRepository(ds).findRecent(10)).hasSize(1);
    }

    @Test
    void findRecent_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findRecent(2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("recent audit");
    }

    @Test
    void findAll_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list audit");
    }

    @Test
    void findByUserId_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByUserId("u1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find by user");
    }

    @Test
    void findByEntityType_getConnectionFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("pool down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByEntityType("E"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entity type");
    }

    @Test
    void findRecent_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenThrow(new SQLException("prep"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findRecent(3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("recent audit");
    }

    @Test
    void findRecent_negativeMax_coercesToAtLeastOne() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("LIMIT ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        assertThat(new JdbcAuditEntryRepository(ds).findRecent(-3)).isEmpty();
        verify(ps).setInt(1, 1);
    }

    @Test
    void findByEntityType_null_usesUnfilteredListSql() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(JdbcAuditEntryRepositoryMockTest::isFindAllSql))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        assertThat(new JdbcAuditEntryRepository(ds).findByEntityType(null)).isEmpty();
    }

    /** Matches {@code findAll()} SQL (delegated to when userId/entityType is null). */
    private static boolean isFindAllSql(String sql) {
        return sql != null
                && sql.contains("ORDER BY id DESC")
                && !sql.contains("WHERE")
                && !sql.contains("LIMIT");
    }

    @Test
    void append_connectionFailure_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("no connection"));
        AuditEntry entry = new AuditEntry(LocalDateTime.now(), "u", "n", "ACT", "d");
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).append(entry))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to append audit entry");
    }

    @Test
    void append_prepareStatementFails_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));
        AuditEntry entry = new AuditEntry(LocalDateTime.now(), "u", "n", "ACT", "d");
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).append(entry))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to append audit entry");
    }

    @Test
    void append_executeUpdateFails_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(new SQLException("insert failed"));
        AuditEntry entry = new AuditEntry(LocalDateTime.now(), "u", "n", "ACT", "d");
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).append(entry))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to append audit entry");
    }

    @Test
    void findRecent_executeFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("query down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findRecent(3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("recent audit");
    }

    @Test
    void findAll_executeFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("list down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list audit");
    }

    @Test
    void findAll_whenOneRow_mapsEntry() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY id DESC")
                && !sql.contains("WHERE")
                && !sql.contains("LIMIT")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 4, 1, 9, 30);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("u2");
        when(rs.getString("user_name")).thenReturn("n2");
        when(rs.getString("action")).thenReturn("SAVE");
        when(rs.getString("details")).thenReturn("x");
        when(rs.getString("entity_type")).thenReturn("APPT");
        when(rs.getString("entity_id")).thenReturn("a1");
        when(rs.getString("old_value")).thenReturn("o");
        when(rs.getString("new_value")).thenReturn("n");

        assertThat(new JdbcAuditEntryRepository(ds).findAll()).hasSize(1);
    }

    @Test
    void findAll_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenThrow(new SQLException("prep"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findAll())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("list audit");
    }

    @Test
    void findByUserId_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByUserId("u1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find by user");
    }

    @Test
    void findByUserId_executeFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("by user down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByUserId("u1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("find by user");
    }

    @Test
    void findByUserId_whenOneRow_mapsEntry() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE user_id = ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 5, 1, 8, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("uid");
        when(rs.getString("user_name")).thenReturn("un");
        when(rs.getString("action")).thenReturn("X");
        when(rs.getString("details")).thenReturn("d");
        when(rs.getString("entity_type")).thenReturn("E");
        when(rs.getString("entity_id")).thenReturn("e");
        when(rs.getString("old_value")).thenReturn(null);
        when(rs.getString("new_value")).thenReturn(null);

        assertThat(new JdbcAuditEntryRepository(ds).findByUserId("uid")).hasSize(1);
    }

    @Test
    void findByEntityType_prepareStatementFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("prep"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByEntityType("T"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entity type");
    }

    @Test
    void findByEntityType_executeFails_wraps() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("entity down"));
        assertThatThrownBy(() -> new JdbcAuditEntryRepository(ds).findByEntityType("T"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entity type");
    }

    @Test
    void findByEntityType_whenOneRow_mapsEntry() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE entity_type = ?")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        LocalDateTime ts = LocalDateTime.of(2026, 6, 1, 14, 0);
        when(rs.getObject("timestamp", LocalDateTime.class)).thenReturn(ts);
        when(rs.getString("user_id")).thenReturn("u");
        when(rs.getString("user_name")).thenReturn("n");
        when(rs.getString("action")).thenReturn("A");
        when(rs.getString("details")).thenReturn("d");
        when(rs.getString("entity_type")).thenReturn("BOOKING");
        when(rs.getString("entity_id")).thenReturn("b1");
        when(rs.getString("old_value")).thenReturn(null);
        when(rs.getString("new_value")).thenReturn(null);

        assertThat(new JdbcAuditEntryRepository(ds).findByEntityType("BOOKING")).hasSize(1);
    }
}
