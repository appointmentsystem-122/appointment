package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.persistence.database.DatabaseConfig;
import com.appointmentscheduler.persistence.database.JdbcAppointmentRepository;
import com.appointmentscheduler.persistence.database.JdbcAuditEntryRepository;
import com.appointmentscheduler.persistence.database.JdbcClinicRepository;
import com.appointmentscheduler.persistence.database.JdbcDoctorRepository;
import com.appointmentscheduler.persistence.database.JdbcRoomRepository;
import com.appointmentscheduler.persistence.database.JdbcUserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Covers {@link MainApp#initializeServices()} branches that use a JDBC {@link DataSource}:
 * database product logging (PostgreSQL / MySQL / default) and fallback when no data source is available.
 */
class MainAppInitializeServicesJdbcPathTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void resetContextAndConfig() throws Exception {
        String prevAuto = System.getProperty("app.test.autoDialogs");
        try {
            resetApplicationContext();
            Method reload = Class.forName("com.appointmentscheduler.application.AppConfig")
                    .getDeclaredMethod("reloadClasspathPropertiesForTest");
            reload.setAccessible(true);
            reload.invoke(null);
        } finally {
            if (prevAuto == null) {
                System.clearProperty("app.test.autoDialogs");
            } else {
                System.setProperty("app.test.autoDialogs", prevAuto);
            }
            System.clearProperty("app.test.jdbcRepositoryWarmup");
        }
    }

    private static void resetApplicationContext() throws Exception {
        for (Method m : ApplicationContext.class.getDeclaredMethods()) {
            if (!m.getName().startsWith("set") || m.getParameterCount() != 1) {
                continue;
            }
            m.setAccessible(true);
            Class<?> pt = m.getParameterTypes()[0];
            if (pt == boolean.class) {
                m.invoke(null, false);
            } else {
                m.invoke(null, new Object[]{null});
            }
        }
    }

    private static void applyProps(Properties p) throws Exception {
        Method apply = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        apply.setAccessible(true);
        apply.invoke(null, p);
    }

    private static void invokeInitializeServices(MainApp app) throws Exception {
        Method init = MainApp.class.getDeclaredMethod("initializeServices");
        init.setAccessible(true);
        assertThatCode(() -> init.invoke(app)).doesNotThrowAnyException();
    }

    @Test
    void initializeServices_withJdbcDataSource_logsPostgresBranch() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:postgresql://localhost:5432/appointment");
        applyProps(p);

        DataSource ds = mock(DataSource.class);
        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class);
             var uc = mockConstruction(JdbcUserRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var ac = mockConstruction(JdbcAppointmentRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var dc = mockConstruction(JdbcDoctorRepository.class);
             var rc = mockConstruction(JdbcRoomRepository.class);
             var cc = mockConstruction(JdbcClinicRepository.class);
             var ax = mockConstruction(JdbcAuditEntryRepository.class)) {

            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.of(ds));

            invokeInitializeServices(new MainApp());
            assertThat(ApplicationContext.isUsingDatabase()).isTrue();
            assertThat(ApplicationContext.getAuthService()).isNotNull();
        }
    }

    @Test
    void initializeServices_withJdbcDataSource_logsMySqlBranch() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:mysql://localhost:3306/appointment");
        applyProps(p);

        DataSource ds = mock(DataSource.class);
        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class);
             var uc = mockConstruction(JdbcUserRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var ac = mockConstruction(JdbcAppointmentRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var dc = mockConstruction(JdbcDoctorRepository.class);
             var rc = mockConstruction(JdbcRoomRepository.class);
             var cc = mockConstruction(JdbcClinicRepository.class);
             var ax = mockConstruction(JdbcAuditEntryRepository.class)) {

            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.of(ds));

            invokeInitializeServices(new MainApp());
            assertThat(ApplicationContext.isUsingDatabase()).isTrue();
        }
    }

    @Test
    void initializeServices_withJdbcDataSource_logsDefaultDbNameWhenNotPgOrMySql() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:h2:mem:init_svc_test;DB_CLOSE_DELAY=-1");
        applyProps(p);

        DataSource ds = mock(DataSource.class);
        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class);
             var uc = mockConstruction(JdbcUserRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var ac = mockConstruction(JdbcAppointmentRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var dc = mockConstruction(JdbcDoctorRepository.class);
             var rc = mockConstruction(JdbcRoomRepository.class);
             var cc = mockConstruction(JdbcClinicRepository.class);
             var ax = mockConstruction(JdbcAuditEntryRepository.class)) {

            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.of(ds));

            invokeInitializeServices(new MainApp());
            assertThat(ApplicationContext.isUsingDatabase()).isTrue();
        }
    }

    /**
     * {@link MainApp#initializeServices()} inner try/catch: JDBC repository use fails → in-memory fallback, optional DB warning.
     * Uses {@code app.test.jdbcRepositoryWarmup} so failure is a normal {@link RuntimeException} from repository code;
     * throwing from a {@code mockConstruction} initializer surfaces as {@link org.mockito.exceptions.base.MockitoException}
     * ("Could not initialize mocked construction") and does not match production failure modes.
     */
    @Test
    void initializeServices_jdbcUserRepositoryConstructionFails_fallsBackToInMemory() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:postgresql://localhost:5432/appointment");
        applyProps(p);
        System.setProperty("app.test.autoDialogs", "true");
        System.setProperty("app.test.jdbcRepositoryWarmup", "true");

        DataSource ds = mock(DataSource.class);
        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class);
             var uc = mockConstruction(JdbcUserRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenThrow(new RuntimeException("simulated jdbc wiring failure")));
             var ac = mockConstruction(JdbcAppointmentRepository.class,
                     (mock, ctx) -> when(mock.findAll()).thenReturn(Collections.emptyList()));
             var dc = mockConstruction(JdbcDoctorRepository.class);
             var rc = mockConstruction(JdbcRoomRepository.class);
             var cc = mockConstruction(JdbcClinicRepository.class);
             var ax = mockConstruction(JdbcAuditEntryRepository.class)) {

            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.of(ds));

            invokeInitializeServices(new MainApp());
            assertThat(ApplicationContext.isUsingDatabase()).isFalse();
            assertThat(ApplicationContext.getAuthService()).isNotNull();

            CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(done::countDown);
            assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void initializeServices_databaseEnabledButNoDataSource_fallsBackAndSchedulesWarning() throws Exception {
        Properties p = new Properties();
        p.setProperty("database.enabled", "true");
        p.setProperty("database.url", "jdbc:mysql://localhost:3306/x");
        applyProps(p);
        System.setProperty("app.test.autoDialogs", "true");

        try (MockedStatic<DatabaseConfig> dbCfg = mockStatic(DatabaseConfig.class)) {
            dbCfg.when(DatabaseConfig::getDataSource).thenReturn(Optional.empty());

            invokeInitializeServices(new MainApp());
            assertThat(ApplicationContext.isUsingDatabase()).isFalse();
            assertThat(ApplicationContext.getAuthService()).isNotNull();

            CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(done::countDown);
            assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        }
    }
}
