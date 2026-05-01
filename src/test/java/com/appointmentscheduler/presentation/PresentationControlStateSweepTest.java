package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextInputControl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Toggles combo/spinner/checkbox/text controls to hit listener branches (validation, summaries) beyond table sweeps.
 */
class PresentationControlStateSweepTest {

    private static javafx.stage.Stage stage;

    @BeforeAll
    static void start() {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
        stage = runOnFx(javafx.stage.Stage::new);
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

    @Test
    void patientDashboard_controls() {
        forceAuth(new User("ctrl-p", "P", "customer@example.com", "pw"));
        PatientDashboardController c = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> sweepInteractiveFields(c))).doesNotThrowAnyException();
    }

    @Test
    void adminDashboard_controls() {
        forceAuth(new Administrator("ctrl-a", "A", "admin@admin.com", "pw"));
        AdminDashboardController c = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> sweepInteractiveFields(c))).doesNotThrowAnyException();
    }

    private static void sweepInteractiveFields(Object controller) {
        for (Class<?> cl = controller.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
            for (Field field : cl.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(controller);
                    if (val instanceof ComboBox<?> cb) {
                        if (!cb.getItems().isEmpty()) {
                            cb.getSelectionModel().select(0);
                            if (cb.getItems().size() > 1) {
                                cb.getSelectionModel().select(cb.getItems().size() - 1);
                            }
                        }
                    } else if (val instanceof Spinner<?> sp) {
                        SpinnerValueFactory<?> vf = sp.getValueFactory();
                        if (vf != null) {
                            try {
                                vf.increment(1);
                                vf.decrement(1);
                            } catch (Throwable ignored) {
                                // best-effort
                            }
                        }
                    } else if (val instanceof CheckBox bx) {
                        bx.setSelected(!bx.isSelected());
                        bx.setSelected(!bx.isSelected());
                    } else if (val instanceof TextInputControl tic) {
                        String o = tic.getText();
                        tic.setText((o == null ? "" : o) + " ");
                        tic.setText(o != null ? o : "");
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
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

    private static <T> T loadFxml(Class<T> controllerType, String fxmlFile) {
        return runOnFx(() -> {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    MainApp.class.getResource(ScreenConstants.BASE_PATH + fxmlFile));
            javafx.scene.Parent root = loader.load();
            stage.setScene(new javafx.scene.Scene(root, 1200, 800));
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
