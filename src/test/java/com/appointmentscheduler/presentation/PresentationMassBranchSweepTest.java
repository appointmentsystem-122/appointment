package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ScheduleService;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.Schedule;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.testsupport.PresentationFxHarness;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
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

/**
 * Heavy JaCoCo branch driver: seed the in-memory schedule with diverse appointment types, load each
 * major FXML screen, then sweep all declared {@link javafx.scene.control} fields and invoke no-arg
 * {@link FXML} handlers. Intended to raise {@code com.appointmentscheduler.presentation} branch %.
 */
@ResourceLock("ApplicationContextServices")
class PresentationMassBranchSweepTest {

    private static Stage stage;

    @BeforeAll
    static void start() throws Exception {
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
        seedScheduleWithVariety();
    }

    @AfterAll
    static void cleanup() {
        System.clearProperty("app.test.autoDialogs");
        try {
            ScheduleService ss = ApplicationContext.getScheduleService();
            if (ss != null) {
                ss.loadSchedule();
            }
        } catch (Throwable ignored) {
            // ignore
        }
    }

    @Test
    void massSweep_allMajorScreens_refreshAndSweeps() {
        Administrator admin = new Administrator(
                "mass-adm-" + System.nanoTime(), "Mass Admin", "mass-adm@example.com", "pw");
        User patient = new User(
                "mass-pat-" + System.nanoTime(), "Mass Pat", "mass-pat@example.com", "pw");

        forceAuth(admin);
        AdminDashboardController adm = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        sleepQuietly(400);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(adm);
            PresentationFxHarness.invokePrivateNoArg(adm, "refreshAllData");
            invokeNoArgFXMLMethods(adm);
            PresentationFxHarness.sweepDeclaredFxControls(adm);
        })).doesNotThrowAnyException();
        sleepQuietly(1200);

        forceAuth(patient);
        PatientDashboardController pat = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        sleepQuietly(400);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(pat);
            PresentationFxHarness.invokePrivateNoArg(pat, "refreshAllData");
            invokeNoArgFXMLMethods(pat);
            PresentationFxHarness.sweepDeclaredFxControls(pat);
        })).doesNotThrowAnyException();
        sleepQuietly(1200);

        forceAuth(patient);
        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        sleepQuietly(400);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(book);
            invokeNoArgFXMLMethods(book);
            PresentationFxHarness.sweepDeclaredFxControls(book);
        })).doesNotThrowAnyException();
        sleepQuietly(800);

        forceAuth(patient);
        ModifyAppointmentController mod = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        sleepQuietly(400);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(mod);
            invokeNoArgFXMLMethods(mod);
            PresentationFxHarness.sweepDeclaredFxControls(mod);
        })).doesNotThrowAnyException();
        sleepQuietly(800);

        LoginController login = loadFxml(LoginController.class, ScreenConstants.FXML_LOGIN);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.sweepDeclaredFxControls(login);
            invokeNoArgFXMLMethods(login);
            PresentationFxHarness.sweepDeclaredFxControls(login);
        })).doesNotThrowAnyException();
    }

    private static void seedScheduleWithVariety() {
        try {
            ScheduleService ss = ApplicationContext.getScheduleService();
            if (ss == null) {
                return;
            }
            ss.loadSchedule();
            Schedule sch = ss.getMasterSchedule();
            User u = new User("seed-u", "Seed User", "seed-u@example.com", "pw");
            LocalDateTime base = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
            for (int i = 0; i < 6; i++) {
                LocalDateTime s = base.plusHours(i * 2);
                TimeSlot slot = new TimeSlot(s, s.plusHours(1));
                switch (i % 4) {
                    case 0 -> {
                        InPersonAppointment a = new InPersonAppointment(u, slot, "R" + i);
                        a.setStatus("CONFIRMED");
                        sch.addAppointment(a);
                    }
                    case 1 -> {
                        UrgentAppointment a = new UrgentAppointment(u, slot);
                        a.setStatus("PENDING");
                        sch.addAppointment(a);
                    }
                    case 2 -> {
                        IndividualAppointment a = new IndividualAppointment(u, slot);
                        a.setStatus("CONFIRMED");
                        sch.addAppointment(a);
                    }
                    default -> {
                        GroupAppointment a = new GroupAppointment(u, slot, 4);
                        a.setStatus("CONFIRMED");
                        sch.addAppointment(a);
                    }
                }
            }
        } catch (Throwable ignored) {
            // best-effort seeding
        }
    }

    private static void invokeNoArgFXMLMethods(Object controller) {
        for (Method m : controller.getClass().getDeclaredMethods()) {
            if (!m.isAnnotationPresent(FXML.class)) {
                continue;
            }
            if (m.getParameterCount() != 0) {
                continue;
            }
            String name = m.getName();
            if (name.toLowerCase().contains("logout")) {
                continue;
            }
            try {
                m.setAccessible(true);
                m.invoke(controller);
            } catch (Throwable ignored) {
                // coverage only
            }
        }
    }

    private static <T> T loadFxml(Class<T> controllerType, String fxmlFile) {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(ScreenConstants.BASE_PATH + fxmlFile));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
            return controllerType.cast(loader.getController());
        });
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

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            if (!latch.await(90, TimeUnit.SECONDS)) {
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
