package com.appointmentscheduler.persistence.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcPostgresHelperTest {

    @Test
    void h2ReportsNeitherPostgresNorMySql_tableUnqualified() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:pg_helper_test;DB_CLOSE_DELAY=-1", "sa", "")) {
            assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
            assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
            assertThat(JdbcPostgresHelper.table(c, "appointment")).isEqualTo("appointment");
        }
    }

    @Test
    void postgresProduct_qualifiesTableName() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        assertThat(JdbcPostgresHelper.isPostgres(c)).isTrue();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
        assertThat(JdbcPostgresHelper.table(c, "app_user")).isEqualTo("appointment.app_user");
    }

    @Test
    void mysqlProductNameWithVersionSubstring_detected() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL Connector/J");
        assertThat(JdbcPostgresHelper.isMySql(c)).isTrue();
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
    }

    @Test
    void mysqlAndMariaDb_detected() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        assertThat(JdbcPostgresHelper.isMySql(c)).isTrue();
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        assertThat(JdbcPostgresHelper.isMySql(c)).isTrue();
    }

    @Test
    void oracleProduct_neitherPostgresNorMySql_tableUnqualified() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("Oracle Database");
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
        assertThat(JdbcPostgresHelper.table(c, "app_user")).isEqualTo("app_user");
    }

    @Test
    void emptyProductName_neitherPostgresNorMySql() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("");
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
        assertThat(JdbcPostgresHelper.table(c, "x")).isEqualTo("x");
    }

    @Test
    void postgresSubstring_caseInsensitive() throws Exception {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("EnterpriseDB (PostgreSQL compatible)");
        assertThat(JdbcPostgresHelper.isPostgres(c)).isTrue();
        assertThat(JdbcPostgresHelper.table(c, "t")).isEqualTo("appointment.t");
    }

    @Test
    void nullMetaData_orNullProductName_safe() throws Exception {
        Connection c = mock(Connection.class);
        when(c.getMetaData()).thenReturn(null);
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();

        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn(null);
        assertThat(JdbcPostgresHelper.isPostgres(c)).isFalse();
        assertThat(JdbcPostgresHelper.isMySql(c)).isFalse();
    }

    @Test
    void getMetaDataThrows_propagatesFromDetectionMethods() throws Exception {
        Connection c = mock(Connection.class);
        when(c.getMetaData()).thenThrow(new SQLException("meta down"));
        assertThatThrownBy(() -> JdbcPostgresHelper.isPostgres(c)).isInstanceOf(SQLException.class).hasMessageContaining("meta down");
        assertThatThrownBy(() -> JdbcPostgresHelper.isMySql(c)).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> JdbcPostgresHelper.table(c, "t")).isInstanceOf(SQLException.class);
    }
}
