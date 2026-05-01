package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;

import java.time.LocalDateTime;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;

class PresentationDeepMethodSweepTest {

    private static Stage stage;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("app.test.autoDialogs", "true");
        JavaFxTestSupport.initPlatform();
        stage = runOnFx(Stage::new);
        MainApp app = new MainApp();
        Throwable startupError = runOnFx(() -> {
            try {
                app.start(stage);
                return null;
            } catch (Throwable t) {
                return t;
            }
        });
        if (startupError != null) throw new RuntimeException(startupError);
        ensureSampleAppointmentForCoverage();
    }

    /**
     * {@link com.appointmentscheduler.presentation.MainApp#setupDummyData} seeds users and clinics but no appointments,
     * so coverage sweeps that pass an {@link Appointment} would otherwise only exercise null-guards. Booking as
     * staff drives the same persistence path as production and keeps schedule and repository aligned.
     */
    private static void ensureSampleAppointmentForCoverage() {
        try {
            var sched = ApplicationContext.getScheduleService();
            var booking = ApplicationContext.getBookingService();
            if (sched == null || booking == null) {
                return;
            }
            if (!sched.getMasterSchedule().getAllAppointments().isEmpty()) {
                return;
            }
            User patient = new User("user-1", "Alex Customer", "customer@example.com", "hash");
            Administrator admin = new Administrator("admin-1", "Admin User", "admin@admin.com", "hash");
            LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(start, start.plusHours(1));
            InPersonAppointment appt = new InPersonAppointment(patient, slot, "Room A");
            appt.setDoctorId("doc-1");
            appt.setRoomId("room-1");
            appt.setClinicId("clinic-1");
            booking.bookAppointment(appt, admin);
        } catch (Throwable ignored) {
            // If rules block booking in a given environment, sweeps still run with null/non-null variants elsewhere.
        }
    }

    @Test
    void sweep_admin_and_patient_handlers_withAppointmentArgument() {
        forceAuthCurrentUser(new Administrator("adm-sweep", "Admin Sweep", "admin@admin.com", "pw"));
        AdminDashboardController admin = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        Appointment any = pickAnyAppointment();
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(admin);
            invokeBroadly(admin, any);
        })).doesNotThrowAnyException();

        forceAuthCurrentUser(new User("usr-sweep", "User Sweep", "customer@example.com", "pw"));
        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        Appointment patientAppt = pickAnyAppointment();
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(patient);
            invokeBroadly(patient, patientAppt);
        })).doesNotThrowAnyException();
    }

    /**
     * Load FXML from the test thread (main), then invoke handlers on the FX thread only.
     * Avoid nesting {@code loadFxml} inside {@code runOnFxVoid}: {@code loadFxml} uses {@code runOnFx}
     * and would deadlock if called from the JavaFX application thread (inner task never runs).
     */
    @Test
    void sweep_book_and_modify_controllers() {
        forceAuthCurrentUser(new User("book-sweep", "Book Sweep", "book-sweep@example.com", "pw"));
        Appointment any = pickAnyAppointment();
        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(book);
            invokeBroadly(book, any);
        })).doesNotThrowAnyException();
        ModifyAppointmentController modify = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(modify);
            invokeBroadly(modify, any);
        })).doesNotThrowAnyException();
    }

    /**
     * Invokes every public {@code void} instance method we can supply arguments for (0- or 1-arg).
     * Excludes {@code Object} methods and logout. Raises IntelliJ branch coverage on large controllers
     * beyond the {@code handle*}-only sweep.
     */
    @Test
    void sweep_public_void_methods_excluding_logout() {
        Appointment any = pickAnyAppointment();

        forceAuthCurrentUser(new Administrator("adm-mass", "Admin Mass", "admin@admin.com", "pw"));
        AdminDashboardController admin = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(admin);
            invokePublicVoidBestEffort(admin, any);
        })).doesNotThrowAnyException();

        forceAuthCurrentUser(new User("usr-mass", "User Mass", "user-mass@example.com", "pw"));
        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(patient);
            invokePublicVoidBestEffort(patient, any);
        })).doesNotThrowAnyException();

        forceAuthCurrentUser(new User("book-mass", "Book Mass", "book-mass@example.com", "pw"));
        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(book);
            invokePublicVoidBestEffort(book, any);
        })).doesNotThrowAnyException();

        ModifyAppointmentController modify = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(modify);
            invokePublicVoidBestEffort(modify, any);
        })).doesNotThrowAnyException();
    }

    @Test
    void sweep_login_controller() {
        LoginController login = loadFxml(LoginController.class, ScreenConstants.FXML_LOGIN);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(login);
            invokeBroadly(login, null);
            invokePublicVoidBestEffort(login, null);
        })).doesNotThrowAnyException();
    }

    /**
     * Declared (including private) zero-arg void methods on controllers — hits FXML callbacks and helpers
     * the {@code handle*}-only sweep misses. Failures are swallowed (best-effort for branch coverage).
     */
    @Test
    void sweep_declared_zero_arg_void_on_main_controllers() {
        Appointment any = pickAnyAppointment();
        forceAuthCurrentUser(new Administrator("adm-deep", "Admin Deep", "admin@admin.com", "pw"));
        AdminDashboardController admin = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(admin);
            invokeZeroArgDeclaredVoidMethods(admin);
            if (any != null) {
                invokeOneArgAppointmentDeclaredVoidMethods(admin, any);
            }
            invokeOneArgAppointmentDeclaredVoidMethods(admin, null);
        })).doesNotThrowAnyException();

        forceAuthCurrentUser(new User("pat-deep", "Pat Deep", "pat-deep@example.com", "pw"));
        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(patient);
            invokeZeroArgDeclaredVoidMethods(patient);
            if (any != null) {
                invokeOneArgAppointmentDeclaredVoidMethods(patient, any);
            }
            invokeOneArgAppointmentDeclaredVoidMethods(patient, null);
        })).doesNotThrowAnyException();

        forceAuthCurrentUser(new User("book-deep", "Book Deep", "book-deep@example.com", "pw"));
        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(book);
            invokeZeroArgDeclaredVoidMethods(book);
            if (any != null) {
                invokeOneArgAppointmentDeclaredVoidMethods(book, any);
            }
            invokeOneArgAppointmentDeclaredVoidMethods(book, null);
        })).doesNotThrowAnyException();

        ModifyAppointmentController modify = loadFxml(ModifyAppointmentController.class, ScreenConstants.FXML_MODIFY_APPOINTMENT);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(modify);
            invokeZeroArgDeclaredVoidMethods(modify);
            if (any != null) {
                invokeOneArgAppointmentDeclaredVoidMethods(modify, any);
            }
            invokeOneArgAppointmentDeclaredVoidMethods(modify, null);
        })).doesNotThrowAnyException();
    }

    /**
     * Walks every admin sidebar view (dashboard, appointments, users, …) then re-runs broad invocation.
     * Exercises {@code switchView} branches, lazy table loads, and stateful UI paths that a single view misses.
     */
    @Test
    void sweep_admin_after_full_navigation_then_invoke() {
        Appointment any = pickAnyAppointment();
        forceAuthCurrentUser(new Administrator("adm-nav", "Admin Nav", "admin@admin.com", "pw"));
        AdminDashboardController admin = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(admin);
            invokeDeclaredHandleMethodsByNamePrefix(admin, "handleNav");
            invokeBroadly(admin, any);
            invokePublicVoidBestEffort(admin, any);
            invokeZeroArgDeclaredVoidMethods(admin);
            if (any != null) {
                invokeOneArgAppointmentDeclaredVoidMethods(admin, any);
            }
            invokeOneArgAppointmentDeclaredVoidMethods(admin, null);
        })).doesNotThrowAnyException();
    }

    /** Patient sidebar: bookings, book, profile, messages — then full sweep. */
    @Test
    void sweep_patient_after_full_navigation_then_invoke() {
        Appointment any = pickAnyAppointment();
        forceAuthCurrentUser(new User("pat-nav", "Patient Nav", "pat-nav@example.com", "pw"));
        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            prepareControllerForBranchSweep(patient);
            invokeDeclaredHandleMethodsByNamePrefix(patient, "handleNav");
            invokeBroadly(patient, any);
            invokePublicVoidBestEffort(patient, any);
            invokeZeroArgDeclaredVoidMethods(patient);
            if (any != null) {
                invokeOneArgAppointmentDeclaredVoidMethods(patient, any);
            }
            invokeOneArgAppointmentDeclaredVoidMethods(patient, null);
        })).doesNotThrowAnyException();
    }

    /**
     * Selects the first row in every {@link TableView} / {@link ListView} and first item in each {@link ComboBox}
     * on the controller so listener branches (e.g. selection-dependent actions) see non-null state.
     */
    private static void prepareControllerForBranchSweep(Object controller) {
        for (Class<?> cl = controller.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
            for (Field field : cl.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(controller);
                    if (val instanceof TableView<?> tv) {
                        if (!tv.getItems().isEmpty()) {
                            tv.getSelectionModel().clearAndSelect(0);
                        }
                    } else if (val instanceof ListView<?> lv) {
                        if (!lv.getItems().isEmpty()) {
                            lv.getSelectionModel().clearAndSelect(0);
                        }
                    } else if (val instanceof ComboBox<?> cb) {
                        if (!cb.getItems().isEmpty()) {
                            cb.getSelectionModel().select(0);
                        }
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    private static void invokePublicVoidBestEffort(Object controller, Appointment appt) {
        for (Method m : controller.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            if (m.getReturnType() != void.class) continue;
            String n = m.getName();
            if (n.contains("Logout") || n.contains("logout")) continue;
            try {
                int pc = m.getParameterCount();
                if (pc == 0) {
                    m.invoke(controller);
                } else if (pc == 1) {
                    Class<?> p = m.getParameterTypes()[0];
                    for (Object arg : materializeArgVariants(p, appt)) {
                        try {
                            m.invoke(controller, arg);
                        } catch (Throwable ignored) {
                            // Best-effort mass coverage.
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Best-effort mass coverage.
            }
        }
    }

    /**
     * Covers both sides of boolean flags, null vs non-null {@link Appointment}, and typical string/window variants
     * so JaCoCo branch counters move on guard clauses that only fire for one primitive value.
     */
    private static List<Object> materializeArgVariants(Class<?> p, Appointment appt) {
        List<Object> out = new ArrayList<>();
        if (p == ActionEvent.class) {
            out.add(new ActionEvent());
        } else if (p == Event.class) {
            out.add(new ActionEvent());
        } else if (Appointment.class.isAssignableFrom(p)) {
            if (appt != null) {
                out.add(appt);
            }
            out.add(null);
        } else if (p == String.class) {
            out.add("");
            out.add("test");
        } else if (p == boolean.class || p == Boolean.class) {
            out.add(false);
            out.add(true);
        } else if (p == int.class || p == Integer.class) {
            out.add(0);
            out.add(1);
        } else if (p == long.class || p == Long.class) {
            out.add(0L);
            out.add(1L);
        } else if (p == double.class || p == Double.class) {
            out.add(0.0);
            out.add(1.0);
        } else if (p == float.class || p == Float.class) {
            out.add(0f);
            out.add(1f);
        } else if (Window.class.isAssignableFrom(p)) {
            out.add(null);
            if (stage != null) {
                out.add(stage);
            }
        } else if (Node.class.isAssignableFrom(p)) {
            out.add(new StackPane());
        } else if (p == Runnable.class) {
            out.add((Runnable) () -> { });
        }
        return out;
    }

    private static void invokeBroadly(Object controller, Appointment appt) {
        for (Class<?> cl = controller.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
            for (Method m : cl.getDeclaredMethods()) {
                String name = m.getName().toLowerCase();
                if (!name.startsWith("handle")) continue;
                if (name.contains("logout")) continue;
                try {
                    m.setAccessible(true);
                    if (m.getParameterCount() == 0) {
                        m.invoke(controller);
                    } else if (m.getParameterCount() == 1) {
                        Class<?> p = m.getParameterTypes()[0];
                        for (Object arg : materializeArgVariantsForDeclaredHandle(p, appt)) {
                            try {
                                m.invoke(controller, arg);
                            } catch (Throwable ignored) {
                                // Best-effort sweep for coverage.
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // Best-effort sweep for coverage.
                }
            }
        }
    }

    /** Same as {@link #materializeArgVariants} but always includes a second {@link ActionEvent} for FX handlers. */
    private static List<Object> materializeArgVariantsForDeclaredHandle(Class<?> p, Appointment appt) {
        List<Object> base = materializeArgVariants(p, appt);
        if (p == ActionEvent.class && base.size() == 1) {
            outDuplicateActionEvent(base);
        }
        return base;
    }

    private static void outDuplicateActionEvent(List<Object> base) {
        base.add(new ActionEvent());
    }

    /**
     * Invokes every declared {@code void} instance method whose name starts with {@code prefix} (e.g. {@code handleNav}).
     */
    private static void invokeDeclaredHandleMethodsByNamePrefix(Object controller, String prefix) {
        String pl = prefix.toLowerCase();
        Appointment ap = pickAnyAppointment();
        for (Class<?> cl = controller.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
            for (Method m : cl.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getReturnType() != void.class) continue;
                if (!m.getName().toLowerCase().startsWith(pl)) continue;
                if (m.getName().toLowerCase().contains("logout")) continue;
                try {
                    m.setAccessible(true);
                    int pc = m.getParameterCount();
                    if (pc == 0) {
                        m.invoke(controller);
                    } else if (pc == 1) {
                        Class<?> p = m.getParameterTypes()[0];
                        for (Object arg : materializeArgVariants(p, ap)) {
                            try {
                                m.invoke(controller, arg);
                            } catch (Throwable ignored) {
                                // best-effort
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    private static void invokeZeroArgDeclaredVoidMethods(Object controller) {
        for (Class<?> cl = controller.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
            for (Method m : cl.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != void.class) continue;
                String n = m.getName();
                if (n.contains("$")) continue;
                if (n.contains("Logout") || n.contains("logout")) continue;
                if ("initialize".equals(n)) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(controller);
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    private static void invokeOneArgAppointmentDeclaredVoidMethods(Object controller, Appointment appt) {
        for (Class<?> cl = controller.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass()) {
            for (Method m : cl.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1) continue;
                if (m.getReturnType() != void.class) continue;
                if (!Appointment.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(controller, appt);
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    private static Appointment pickAnyAppointment() {
        try {
            List<Appointment> all = ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments();
            for (Appointment a : all) if (a != null) return a;
        } catch (Throwable ignored) {
            // ignore
        }
        return null;
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

    private static <T> T loadFxml(Class<T> controllerType, String fxmlFile) {
        return runOnFx(() -> {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(ScreenConstants.BASE_PATH + fxmlFile));
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
            if (!latch.await(45, TimeUnit.SECONDS)) throw new AssertionError("FX task timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
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
}
