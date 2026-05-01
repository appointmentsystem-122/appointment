package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PresentationIntegrationCoverageTest {

    private static Stage stage;
    private static MainApp mainApp;

    @BeforeAll
    static void startApp() throws Exception {
        // Prevent blocking UI dialogs/alerts during automated tests.
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();

        stage = runOnFx(Stage::new);
        mainApp = new MainApp();

        Throwable startupError = runOnFx(() -> {
            try {
                mainApp.start(stage);
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        if (startupError != null) {
            throw new RuntimeException("MainApp.start failed", startupError);
        }
    }

    @Test
    void presentation_integration_loadsCoreScreens_andInvokesMainActions() {
        // 1) Login: open registration dialog (auto-submit)
        LoginController loginController = loadFxml(LoginController.class, ScreenConstants.FXML_LOGIN);
        LoginController lc1 = loginController;
        assertThatCode(() -> runOnFxVoid(lc1::handleOpenRegistration)).doesNotThrowAnyException();

        // 2) Login as admin -> triggers AdminDashboard
        setTextField(loginController, "emailField", "admin@admin.com");
        setTextField(loginController, "passwordField", "admin123");
        LoginController lc2 = loginController;
        assertThatCode(() -> runOnFxVoid(lc2::handleLogin)).doesNotThrowAnyException();
        sleepQuietly(1200);

        AdminDashboardController adminController = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThat(adminController).isNotNull();

        // Cover navigation + report filters + shortcuts (no blocking dialogs).
        assertThatCode(() -> runOnFxVoid(adminController::handleNavAppointments)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleNavReports)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleNavAudit)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleNavSettings)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleShowShortcutsAdmin)).doesNotThrowAnyException();

        // Cover exports that used FileChooser.showSaveDialog.
        assertThatCode(() -> runOnFxVoid(adminController::handleExport)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleExportAudit)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleReportUsers)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(adminController::handleReportCancellations)).doesNotThrowAnyException();

        // Trigger private complete/cancel confirmation flows.
        Appointment anyAppointment = pickAnyNonTerminalAppointment();
        if (anyAppointment != null) {
            assertThatCode(() -> runOnFxVoid(() ->
                invokePrivate(adminController, "handleCompleteAppt", new Class[]{Appointment.class}, anyAppointment)
            )).doesNotThrowAnyException();
            assertThatCode(() -> runOnFxVoid(() ->
                invokePrivate(adminController, "handleCancelAppt", new Class[]{Appointment.class}, anyAppointment)
            )).doesNotThrowAnyException();
        }

        // 3) Login as patient -> triggers PatientDashboard
        loginController = loadFxml(LoginController.class, ScreenConstants.FXML_LOGIN);
        setTextField(loginController, "emailField", "customer@example.com");
        setTextField(loginController, "passwordField", "password123");
        LoginController lc3 = loginController;
        assertThatCode(() -> runOnFxVoid(lc3::handleLogin)).doesNotThrowAnyException();
        sleepQuietly(1200);

        PatientDashboardController patientController = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThat(patientController).isNotNull();

        // Cover patient actions/dialog substitution.
        assertThatCode(() -> runOnFxVoid(patientController::handleChangePassword)).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(patientController::handleToggleTheme)).doesNotThrowAnyException();

        // Trigger private cancel confirmation (needs an appointment owned by the current patient).
        Appointment patientAppt = pickAppointmentForEmail("customer@example.com");
        if (patientAppt != null) {
            assertThatCode(() -> runOnFxVoid(() ->
                invokePrivate(patientController, "handleCancelAppt", new Class[]{Appointment.class}, patientAppt)
            )).doesNotThrowAnyException();
        }

        // 4) Load the main appointment flows (initialize + UI wiring)
        // SessionManager can expire the session during the whole test suite runtime,
        // so force-auth as patient right before loading deeper controllers.
        forceAuthCurrentUserFromAppointment("customer@example.com");
        sleepQuietly(300);

        assertThatCode(() -> loadFxml(Object.class, ScreenConstants.FXML_BOOK_APPOINTMENT)).doesNotThrowAnyException();
        assertThatCode(() -> loadFxml(Object.class, ScreenConstants.FXML_MODIFY_APPOINTMENT)).doesNotThrowAnyException();
    }

    private static Appointment pickAnyNonTerminalAppointment() {
        try {
            List<Appointment> all = ApplicationContext.getScheduleService()
                    .getMasterSchedule()
                    .getAllAppointments();
            for (Appointment a : all) {
                if (a == null || a.getStatus() == null) continue;
                String st = a.getStatus();
                if (!"COMPLETED".equals(st) && !"CANCELLED".equals(st) && !"EXPIRED".equals(st)) return a;
            }
        } catch (Throwable ignored) {
            // ignore; tests will still cover init branches.
        }
        return null;
    }

    private static Appointment pickAppointmentForEmail(String email) {
        if (email == null || email.isBlank()) return null;
        try {
            List<Appointment> all = ApplicationContext.getScheduleService()
                    .getMasterSchedule()
                    .getAllAppointments();
            for (Appointment a : all) {
                if (a == null || a.getPatient() == null) continue;
                if (email.equalsIgnoreCase(a.getPatient().getEmail())) return a;
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return null;
    }

    private static <T> T loadFxml(Class<T> controllerType, String fxmlFile) {
        try {
            return runOnFx(() -> {
                FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(ScreenConstants.BASE_PATH + fxmlFile));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 800);
                stage.setScene(scene);
                stage.show();

                Object controller = loader.getController();
                if (controllerType == Object.class) {
                    return (T) controller;
                }
                return controllerType.cast(controller);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed loading FXML: " + fxmlFile, e);
        }
    }

    private static void setTextField(Object target, String fieldName, String text) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object node = f.get(target);
            if (node instanceof javafx.scene.control.TextField tf) {
                tf.setText(text);
            } else if (node instanceof PasswordField pf) {
                pf.setText(text);
            } else {
                throw new IllegalArgumentException("Field is not a TextField/PasswordField: " + fieldName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed setting text field: " + fieldName, e);
        }
    }

    private static void invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
            m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed invoking private: " + methodName, e);
        }
    }

    private static void forceAuthCurrentUserFromAppointment(String patientEmail) {
        if (patientEmail == null || patientEmail.isBlank()) return;

        try {
            if (ApplicationContext.getScheduleService() != null) {
                ApplicationContext.getScheduleService().loadSchedule();
            }

            Appointment appt = pickAppointmentForEmail(patientEmail);
            if (appt != null && appt.getPatient() != null) {
                Object authSvc = ApplicationContext.getAuthService();
                if (authSvc != null) {
                    Field f = authSvc.getClass().getDeclaredField("currentUser");
                    f.setAccessible(true);
                    f.set(authSvc, appt.getPatient());
                }
                return;
            }
        } catch (Throwable ignored) {
            // fall through
        }

        // Last resort: ensure currentUser is non-null to avoid NPEs in UI initialization.
        try {
            Object authSvc = ApplicationContext.getAuthService();
            if (authSvc != null) {
                var userRepo = ApplicationContext.getAuthService().getUserRepository();
                if (userRepo != null) {
                    var userOpt = userRepo.findByEmail(patientEmail);
                    if (userOpt != null && userOpt.isPresent()) {
                        Field f = authSvc.getClass().getDeclaredField("currentUser");
                        f.setAccessible(true);
                        f.set(authSvc, userOpt.get());
                        return;
                    }
                }

                // If repository has no record (e.g., empty DB), use a stub user.
                com.appointmentscheduler.domain.User stub = new com.appointmentscheduler.domain.User(
                        "forced-user-" + System.nanoTime(),
                        "Forced Patient",
                        patientEmail,
                        "pw"
                );
                Field f = authSvc.getClass().getDeclaredField("currentUser");
                f.setAccessible(true);
                f.set(authSvc, stub);
            }
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T runOnFx(Callable<T> task) throws Exception {
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
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX task timed out");
        }
        if (err.get() != null) throw new RuntimeException(err.get());
        return ref.get();
    }

    private static void runOnFxVoid(Runnable r) throws Exception {
        runOnFx(() -> {
            r.run();
            return null;
        });
    }
}

