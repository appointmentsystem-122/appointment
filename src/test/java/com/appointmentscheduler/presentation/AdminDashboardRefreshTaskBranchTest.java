package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ReportingService;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Exercises {@link AdminDashboardController#refreshAllData(Runnable)} background {@link javafx.concurrent.Task}
 * anonymous {@code Task} in {@code refreshAllData}, including reporting vs non-reporting branches in {@code succeeded()}.
 */
@ResourceLock("ApplicationContextServices")
class AdminDashboardRefreshTaskBranchTest {

    private static Stage stage;
    private static ScheduleService originalScheduleService;
    private static ReportingService originalReportingService;

    @BeforeAll
    static void startMainApp() {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
        stage = runOnFx(Stage::new);
        MainApp app = new MainApp();
        Throwable err = runOnFx(() -> {
            try {
                app.start(stage);
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        if (err != null) {
            throw new RuntimeException(err);
        }
        originalScheduleService = ApplicationContext.getScheduleService();
        originalReportingService = ApplicationContext.getReportingService();
    }

    @AfterEach
    void restoreContext() {
        ApplicationContext.setScheduleService(originalScheduleService);
        ApplicationContext.setReportingService(originalReportingService);
    }

    @AfterAll
    static void clearAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void refreshTask_succeeded_withReportingService() {
        stubScheduleLoadQuiet();
        ReportingService rs = mock(ReportingService.class);
        when(rs.getTotalAppointmentsCount(anyString())).thenReturn(20L);
        when(rs.getTodayAppointmentsCount(anyString())).thenReturn(4L);
        when(rs.getThisWeekAppointmentsCount(anyString())).thenReturn(11L);
        when(rs.getCancellationRate(anyString())).thenReturn(22.0d);
        when(rs.getPeakBookingHour()).thenReturn(13);
        when(rs.getYesterdayAppointmentsCount(anyString())).thenReturn(1L);
        when(rs.getLastWeekAppointmentsCount(anyString())).thenReturn(8L);
        ApplicationContext.setReportingService(rs);
        Administrator admin = new Administrator("adm-ref-rs", "Admin Ref", "adm-ref-rs@example.com", "pw");
        forceAuth(admin);
        AdminDashboardController c = loadAdmin();
        assertThatCode(() -> {
            seedScheduleForAdminStats();
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForAdminRefreshTask();
        }).doesNotThrowAnyException();
        assertThat(readLabelText(c, "peakHourLabel")).isNotNull();
    }

    @Test
    void refreshTask_succeeded_withoutReportingService_fallsBackToAllAppts() {
        stubScheduleLoadQuiet();
        ApplicationContext.setReportingService(null);
        Administrator admin = new Administrator("adm-ref-nors", "Admin No RS", "adm-ref-nors@example.com", "pw");
        forceAuth(admin);
        AdminDashboardController c = loadAdmin();
        assertThatCode(() -> {
            seedScheduleForAdminStats();
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForAdminRefreshTask();
        }).doesNotThrowAnyException();
        assertThat(readLabelText(c, "peakHourLabel")).isEqualTo("—");
        assertThat(readLabelText(c, "todayTrendLabel")).isEqualTo("");
        assertThat(readLabelText(c, "weekTrendLabel")).isEqualTo("");
    }

    /**
     * After {@code succeeded()} populates tables/lists, a second pass hits many branches that stay
     * cold when {@link PresentationFxControlBranchSweepTest} runs on an empty schedule.
     */
    @Test
    void refreshTask_succeeded_then_sweepFxControls_withLoadedRows() {
        stubScheduleLoadQuiet();
        Administrator admin = new Administrator("adm-ref-sweep", "Admin Sw", "adm-ref-sweep@example.com", "pw");
        forceAuth(admin);
        AdminDashboardController c = loadAdmin();
        assertThatCode(() -> {
            seedScheduleForAdminStats();
            runOnFxVoid(() -> invokeRefreshAllData(c));
            waitForAdminRefreshTask();
            runOnFxVoid(() -> {
                PresentationFxHarness.invokePrivateNoArg(c, "refreshStaffContactInbox");
                PresentationFxHarness.sweepDeclaredFxControls(c);
            });
        }).doesNotThrowAnyException();
    }

    private static void seedScheduleForAdminStats() {
        if (originalScheduleService == null) {
            return;
        }
        var sched = originalScheduleService.getMasterSchedule();
        User p = new User("adm-seed-p", "Seed P", "seedp@example.com", "pw");
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        InPersonAppointment confirmed = new InPersonAppointment(p, new TimeSlot(base, base.plusHours(1)), "R1");
        confirmed.setStatus("CONFIRMED");
        sched.addAppointment(confirmed);

        InPersonAppointment cancelled = new InPersonAppointment(p, new TimeSlot(base.plusHours(2), base.plusHours(3)), "R2");
        cancelled.setStatus("CANCELLED");
        sched.addAppointment(cancelled);

        UrgentAppointment urgent = new UrgentAppointment(p, new TimeSlot(base.plusHours(4), base.plusHours(5)));
        urgent.setStatus("CONFIRMED");
        sched.addAppointment(urgent);
    }

    private void stubScheduleLoadQuiet() {
        if (originalScheduleService == null) {
            return;
        }
        ScheduleService ssSpy = spy(originalScheduleService);
        doNothing().when(ssSpy).loadSchedule();
        ApplicationContext.setScheduleService(ssSpy);
    }

    private static void invokeRefreshAllData(AdminDashboardController c) {
        try {
            Method m = AdminDashboardController.class.getDeclaredMethod("refreshAllData", Runnable.class);
            m.setAccessible(true);
            m.invoke(c, new Object[]{null});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForAdminRefreshTask() {
        try {
            Thread.sleep(900);
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("FX queue did not drain");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void forceAuth(User user) {
        try {
            Object authSvc = ApplicationContext.getAuthService();
            if (authSvc == null) {
                return;
            }
            Field f = authSvc.getClass().getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(authSvc, user);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private static String readLabelText(AdminDashboardController c, String fieldName) {
        try {
            Field f = c.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object obj = f.get(c);
            if (obj instanceof javafx.scene.control.Label lbl) {
                return lbl.getText();
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AdminDashboardController loadAdmin() {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(ScreenConstants.BASE_PATH + ScreenConstants.FXML_ADMIN_DASHBOARD));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
            return loader.getController();
        });
    }

    private static <T> T runOnFx(Callable<T> task) {
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(task.call());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(60, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
        return ref.get();
    }

    private static void runOnFxVoid(Runnable r) {
        runOnFx(() -> {
            r.run();
            return null;
        });
    }
}
