package com.appointmentscheduler.application;

import com.appointmentscheduler.persistence.database.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** H2 + Flyway integration for DatabaseConfig. MySQL/Postgres URL branches need real DBs or reflection tests. */
@ResourceLock("AppConfigProps")
class DatabaseConfigTest {

    @BeforeEach
    @AfterEach
    void resetPoolAndConfig() {
        DatabaseConfig.shutdown();
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void getDataSource_whenDatabaseDisabled_returnsEmpty() {
        Properties p = new Properties();
        p.setProperty("database.enabled", "false");
        AppConfig.applyPropertiesForTest(p);
        assertThat(DatabaseConfig.getDataSource()).isEmpty();
    }

    @Test
    void getDataSource_withH2_initializesPoolAndFlyway() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:h2:mem:dbconfig_flyway;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        p.setProperty("database.username", "sa");
        p.setProperty("database.password", "");
        AppConfig.applyPropertiesForTest(p);

        assertThat(DatabaseConfig.getDataSource()).isPresent();
        DataSource ds1 = DatabaseConfig.getDataSource().get();
        assertThat(DatabaseConfig.getDataSource().get()).isSameAs(ds1);

        try (Connection c = ds1.getConnection()) {
            assertThat(c.isValid(2)).isTrue();
        }

        DatabaseConfig.shutdown();
        assertThat(DatabaseConfig.getDataSource()).isPresent();
        DataSource ds2 = DatabaseConfig.getDataSource().get();
        assertThat(ds2).isNotSameAs(ds1);
    }

    @Test
    void shutdown_whenPoolNeverCreated_isNoOp() {
        Properties p = new Properties();
        p.setProperty("database.enabled", "false");
        AppConfig.applyPropertiesForTest(p);
        DatabaseConfig.shutdown();
        assertThat(DatabaseConfig.getDataSource()).isEmpty();
    }
}
