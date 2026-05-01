package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import com.appointmentscheduler.application.ApplicationContext;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class PresentationPrintCalendarMainAppTest {

    private static Stage stage;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
        stage = runOnFx(Stage::new);

        MainApp mainApp = new MainApp();
        Throwable startupError = runOnFx(() -> {
            try {
                mainApp.start(stage);
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        if (startupError != null) throw new RuntimeException("MainApp.start failed", startupError);
        forceAuthCurrentUser(new User(
                "forced-user-" + System.nanoTime(),
                "Forced User",
                "forced-user@example.com",
                "pw"
        ));
    }

    @Test
    void coverCalendarMainAppAndPrintHelper() {
        // Calendar: exercise DAILY/WEEKLY/MONTHLY + styleEventBlock branches.
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate firstOfMonth = today.withDayOfMonth(1);

        User patient = new User("p-1", "Patient One", "p1@example.com", "pw");
        List<com.appointmentscheduler.domain.Appointment> appts = new ArrayList<>();
        appts.add(makeAppt("u1", new UrgentAppointment(patient, slot(monday, 9)), "PENDING"));
        appts.add(makeAppt("a1", new AssessmentAppointment(patient, slot(monday.plusDays(1), 10)), "PENDING"));
        appts.add(makeAppt("c1", new IndividualAppointment(patient, slot(monday.plusDays(2), 11)), "CONFIRMED"));
        appts.add(makeAppt("x1", new IndividualAppointment(patient, slot(monday.plusDays(3), 12)), "CANCELLED"));
        appts.add(makeAppt("m1", new IndividualAppointment(patient, slot(firstOfMonth, 9)), "CONFIRMED"));

        runOnFxVoid(() -> {
            new CalendarViewComponent(appts, monday, CalendarViewComponent.ViewMode.WEEKLY);
            new CalendarViewComponent(appts, today, CalendarViewComponent.ViewMode.DAILY);
            new CalendarViewComponent(appts, firstOfMonth, CalendarViewComponent.ViewMode.MONTHLY);
        });

        // MainApp: load a few screens + force error path.
        assertThatCode(() -> runOnFxVoid(() -> {
            forceAuthCurrentUser(new com.appointmentscheduler.domain.Administrator(
                    "a-admin-" + System.nanoTime(),
                    "Admin",
                    "admin@admin.com",
                    "pw"
            ));
            MainApp.loadScreen(ScreenConstants.FXML_ADMIN_DASHBOARD, ScreenConstants.titleAdminDashboard());
            MainApp.loadScreen(ScreenConstants.FXML_PATIENT_DASHBOARD, ScreenConstants.titlePatientDashboard());

            // Invalid fxml => exercises loadScreen error handling + messageOf + showErrorScene.
            MainApp.loadScreen("ThisDoesNotExist.fxml", "Broken");

            // Covers performLogout + logout confirmation flow (autoDialogs => non-blocking).
            com.appointmentscheduler.domain.User currentUser = ApplicationContext.getAuthService().getCurrentUser();
            MainApp.performLogout(stage.getScene().getWindow(), currentUser);
        })).doesNotThrowAnyException();

        // PrintHelper: in autoDialogs mode, should not open OS print dialog.
        com.appointmentscheduler.domain.Appointment apptToPrint = appts.get(2);
        assertThatCode(() -> runOnFxVoid(() -> PrintHelper.printAppointmentReceipt(apptToPrint, stage.getScene().getWindow())))
                .doesNotThrowAnyException();
        sleepQuietly(600);
    }

    private static TimeSlot slot(LocalDate date, int hour) {
        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(hour, 0));
        LocalDateTime end = LocalDateTime.of(date, LocalTime.of(hour, 30));
        return new TimeSlot(start, end);
    }

    private static com.appointmentscheduler.domain.Appointment makeAppt(String id, com.appointmentscheduler.domain.Appointment appt, String status) {
        if (appt != null) appt.setStatus(status);
        return appt;
    }

    private static void forceAuthCurrentUser(User user) {
        try {
            Object authSvc = ApplicationContext.getAuthService();
            if (authSvc == null) return;
            Field f = authSvc.getClass().getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(authSvc, user);
        } catch (Throwable ignored) {
            // ignore
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
            if (!latch.await(15, TimeUnit.SECONDS)) throw new AssertionError("FX task timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (err.get() != null) throw new RuntimeException(err.get());
        return ref.get();
    }

    private static void runOnFxVoid(Runnable r) {
        runOnFx(() -> {
            r.run();
            return null;
        });
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

