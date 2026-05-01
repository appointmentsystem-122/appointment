package com.appointmentscheduler.presentation;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Systematic JaCoCo branch coverage for JavaFX {@code presentation}: reflects over each controller's
 * declared {@link javafx.scene.control.TableView}, {@link javafx.scene.control.ListView},
 * {@link javafx.scene.control.DatePicker}, and {@link javafx.scene.control.ComboBox} fields and
 * drives {@code updateItem} / day cells. Supplements targeted tests in
 * {@link PresentationTableCellFxCoverageTest} with a single exhaustive pass per screen.
 */
@ResourceLock("ApplicationContextServices")
class PresentationFxControlBranchSweepTest {

    private static Stage stage;

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
    }

    @AfterAll
    static void clearDialogsProperty() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void patientDashboard_exhaustiveFxControlSweep() {
        forceAuth(new User("sweep-pat", "Sweep Pat", "sweep-pat@example.com", "pw"));
        PatientDashboardController c = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.invokePrivateNoArg(c, "refreshPatientInbox");
            PresentationFxHarness.sweepDeclaredFxControls(c);
        })).doesNotThrowAnyException();
    }

    @Test
    void adminDashboard_exhaustiveFxControlSweep() {
        forceAuth(new Administrator("sweep-adm", "Sweep Adm", "sweep-adm@example.com", "pw"));
        AdminDashboardController c = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            PresentationFxHarness.invokePrivateNoArg(c, "refreshStaffContactInbox");
            PresentationFxHarness.sweepDeclaredFxControls(c);
        })).doesNotThrowAnyException();
    }

    @Test
    void bookAppointment_exhaustiveFxControlSweep() {
        forceAuth(new User("sweep-book", "Sweep Book", "sweep-book@example.com", "pw"));
        BookAppointmentController c = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c)))
                .doesNotThrowAnyException();
    }

    @Test
    void modifyAppointment_exhaustiveFxControlSweep() {
        forceAuth(new User("sweep-mod", "Sweep Mod", "sweep-mod@example.com", "pw"));
        ModifyAppointmentController c = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c)))
                .doesNotThrowAnyException();
    }

    @Test
    void login_exhaustiveFxControlSweep() {
        LoginController c = loadFxml(LoginController.class, ScreenConstants.FXML_LOGIN);
        assertThatCode(() -> runOnFxVoid(() -> PresentationFxHarness.sweepDeclaredFxControls(c)))
                .doesNotThrowAnyException();
    }

    private static void forceAuth(User user) {
        try {
            Object authSvc = com.appointmentscheduler.application.ApplicationContext.getAuthService();
            if (authSvc == null) {
                return;
            }
            java.lang.reflect.Field f = authSvc.getClass().getDeclaredField("currentUser");
            f.setAccessible(true);
            f.set(authSvc, user);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private static <T> T loadFxml(Class<T> controllerType, String fxmlFile) {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(ScreenConstants.BASE_PATH + fxmlFile));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1200, 800));
            stage.show();
            return controllerType.cast(loader.getController());
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
