package com.appointmentscheduler.persistence.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import com.appointmentscheduler.application.AppConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises private helpers in {@link DatabaseConfig} via reflection so branch coverage includes
 * MySQL error-code handling and Flyway migration resource fallbacks without a real MySQL/PostgreSQL server.
 */
class DatabaseConfigReflectionBranchTest {

    @Test
    void mySqlColumnOrTableAlreadyOk_errorCodesAndMessages() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("mySqlColumnOrTableAlreadyOk", SQLException.class);
        m.setAccessible(true);

        assertThat(m.invoke(null, new SQLException("", "", 1060))).isEqualTo(true);
        assertThat(m.invoke(null, new SQLException("", "", 1146))).isEqualTo(true);
        assertThat(m.invoke(null, new SQLException("Duplicate column name 'x'", "", 0))).isEqualTo(true);
        assertThat(m.invoke(null, new SQLException("duplicate column foo", "", 0))).isEqualTo(true);

        assertThat(m.invoke(null, new SQLException("fatal", "", 9999))).isEqualTo(false);
        assertThat(m.invoke(null, new SQLException())).isEqualTo(false);
        SQLException nullMsg = new SQLException(null, "", 0);
        assertThat(nullMsg.getMessage()).isNull();
        assertThat(m.invoke(null, nullMsg)).isEqualTo(false);
    }

    @Test
    void migrationFallbackUrl_onlyMigrationPackage() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationFallbackUrl", String.class);
        m.setAccessible(true);

        assertThat(m.invoke(null, "com/appointmentscheduler/persistence/database/migration/V1__enterprise_schema.sql"))
                .isNotNull();
        assertThat(m.invoke(null, "wrong/prefix/V1__x.sql")).isNull();
    }

    @Test
    void migrationFallbackStream_onlyMigrationPackage() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationFallbackStream", String.class);
        m.setAccessible(true);

        try (InputStream in = (InputStream) m.invoke(null,
                "com/appointmentscheduler/persistence/database/migration/V1__enterprise_schema.sql")) {
            assertThat(in).isNotNull();
        }
        assertThat(m.invoke(null, "wrong/prefix/V1__x.sql")).isNull();
    }

    @Test
    void migrationClassLoader_whenContextClassLoaderNull_stillResolvesMigrationResource() throws Exception {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
            m.setAccessible(true);
            ClassLoader cl = (ClassLoader) m.invoke(null);
            String path = "com/appointmentscheduler/persistence/database/migration/V1__enterprise_schema.sql";
            assertThat(cl.getResource(path)).isNotNull();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Test
    void migrationClassLoader_getResourceDelegatesToFallback() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);

        String path = "com/appointmentscheduler/persistence/database/migration/V2__booking_request_fields.sql";
        URL u = cl.getResource(path);
        assertThat(u).isNotNull();

        try (InputStream in = cl.getResourceAsStream(path)) {
            assertThat(in).isNotNull();
        }
    }

    @Test
    void migrationClassLoader_getResources_enumeratesMigrationSql() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);
        String path = "com/appointmentscheduler/persistence/database/migration/V1__enterprise_schema.sql";
        Enumeration<URL> en = cl.getResources(path);
        assertThat(en.hasMoreElements()).isTrue();
        assertThat(en.nextElement()).isNotNull();
    }

    /**
     * {@link ClassLoader#getResources(String)} delegates to the custom {@code findResources} for the
     * migration prefix branch where {@code migrationFallbackUrl} returns null (no reflection on
     * {@code ClassLoader.findResources} — JPMS blocks that on modern JDKs).
     */
    @Test
    void migrationClassLoader_getResources_wrongMigrationPrefix_emptyViaFindResources() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);
        Enumeration<URL> en = cl.getResources("com/other/not_migration.sql");
        assertThat(en.hasMoreElements()).isFalse();
    }

    @Test
    void runMigrations_postgresUrl_setsBaselineVersion() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.url", "jdbc:postgresql://localhost:5432/appointment");
        applyAppConfigProps(p);
        try {
            Method m = DatabaseConfig.class.getDeclaredMethod("runMigrations", DataSource.class);
            m.setAccessible(true);

            DataSource ds = mock(DataSource.class);
            FluentConfiguration cfg = mock(FluentConfiguration.class);
            Flyway fw = mock(Flyway.class);
            MigrateResult mr = mock(MigrateResult.class);

            when(cfg.dataSource(ds)).thenReturn(cfg);
            when(cfg.locations("classpath:com/appointmentscheduler/persistence/database/migration")).thenReturn(cfg);
            when(cfg.baselineOnMigrate(true)).thenReturn(cfg);
            when(cfg.baselineVersion("1")).thenReturn(cfg);
            when(cfg.load()).thenReturn(fw);
            when(fw.migrate()).thenReturn(mr);

            try (MockedStatic<Flyway> ms = org.mockito.Mockito.mockStatic(Flyway.class)) {
                ms.when(() -> Flyway.configure(any(ClassLoader.class))).thenReturn(cfg);
                m.invoke(null, ds);
            }

            verify(cfg).baselineVersion("1");
            verify(fw).migrate();
        } finally {
            reloadAppConfig();
        }
    }

    @Test
    void runMigrations_nonPostgresUrl_skipsBaselineVersion() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.url", "jdbc:h2:mem:dbcfg_runmig");
        applyAppConfigProps(p);
        try {
            Method m = DatabaseConfig.class.getDeclaredMethod("runMigrations", DataSource.class);
            m.setAccessible(true);

            DataSource ds = mock(DataSource.class);
            FluentConfiguration cfg = mock(FluentConfiguration.class);
            Flyway fw = mock(Flyway.class);
            MigrateResult mr = mock(MigrateResult.class);

            when(cfg.dataSource(ds)).thenReturn(cfg);
            when(cfg.locations("classpath:com/appointmentscheduler/persistence/database/migration")).thenReturn(cfg);
            when(cfg.baselineOnMigrate(true)).thenReturn(cfg);
            when(cfg.load()).thenReturn(fw);
            when(fw.migrate()).thenReturn(mr);

            try (MockedStatic<Flyway> ms = org.mockito.Mockito.mockStatic(Flyway.class)) {
                ms.when(() -> Flyway.configure(any(ClassLoader.class))).thenReturn(cfg);
                m.invoke(null, ds);
            }

            verify(cfg, never()).baselineVersion("1");
            verify(fw).migrate();
        } finally {
            reloadAppConfig();
        }
    }

    @Test
    void ensureMySqlAppointmentBookingColumns_missingTableErrorCode1146_isSkipped() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensureMySqlAppointmentBookingColumns", Connection.class);
        m.setAccessible(true);

        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenThrow(new SQLException("Table doesn't exist", "", 1146));

        assertThat(m.invoke(null, c)).isNull();
        verify(c, org.mockito.Mockito.times(5)).createStatement();
    }

    @Test
    void ensureMySqlAppointmentBookingColumns_duplicateOrMissingTable_areSkipped() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensureMySqlAppointmentBookingColumns", Connection.class);
        m.setAccessible(true);

        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenThrow(new SQLException("Duplicate column name 'x'", "", 1060));

        // All ALTER TABLE executions throw duplicate-column and must be swallowed.
        assertThat(m.invoke(null, c)).isNull();
        verify(c, org.mockito.Mockito.times(5)).createStatement();
    }

    @Test
    void ensureMySqlAppointmentBookingColumns_unhandledSqlError_propagates() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensureMySqlAppointmentBookingColumns", Connection.class);
        m.setAccessible(true);

        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenThrow(new SQLException("fatal syntax", "", 9999));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> m.invoke(null, c))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(SQLException.class);
    }

    /**
     * Duplicate column detected only via message (non-1060 code) still hits the {@code mySqlColumnOrTableAlreadyOk}
     * message branch and skips the ALTER.
     */
    @Test
    void ensureMySqlAppointmentBookingColumns_duplicateColumnMessageLowCode_isSkipped() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensureMySqlAppointmentBookingColumns", Connection.class);
        m.setAccessible(true);

        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenThrow(new SQLException("duplicate column name 'customer_notes'", "", 5000));

        assertThat(m.invoke(null, c)).isNull();
        verify(c, org.mockito.Mockito.times(5)).createStatement();
    }

    @Test
    void ensureMySqlSchema_connectionFailure_wrapsRuntimeException() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensureMySqlSchema", javax.sql.DataSource.class);
        m.setAccessible(true);

        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("db down"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> m.invoke(null, ds))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("db down");
    }

    @Test
    void ensurePostgresSchema_statementFailure_wrapsRuntimeException() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensurePostgresSchema", javax.sql.DataSource.class);
        m.setAccessible(true);

        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenThrow(new SQLException("cannot create schema"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> m.invoke(null, ds))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void ensureMySqlSchema_successPath_executesCreateAndAlterFlow() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensureMySqlSchema", javax.sql.DataSource.class);
        m.setAccessible(true);

        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenReturn(true);

        assertThat(m.invoke(null, ds)).isNull();
        verify(st, org.mockito.Mockito.atLeastOnce()).execute(anyString());
    }

    @Test
    void ensurePostgresSchema_successPath_executesStatements() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("ensurePostgresSchema", javax.sql.DataSource.class);
        m.setAccessible(true);

        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenReturn(true);

        assertThat(m.invoke(null, ds)).isNull();
        verify(st, org.mockito.Mockito.atLeastOnce()).execute(anyString());
    }

    @Test
    void shutdown_whenPoolNull_orClosed_orOpen_coversBranches() throws Exception {
        java.lang.reflect.Field f = DatabaseConfig.class.getDeclaredField("dataSource");
        f.setAccessible(true);

        // null branch
        f.set(null, null);
        DatabaseConfig.shutdown();

        // already-closed branch
        HikariDataSource closed = mock(HikariDataSource.class);
        when(closed.isClosed()).thenReturn(true);
        f.set(null, closed);
        DatabaseConfig.shutdown();
        assertThat(f.get(null)).isSameAs(closed);

        // open branch (close + null assign)
        HikariDataSource open = mock(HikariDataSource.class);
        when(open.isClosed()).thenReturn(false);
        f.set(null, open);
        DatabaseConfig.shutdown();
        verify(open).close();
        assertThat(f.get(null)).isNull();
    }

    private static void applyAppConfigProps(Properties p) throws Exception {
        Method m = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        m.setAccessible(true);
        m.invoke(null, p);
    }

    private static void reloadAppConfig() throws Exception {
        Method m = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("reloadClasspathPropertiesForTest");
        m.setAccessible(true);
        m.invoke(null);
    }

    @Test
    void migrationClassLoader_getResourceAsStream_hitsFallbackPath() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);
        String path = "com/appointmentscheduler/persistence/database/migration/V2__booking_request_fields.sql";
        try (InputStream in = cl.getResourceAsStream(path)) {
            assertThat(in).isNotNull();
        }
    }

    @Test
    void migrationClassLoader_parentResourceBranches_nonFallbackPath() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);

        // Uses parent classloader normal resolution (not migration fallback branch).
        assertThat(cl.getResource("java/lang/String.class")).isNotNull();
        try (InputStream in = cl.getResourceAsStream("java/lang/String.class")) {
            assertThat(in).isNotNull();
        }
    }

    @Test
    void migrationClassLoader_migrationPathMissingFile_returnsNullFromGetResource() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);
        String path = "com/appointmentscheduler/persistence/database/migration/NO_SUCH_FILE_99999.sql";
        assertThat(cl.getResource(path)).isNull();
        assertThat(cl.getResourceAsStream(path)).isNull();
    }

    @Test
    void migrationClassLoader_findResources_missingMigrationFile_returnsEmptyEnumeration() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);
        String path = "com/appointmentscheduler/persistence/database/migration/NO_SUCH_FILE_99999.sql";
        Enumeration<URL> en = cl.getResources(path);
        assertThat(en.hasMoreElements()).isFalse();
    }

    @Test
    void migrationClassLoader_nonMigrationUnknownPath_returnsNullOrEmpty() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("migrationClassLoader");
        m.setAccessible(true);
        ClassLoader cl = (ClassLoader) m.invoke(null);

        String unknown = "not/a/migration/resource/that/does/not/exist.txt";
        assertThat(cl.getResource(unknown)).isNull();
        assertThat(cl.getResourceAsStream(unknown)).isNull();
        Enumeration<URL> en = cl.getResources(unknown);
        assertThat(en.hasMoreElements()).isFalse();
    }

    @Test
    void runMigrations_nullDatabaseUrl_treatedAsNonPostgres() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("runMigrations", DataSource.class);
        m.setAccessible(true);

        DataSource ds = mock(DataSource.class);
        FluentConfiguration cfg = mock(FluentConfiguration.class);
        Flyway fw = mock(Flyway.class);
        MigrateResult mr = mock(MigrateResult.class);

        when(cfg.dataSource(ds)).thenReturn(cfg);
        when(cfg.locations("classpath:com/appointmentscheduler/persistence/database/migration")).thenReturn(cfg);
        when(cfg.baselineOnMigrate(true)).thenReturn(cfg);
        when(cfg.load()).thenReturn(fw);
        when(fw.migrate()).thenReturn(mr);

        try (MockedStatic<Flyway> ms = org.mockito.Mockito.mockStatic(Flyway.class);
             MockedStatic<AppConfig> ac = org.mockito.Mockito.mockStatic(AppConfig.class)) {
            ac.when(AppConfig::getDatabaseUrl).thenReturn(null);
            ms.when(() -> Flyway.configure(any(ClassLoader.class))).thenReturn(cfg);
            m.invoke(null, ds);
        }

        verify(cfg, never()).baselineVersion("1");
        verify(fw).migrate();
    }

    @Test
    void resolveMigrationBaseClassLoader_prefersNamedModuleLoader() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("resolveMigrationBaseClassLoader",
                boolean.class, ClassLoader.class, ClassLoader.class, ClassLoader.class, ClassLoader.class);
        m.setAccessible(true);

        ClassLoader moduleLoader = new ClassLoader() { };
        ClassLoader classLoader = new ClassLoader() { };
        ClassLoader ctxLoader = new ClassLoader() { };
        ClassLoader sysLoader = new ClassLoader() { };

        Object out = m.invoke(null, true, moduleLoader, classLoader, ctxLoader, sysLoader);
        assertThat(out).isSameAs(moduleLoader);
    }

    @Test
    void resolveMigrationBaseClassLoader_fallsBackToClassLoader() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("resolveMigrationBaseClassLoader",
                boolean.class, ClassLoader.class, ClassLoader.class, ClassLoader.class, ClassLoader.class);
        m.setAccessible(true);

        ClassLoader classLoader = new ClassLoader() { };
        ClassLoader ctxLoader = new ClassLoader() { };
        ClassLoader sysLoader = new ClassLoader() { };

        Object out = m.invoke(null, false, null, classLoader, ctxLoader, sysLoader);
        assertThat(out).isSameAs(classLoader);
    }

    @Test
    void resolveMigrationBaseClassLoader_namedModuleWithoutLoader_fallsBackToClassLoader() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("resolveMigrationBaseClassLoader",
                boolean.class, ClassLoader.class, ClassLoader.class, ClassLoader.class, ClassLoader.class);
        m.setAccessible(true);

        ClassLoader classLoader = new ClassLoader() { };
        ClassLoader ctxLoader = new ClassLoader() { };
        ClassLoader sysLoader = new ClassLoader() { };

        Object out = m.invoke(null, true, null, classLoader, ctxLoader, sysLoader);
        assertThat(out).isSameAs(classLoader);
    }

    @Test
    void resolveMigrationBaseClassLoader_fallsBackToContextLoader() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("resolveMigrationBaseClassLoader",
                boolean.class, ClassLoader.class, ClassLoader.class, ClassLoader.class, ClassLoader.class);
        m.setAccessible(true);

        ClassLoader ctxLoader = new ClassLoader() { };
        ClassLoader sysLoader = new ClassLoader() { };

        Object out = m.invoke(null, false, null, null, ctxLoader, sysLoader);
        assertThat(out).isSameAs(ctxLoader);
    }

    @Test
    void resolveMigrationBaseClassLoader_fallsBackToSystemLoader() throws Exception {
        Method m = DatabaseConfig.class.getDeclaredMethod("resolveMigrationBaseClassLoader",
                boolean.class, ClassLoader.class, ClassLoader.class, ClassLoader.class, ClassLoader.class);
        m.setAccessible(true);

        ClassLoader sysLoader = new ClassLoader() { };

        Object out = m.invoke(null, false, null, null, null, sysLoader);
        assertThat(out).isSameAs(sysLoader);
    }
}
