package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.application.AppConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers {@link DatabaseConfig#getDataSource()} URL branches (MySQL vs PostgreSQL bootstrap) by mocking
 * {@link HikariDataSource} construction so no real MySQL/PostgreSQL server is required.
 */
class DatabaseConfigGetDataSourceMockTest {

    @BeforeEach
    @AfterEach
    void clearPool() throws Exception {
        DatabaseConfig.shutdown();
        Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void getDataSource_jdbcUrlContainsMysql_runsEnsureMySqlSchema() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = org.mockito.Mockito.mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            Optional<DataSource> ds = DatabaseConfig.getDataSource();
            assertThat(ds).isPresent();
            verify(statement, atLeast(6)).execute(anyString());
        }
    }

    @Test
    void getDataSource_jdbcUrlContainsPostgresql_runsEnsurePostgresSchema() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = org.mockito.Mockito.mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:postgresql://localhost:5432/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            Optional<DataSource> ds = DatabaseConfig.getDataSource();
            assertThat(ds).isPresent();
            verify(statement, atLeast(2)).execute(anyString());
        }
    }

    @Test
    void getDataSource_jdbcUrlPostgresql_caseInsensitive_runsEnsurePostgresSchema() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("JDBC:POSTGRESQL://DB:5432/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(statement, atLeast(2)).execute(anyString());
        }
    }

    @Test
    void getDataSource_mysql_alterTableMissingTableErrorCode1146_isIgnored() throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, String.class);
            if (sql != null && sql.startsWith("ALTER TABLE")) {
                throw new SQLException("Table 'appointment' doesn't exist", "", 1146);
            }
            return true;
        });

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(statement, atLeast(6)).execute(anyString());
        }
    }

    @Test
    void getDataSource_mysql_alterTableDuplicateColumnMessageWithoutDupErrorCode_isIgnored() throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        // Error code 0: relies on mySqlColumnOrTableAlreadyOk message branch ("Duplicate column" substring).
        when(statement.execute(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, String.class);
            if (sql != null && sql.startsWith("ALTER TABLE")) {
                throw new SQLException("Duplicate column name 'customer_notes'", "", 0);
            }
            return true;
        });

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(statement, atLeast(6)).execute(anyString());
        }
    }

    @Test
    void getDataSource_mysql_alterTableDuplicateColumnErrorCode1060_isIgnored() throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, String.class);
            if (sql != null && sql.startsWith("ALTER TABLE")) {
                throw new SQLException("Duplicate column", "", 1060);
            }
            return true;
        });

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(statement, atLeast(6)).execute(anyString());
        }
    }

    @Test
    void getDataSource_mysql_alterTableUnhandledSqlError_wrapsRuntimeException() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, String.class);
            if (sql != null && sql.startsWith("ALTER TABLE")) {
                throw new SQLException("fatal", "", 9999);
            }
            return true;
        });

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThatThrownBy(DatabaseConfig::getDataSource)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("MySQL schema setup failed");
        }
    }

    @Test
    void getDataSource_mysql_getConnectionFails_wrapsRuntimeException() throws Exception {
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenThrow(new SQLException("no route")));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThatThrownBy(DatabaseConfig::getDataSource)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("MySQL schema setup failed");
        }
    }

    @Test
    void getDataSource_mysql_createAppUserTableFails_wrapsRuntimeException() throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, String.class);
            if (sql != null && sql.contains("CREATE TABLE IF NOT EXISTS app_user")) {
                throw new SQLException("denied");
            }
            return true;
        });

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThatThrownBy(DatabaseConfig::getDataSource)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("MySQL schema setup failed");
        }
    }

    @Test
    void getDataSource_postgres_executeFails_wrapsRuntimeException() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException("role does not exist"));

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:postgresql://localhost:5432/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThatThrownBy(DatabaseConfig::getDataSource)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PostgreSQL schema setup failed");
        }
    }

    @Test
    void getDataSource_plainH2UrlWithoutMysqlOrPostgresql_runsFlywayMigrations() throws Exception {
        Connection connection = mock(Connection.class);
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class);
             MockedStatic<Flyway> fly = mockStatic(Flyway.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:h2:mem:cfg_flyway_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
            app.when(AppConfig::getDatabaseUsername).thenReturn("sa");
            app.when(AppConfig::getDatabasePassword).thenReturn("");

            FluentConfiguration cfg = mock(FluentConfiguration.class);
            Flyway fw = mock(Flyway.class);
            MigrateResult mr = mock(MigrateResult.class);
            when(cfg.dataSource(any(DataSource.class))).thenReturn(cfg);
            when(cfg.locations("classpath:com/appointmentscheduler/persistence/database/migration")).thenReturn(cfg);
            when(cfg.baselineOnMigrate(true)).thenReturn(cfg);
            when(cfg.load()).thenReturn(fw);
            when(fw.migrate()).thenReturn(mr);

            fly.when(() -> Flyway.configure(any(ClassLoader.class))).thenReturn(cfg);

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(fw).migrate();
            verify(cfg, never()).baselineVersion(anyString());
        }
    }

    /**
     * {@code jdbc:mariadb} does not contain {@code mysql}; initialization uses the Flyway branch like H2,
     * not {@link DatabaseConfig}'s MySQL schema helper.
     */
    @Test
    void getDataSource_mariadbUrl_runsFlywayNotMySqlBootstrap() throws Exception {
        Connection connection = mock(Connection.class);
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class);
             MockedStatic<Flyway> fly = mockStatic(Flyway.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mariadb://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            FluentConfiguration cfg = mock(FluentConfiguration.class);
            Flyway fw = mock(Flyway.class);
            MigrateResult mr = mock(MigrateResult.class);
            when(cfg.dataSource(any(DataSource.class))).thenReturn(cfg);
            when(cfg.locations("classpath:com/appointmentscheduler/persistence/database/migration")).thenReturn(cfg);
            when(cfg.baselineOnMigrate(true)).thenReturn(cfg);
            when(cfg.load()).thenReturn(fw);
            when(fw.migrate()).thenReturn(mr);

            fly.when(() -> Flyway.configure(any(ClassLoader.class))).thenReturn(cfg);

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(fw).migrate();
            verify(cfg, never()).baselineVersion(anyString());
        }
    }

    @Test
    void getDataSource_secondCall_reusesInitializedPool() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            DataSource a = DatabaseConfig.getDataSource().orElseThrow();
            DataSource b = DatabaseConfig.getDataSource().orElseThrow();
            assertThat(a).isSameAs(b);
        }
    }

    @Test
    void getDataSource_databaseDisabled_returnsEmpty() {
        try (MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(false);
            assertThat(DatabaseConfig.getDataSource()).isEmpty();
        }
    }

    @Test
    void getDataSource_databaseDisabled_returnsEmptyEvenIfPoolInjected() throws Exception {
        HikariDataSource injected = mock(HikariDataSource.class);
        Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(null, injected);
        try (MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(false);
            assertThat(DatabaseConfig.getDataSource()).isEmpty();
        }
        verifyNoInteractions(injected);
    }

    /**
     * Covers {@code if (dataSource == null)} false in {@link DatabaseConfig#getDataSource()} without creating a
     * second {@link HikariDataSource}.
     */
    @Test
    void getDataSource_poolPreInjected_skipsHikariConstructionAndSchemaBootstrap() throws Exception {
        HikariDataSource injected = mock(HikariDataSource.class);
        Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(null, injected);
        AtomicInteger hikariConstructions = new AtomicInteger();
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> hikariConstructions.incrementAndGet());
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            assertThat(DatabaseConfig.getDataSource()).contains(injected);
        }
        assertThat(hikariConstructions.get()).isZero();
        verifyNoInteractions(injected);
    }

    /**
     * When {@code database.url} is null, neither MySQL nor PostgreSQL URL hints apply; Flyway runs on the pool.
     */
    @Test
    void getDataSource_nullDatabaseUrl_runsFlywayElsePath() throws Exception {
        Connection connection = mock(Connection.class);
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> when(mock.getConnection()).thenReturn(connection));
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class);
             MockedStatic<Flyway> fly = mockStatic(Flyway.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn(null);
            app.when(AppConfig::getDatabaseUsername).thenReturn("sa");
            app.when(AppConfig::getDatabasePassword).thenReturn("");

            FluentConfiguration cfg = mock(FluentConfiguration.class);
            Flyway fw = mock(Flyway.class);
            MigrateResult mr = mock(MigrateResult.class);
            when(cfg.dataSource(any(DataSource.class))).thenReturn(cfg);
            when(cfg.locations("classpath:com/appointmentscheduler/persistence/database/migration")).thenReturn(cfg);
            when(cfg.baselineOnMigrate(true)).thenReturn(cfg);
            when(cfg.load()).thenReturn(fw);
            when(fw.migrate()).thenReturn(mr);

            fly.when(() -> Flyway.configure(any(ClassLoader.class))).thenReturn(cfg);

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            verify(fw).migrate();
            verify(cfg, never()).baselineVersion(anyString());
        }
    }

    /**
     * Second call to {@link DatabaseConfig#initializePoolIfAbsent()} after the pool exists hits the inner
     * {@code if (dataSource == null)} false branch (same intent as the second thread in double-checked locking,
     * without cross-thread {@link MockedConstruction} limitations).
     */
    @Test
    void initializePoolIfAbsent_secondCall_skipsInnerCreationWhenPoolAlreadyInitialized() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);

        AtomicInteger hikariConstructions = new AtomicInteger();
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> {
                    hikariConstructions.incrementAndGet();
                    when(mock.getConnection()).thenReturn(connection);
                });
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::isDatabaseEnabled).thenReturn(true);
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            app.when(AppConfig::getDatabaseUsername).thenReturn("u");
            app.when(AppConfig::getDatabasePassword).thenReturn("p");

            assertThat(DatabaseConfig.getDataSource()).isPresent();
            assertThat(hikariConstructions.get()).isEqualTo(1);

            DatabaseConfig.initializePoolIfAbsent();
            assertThat(hikariConstructions.get()).isEqualTo(1);
        }
    }

    @Test
    void initializePoolIfAbsent_skipsInnerBlockWhenPoolPreInjected() throws Exception {
        HikariDataSource injected = mock(HikariDataSource.class);
        Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(null, injected);
        AtomicInteger hikariConstructions = new AtomicInteger();
        try (MockedConstruction<HikariDataSource> ignored = mockConstruction(HikariDataSource.class,
                (mock, ctx) -> hikariConstructions.incrementAndGet());
             MockedStatic<AppConfig> app = mockStatic(AppConfig.class)) {
            app.when(AppConfig::getDatabaseUrl).thenReturn("jdbc:mysql://localhost:3306/appointment");
            DatabaseConfig.initializePoolIfAbsent();
        }
        assertThat(hikariConstructions.get()).isZero();
    }

    @Test
    void shutdown_whenPoolNeverInitialized_completesWithoutException() {
        assertThatCode(DatabaseConfig::shutdown).doesNotThrowAnyException();
    }

    @Test
    void shutdown_whenPoolAlreadyClosed_skipsClose() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.isClosed()).thenReturn(true);
        Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(null, ds);
        DatabaseConfig.shutdown();
        verify(ds, never()).close();
    }

    @Test
    void shutdown_whenPoolOpen_closesAndClearsStaticPool() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.isClosed()).thenReturn(false);
        Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(null, ds);
        DatabaseConfig.shutdown();
        verify(ds).close();
        assertThat(f.get(null)).isNull();
    }

}
