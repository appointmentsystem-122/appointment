package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.persistence.database.JdbcAppointmentRepository;
import com.appointmentscheduler.persistence.database.JdbcAuditEntryRepository;
import com.appointmentscheduler.persistence.database.JdbcClinicRepository;
import com.appointmentscheduler.persistence.database.JdbcDoctorRepository;
import com.appointmentscheduler.persistence.database.JdbcRoomRepository;
import com.appointmentscheduler.persistence.database.JdbcUserRepository;
import com.appointmentscheduler.persistence.database.DatabaseConfig;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers {@link MainApp#initializeServices()} JDBC success path: {@code dsOpt.isPresent()},
 * database product name logging (PostgreSQL / MySQL / default), and the warning fallback when
 * {@link DatabaseConfig#getDataSource()} returns empty while DB is enabled.
 */
@ResourceLock("ApplicationContextServices")
class MainAppJdbcInitializeBranchCoverageTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void resetContextAndConfig() throws Exception {
        resetApplicationContext();
        reloadClasspathProperties();
    }

    @Test
    void initializeServices_jdbcPath_logsPostgreSql_whenUrlContainsPostgresql() throws Exception {
        runJdbcInitWithUrl("jdbc:postgresql://localhost:5432/appointment");
        assertThat(ApplicationContext.isUsingDatabase()).isTrue();
        assertThat(ApplicationContext.getAuthService()).isNotNull();
    }

    @Test
    void initializeServices_jdbcPath_logsMySql_whenUrlContainsMysql() throws Exception {
        runJdbcInitWithUrl("jdbc:mysql://localhost:3306/appointment");
        assertThat(ApplicationContext.isUsingDatabase()).isTrue();
    }

    @Test
    void initializeServices_jdbcPath_logsDefaultDbName_whenUrlIsNeitherPgNorMySql() throws Exception {
        runJdbcInitWithUrl("jdbc:h2:mem:coverage_init;DB_CLOSE_DELAY=-1");
        assertThat(ApplicationContext.isUsingDatabase()).isTrue();
    }

    @Test
    void initializeServices_databaseEnabled_emptyDataSource_optional_fallsBackToInMemoryAndSchedulesWarning() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:mysql://localhost:3306/appointment");
        applyPropertiesForTest(p);

        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class)) {
            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.empty());

            MainApp app = new MainApp();
            Method init = MainApp.class.getDeclaredMethod("initializeServices");
            init.setAccessible(true);
            assertThatCode(() -> init.invoke(app)).doesNotThrowAnyException();

            assertThat(ApplicationContext.isUsingDatabase()).isFalse();
            assertThat(ApplicationContext.getAuthService()).isNotNull();

            CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(done::countDown);
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void runJdbcInitWithUrl(String databaseUrl) throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", databaseUrl);
        applyPropertiesForTest(p);

        DataSource ds = mock(DataSource.class);
        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class);
             MockedConstruction<JdbcUserRepository> c1 = mockConstruction(JdbcUserRepository.class,
                     (mock, ctx) -> {
                         when(mock.findAll()).thenReturn(Collections.emptyList());
                         when(mock.findByEmail(anyString())).thenReturn(Optional.empty());
                     });
             MockedConstruction<JdbcAppointmentRepository> c2 = mockConstruction(JdbcAppointmentRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             MockedConstruction<JdbcDoctorRepository> c3 = mockConstruction(JdbcDoctorRepository.class);
             MockedConstruction<JdbcRoomRepository> c4 = mockConstruction(JdbcRoomRepository.class);
             MockedConstruction<JdbcClinicRepository> c5 = mockConstruction(JdbcClinicRepository.class);
             MockedConstruction<JdbcAuditEntryRepository> c6 = mockConstruction(JdbcAuditEntryRepository.class)) {

            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.of(ds));

            MainApp app = new MainApp();
            Method init = MainApp.class.getDeclaredMethod("initializeServices");
            init.setAccessible(true);
            assertThatCode(() -> init.invoke(app)).doesNotThrowAnyException();
        }
    }

    private static void applyPropertiesForTest(Properties p) throws Exception {
        Method apply = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        apply.setAccessible(true);
        apply.invoke(null, p);
    }

    private static void reloadClasspathProperties() throws Exception {
        Method reload = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("reloadClasspathPropertiesForTest");
        reload.setAccessible(true);
        reload.invoke(null);
    }

    private static void resetApplicationContext() throws Exception {
        for (Method m : ApplicationContext.class.getDeclaredMethods()) {
            if (!m.getName().startsWith("set") || m.getParameterCount() != 1) {
                continue;
            }
            Class<?> t = m.getParameterTypes()[0];
            if (t == boolean.class) {
                m.invoke(null, false);
            } else {
                m.invoke(null, new Object[]{null});
            }
        }
    }
}
