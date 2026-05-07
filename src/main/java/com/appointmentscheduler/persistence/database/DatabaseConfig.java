package com.appointmentscheduler.persistence.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appointmentscheduler.application.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Enterprise database configuration: HikariCP connection pool and Flyway migrations.
 * Single responsibility: provide a configured DataSource and run migrations.
 */
public final class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final AtomicReference<HikariDataSource> dataSource = new AtomicReference<>();

    /**
     * Returns a shared DataSource when database is enabled; otherwise empty.
     */
    public static Optional<DataSource> getDataSource() {
        if (!AppConfig.isDatabaseEnabled()) {
            return Optional.empty();
        }
        if (dataSource.get() == null) {
            initializePoolIfAbsent();
        }
        return Optional.of(dataSource.get());
    }

    /**
     * Lazy pool creation with double-checked locking semantics (inner guard lives here).
     * Package-private so tests can invoke a second time to cover the {@code dataSource != null} path
     * inside the synchronized block without cross-thread flakiness.
     */
    static void initializePoolIfAbsent() {
        synchronized (DatabaseConfig.class) {
            if (dataSource.get() == null) {
                HikariDataSource ds = createPool();
                dataSource.set(ds);

                String url = AppConfig.getDatabaseUrl();
                boolean isPostgres = url != null && url.toLowerCase().contains("postgresql");
                boolean isMySql = url != null && url.toLowerCase().contains("mysql");

                if (isMySql) {
                    ensureMySqlSchema(ds);
                } else if (isPostgres) {
                    ensurePostgresSchema(ds);
                } else {
                    runMigrations(ds);
                }
            }
        }
    }

    /**
     * Shuts down the connection pool. Call on application exit.
     */
    public static void shutdown() {
        HikariDataSource ds = dataSource.get();
        if (ds != null && !ds.isClosed()) {
            ds.close();
            dataSource.set(null);
            log.info("Database connection pool closed");
        }
    }

    private static HikariDataSource createPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.getDatabaseUrl());
        config.setUsername(AppConfig.getDatabaseUsername());
        config.setPassword(AppConfig.getDatabasePassword());
        config.setPoolName("AppointmentBookingPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "256");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource ds = new HikariDataSource(config);
        log.info("Database connection pool initialized: {}", config.getJdbcUrl());
        return ds;
    }

    /** Creates app_user table in MySQL/MariaDB if it does not exist (for phpMyAdmin). */
    private static void ensureMySqlSchema(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS app_user ("
                        + "id CHAR(36) NOT NULL PRIMARY KEY, "
                        + "name VARCHAR(200) NOT NULL, "
                        + "email VARCHAR(320) NOT NULL, "
                        + "password_hash VARCHAR(255) NOT NULL, "
                        + "user_type VARCHAR(50) NOT NULL DEFAULT 'PATIENT', "
                        + "is_active TINYINT(1) NOT NULL DEFAULT 1, "
                        + "created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), "
                        + "updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6), "
                        + "UNIQUE KEY uq_app_user_email (email)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                log.info("MySQL: table app_user verified/created");
            }
            ensureMySqlAppointmentBookingColumns(c);
        } catch (Exception e) {
            log.error("MySQL schema setup failed: {}", e.getMessage());
            throw new RuntimeException("MySQL schema setup failed", e);
        }
    }

    /**
     * Flyway V2 columns for booking extras; JDBC always writes them. Older MySQL imports may lack these.
     * Duplicate-column / missing-table errors are ignored.
     */
    private static void ensureMySqlAppointmentBookingColumns(Connection c) throws SQLException {
        String[] ddl = {
                "ALTER TABLE appointment ADD COLUMN customer_notes VARCHAR(2000) NULL",
                "ALTER TABLE appointment ADD COLUMN contact_phone VARCHAR(64) NULL",
                "ALTER TABLE appointment ADD COLUMN reminder_channel VARCHAR(32) NULL",
                "ALTER TABLE appointment ADD COLUMN accessibility_needs VARCHAR(512) NULL",
                "ALTER TABLE appointment ADD COLUMN preferred_language VARCHAR(16) NULL"
        };
        for (String sql : ddl) {
            try (Statement st = c.createStatement()) {
                st.execute(sql);
                log.info("MySQL: {}", sql);
            } catch (SQLException e) {
                if (mySqlColumnOrTableAlreadyOk(e)) {
                    log.debug("MySQL (skipped): {} — {}", sql, e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    private static boolean mySqlColumnOrTableAlreadyOk(SQLException e) {
        int code = e.getErrorCode();
        if (code == 1060) return true; // ER_DUP_FIELDNAME
        if (code == 1146) return true; // ER_NO_SUCH_TABLE (e.g. appointment not created yet)
        String m = e.getMessage() != null ? e.getMessage() : "";
        return m.contains("Duplicate column") || m.contains("duplicate column");
    }

    /** Creates schema and app_user table in PostgreSQL if they do not exist. */
    private static void ensurePostgresSchema(DataSource ds) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA IF NOT EXISTS appointment");
            st.execute("CREATE TABLE IF NOT EXISTS appointment.app_user ("
                    + "id UUID PRIMARY KEY, "
                    + "name VARCHAR(200) NOT NULL, "
                    + "email VARCHAR(320) NOT NULL UNIQUE, "
                    + "password_hash VARCHAR(255) NOT NULL, "
                    + "user_type VARCHAR(50) NOT NULL DEFAULT 'PATIENT', "
                    + "is_active BOOLEAN NOT NULL DEFAULT true, "
                    + "created_at TIMESTAMPTZ NOT NULL DEFAULT now(), "
                    + "updated_at TIMESTAMPTZ NOT NULL DEFAULT now()"
                    + ")");
            log.info("PostgreSQL: schema and table appointment.app_user verified/created");
        } catch (Exception e) {
            log.error("PostgreSQL schema setup failed: {}", e.getMessage());
            throw new RuntimeException("PostgreSQL schema setup failed", e);
        }
    }

    /**
     * ClassLoader Flyway uses to read SQL migrations. Flyway 9 {@code ClassPathResource} uses
     * {@link ClassLoader#getResources(String)} (and thus {@link ClassLoader#findResources(String)} on this
     * loader), not only {@link ClassLoader#getResource(String)}. Under IntelliJ + JPMS the parent loader can
     * return no URLs for some migration files; we append URLs from {@link DatabaseConfig#getResource(String)}.
     */
    private static ClassLoader migrationClassLoader() {
        Module mod = DatabaseConfig.class.getModule();
        ClassLoader base = resolveMigrationBaseClassLoader(
                mod.isNamed(),
                mod.getClassLoader(),
                DatabaseConfig.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader());
        final ClassLoader parent = base;
        return new ClassLoader(parent) {
            @Override
            public URL getResource(String name) {
                URL u = super.getResource(name);
                if (u != null) {
                    return u;
                }
                return migrationFallbackUrl(name);
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                InputStream in = super.getResourceAsStream(name);
                if (in != null) {
                    return in;
                }
                return migrationFallbackStream(name);
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                URL u = migrationFallbackUrl(name);
                if (u != null) {
                    return Collections.enumeration(Collections.singletonList(u));
                }
                return Collections.emptyEnumeration();
            }
        };
    }

    private static ClassLoader resolveMigrationBaseClassLoader(boolean moduleNamed,
                                                               ClassLoader moduleClassLoader,
                                                               ClassLoader classClassLoader,
                                                               ClassLoader contextClassLoader,
                                                               ClassLoader systemClassLoader) {
        if (moduleNamed && moduleClassLoader != null) {
            return moduleClassLoader;
        }
        if (classClassLoader != null) {
            return classClassLoader;
        }
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return systemClassLoader;
    }

    private static URL migrationFallbackUrl(String name) {
        if (!name.startsWith("com/appointmentscheduler/persistence/database/migration/")) {
            return null;
        }
        return DatabaseConfig.class.getResource("/" + name);
    }

    private static InputStream migrationFallbackStream(String name) {
        if (!name.startsWith("com/appointmentscheduler/persistence/database/migration/")) {
            return null;
        }
        return DatabaseConfig.class.getResourceAsStream("/" + name);
    }

    private static void runMigrations(DataSource ds) {
        String url = AppConfig.getDatabaseUrl();
        boolean isPostgres = url != null && url.toLowerCase().contains("postgresql");

        var config = Flyway.configure(migrationClassLoader())
                .dataSource(ds)
                .locations("classpath:com/appointmentscheduler/persistence/database/migration")
                .baselineOnMigrate(true);

        if (isPostgres) {
            // PostgreSQL schema was created manually (pgAdmin). Treat as already at version 1.
            config.baselineVersion("1");
            log.info("PostgreSQL detected: baselining Flyway (schema created externally)");
        }

        Flyway flyway = config.load();
        int count = flyway.migrate().migrationsExecuted;
        log.info("Flyway migrations executed: {}", count);
    }
}