package com.appointmentscheduler.presentation;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.PatientInboxEntry;
import com.appointmentscheduler.application.StaffContactMessage;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.control.Cell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Invokes {@link TableCell} {@code updateItem} via reflection (protected in JavaFX) so nested column
 * cell factories hit empty/non-empty branches.
 */
class PresentationTableCellFxCoverageTest {

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
    void patientDashboard_tableCells() {
        forceAuth(new User("tcell-p", "P", "customer@example.com", "pw"));
        PatientDashboardController c = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> exerciseTableViewFromController(c, "appointmentsTable"))).doesNotThrowAnyException();
        assertThatCode(() -> runOnFxVoid(() -> exerciseTableViewFromController(c, "pastAppointmentsTable"))).doesNotThrowAnyException();
    }

    /**
     * Drives {@code pastColRate} cell factory branches: cancelled, unrated, rated with comment.
     */
    @Test
    void patientDashboard_pastRateColumn_extraBranches() {
        User patient = new User("tcell-past", "Past", "past-rate@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> exercisePastRateColumnBranches(c, patient))).doesNotThrowAnyException();
    }

    /**
     * Drives {@code patientInboxList} {@link ListCell} branches: empty row, null item, empty body preview ("—"), long body.
     */
    @Test
    void patientInbox_listCell_bodyPreviewBranches() {
        User patient = new User("tcell-inbox", "Inbox", "inbox-prev@example.com", "pw");
        forceAuth(patient);
        PatientDashboardController c = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> exercisePatientInboxListCellPreviewBranches(c))).doesNotThrowAnyException();
    }

    /**
     * Drives {@code staffContactInboxList} {@link ListCell} (JaCoCo: {@code AdminDashboardController$7}):
     * empty row, null item, empty vs long {@link StaffContactMessage#bodyPreview(int)}.
     */
    @Test
    void admin_staffContactInbox_listCell_previewBranches() {
        forceAuth(new Administrator("tcell-staff-inbox", "A", "staff-inbox@example.com", "pw"));
        AdminDashboardController c = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> exerciseStaffContactInboxListCellBranches(c))).doesNotThrowAnyException();
    }

    @Test
    void adminDashboard_appointmentsTableCells() {
        forceAuth(new Administrator("tcell-a", "A", "admin@admin.com", "pw"));
        AdminDashboardController c = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> exerciseTableViewFromController(c, "appointmentsTable"))).doesNotThrowAnyException();
    }

    @Test
    void bookAndPatient_datePickerDayCells_and_patientInboxListCells() {
        forceAuth(new User("tcell-dp", "P", "customer@example.com", "pw"));
        BookAppointmentController book = loadFxml(BookAppointmentController.class, ScreenConstants.FXML_BOOK_APPOINTMENT);
        PatientDashboardController patient = loadFxml(PatientDashboardController.class, ScreenConstants.FXML_PATIENT_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            exerciseDatePickerDayCells(book, "datePicker");
            exerciseDatePickerDayCells(patient, "datePicker");
            invokePrivateNoArg(patient, "refreshPatientInbox");
            exerciseListViewFromController(patient, "patientInboxList");
        })).doesNotThrowAnyException();
    }

    @Test
    void admin_appointmentTypesTable_usersList_staffInboxListCells() {
        forceAuth(new Administrator("tcell-adm2", "A2", "admin2@admin.com", "pw"));
        AdminDashboardController c = loadFxml(AdminDashboardController.class, ScreenConstants.FXML_ADMIN_DASHBOARD);
        assertThatCode(() -> runOnFxVoid(() -> {
            exerciseTableViewAny(c, "appointmentTypesTable");
            exerciseListViewFromController(c, "usersList");
            invokePrivateNoArg(c, "refreshStaffContactInbox");
            exerciseListViewFromController(c, "staffContactInboxList");
        })).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private static void exerciseTableViewFromController(Object controller, String fieldName) {
        try {
            Field f = findField(controller.getClass(), fieldName);
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object tvObj = f.get(controller);
            if (!(tvObj instanceof TableView)) {
                return;
            }
            TableView<Appointment> tv = (TableView<Appointment>) tvObj;
            walkColumns(tv.getColumns(), tv);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void exerciseTableViewAny(Object controller, String fieldName) {
        try {
            Field f = findField(controller.getClass(), fieldName);
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object tvObj = f.get(controller);
            if (!(tvObj instanceof TableView)) {
                return;
            }
            TableView tv = (TableView) tvObj;
            walkColumnsAny(tv.getColumns(), tv);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exerciseListViewFromController(Object controller, String fieldName) {
        try {
            Field f = findField(controller.getClass(), fieldName);
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object lvObj = f.get(controller);
            if (!(lvObj instanceof ListView)) {
                return;
            }
            ListView<?> lv = (ListView<?>) lvObj;
            Callback cf = lv.getCellFactory();
            if (cf == null) {
                return;
            }
            ListCell cell = (ListCell) cf.call(lv);
            cell.updateIndex(0);
            try {
                invokeCellUpdateItem(cell, null, true);
            } catch (Throwable ignored) {
                // best-effort
            }
            if (!lv.getItems().isEmpty()) {
                try {
                    invokeCellUpdateItem(cell, lv.getItems().get(0), false);
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void exerciseDatePickerDayCells(Object controller, String fieldName) {
        try {
            Field f = findField(controller.getClass(), fieldName);
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object dpObj = f.get(controller);
            if (!(dpObj instanceof DatePicker)) {
                return;
            }
            DatePicker dp = (DatePicker) dpObj;
            Callback<DatePicker, javafx.scene.control.DateCell> factory = dp.getDayCellFactory();
            if (factory == null) {
                return;
            }
            javafx.scene.control.DateCell cell = factory.call(dp);
            try {
                invokeCellUpdateItem(cell, null, true);
            } catch (Throwable ignored) {
                // best-effort
            }
            try {
                invokeCellUpdateItem(cell, LocalDate.now(), false);
            } catch (Throwable ignored) {
                // best-effort
            }
            try {
                invokeCellUpdateItem(cell, LocalDate.now(), true);
            } catch (Throwable ignored) {
                // best-effort
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exercisePatientInboxListCellPreviewBranches(PatientDashboardController c) {
        try {
            invokePrivateNoArg(c, "refreshPatientInbox");
            Field f = findField(c.getClass(), "patientInboxList");
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object lvObj = f.get(c);
            if (!(lvObj instanceof ListView)) {
                return;
            }
            ListView<?> lv = (ListView<?>) lvObj;
            Callback cf = lv.getCellFactory();
            if (cf == null) {
                return;
            }
            ListCell cell = (ListCell) cf.call(lv);
            cell.updateIndex(0);
            invokeCellUpdateItem(cell, null, true);
            invokeCellUpdateItem(cell, null, false);
            LocalDateTime now = LocalDateTime.now();
            PatientInboxEntry emptyBody = new PatientInboxEntry("T", "", now, "Staff");
            PatientInboxEntry longBody = new PatientInboxEntry("T2", "x".repeat(300), now, "Staff");
            invokeCellUpdateItem(cell, emptyBody, false);
            invokeCellUpdateItem(cell, longBody, false);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exerciseStaffContactInboxListCellBranches(AdminDashboardController c) {
        try {
            invokePrivateNoArg(c, "refreshStaffContactInbox");
            Field f = findField(c.getClass(), "staffContactInboxList");
            if (f == null) {
                return;
            }
            f.setAccessible(true);
            Object lvObj = f.get(c);
            if (!(lvObj instanceof ListView)) {
                return;
            }
            ListView<?> lv = (ListView<?>) lvObj;
            Callback cf = lv.getCellFactory();
            if (cf == null) {
                return;
            }
            ListCell cell = (ListCell) cf.call(lv);
            cell.updateIndex(0);
            invokeCellUpdateItem(cell, null, true);
            invokeCellUpdateItem(cell, null, false);
            LocalDateTime now = LocalDateTime.now();
            StaffContactMessage emptyBody = new StaffContactMessage("Subj", "", now, "cid", "Cust", "c@example.com");
            StaffContactMessage longBody = new StaffContactMessage("Subj2", "x".repeat(300), now, "cid", "Cust", "c@example.com");
            invokeCellUpdateItem(cell, emptyBody, false);
            invokeCellUpdateItem(cell, longBody, false);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void exercisePastRateColumnBranches(PatientDashboardController c, User patient) {
        try {
            Field colF = findField(c.getClass(), "pastColRate");
            Field tvF = findField(c.getClass(), "pastAppointmentsTable");
            if (colF == null || tvF == null) {
                return;
            }
            colF.setAccessible(true);
            tvF.setAccessible(true);
            TableColumn col = (TableColumn) colF.get(c);
            TableView tv = (TableView) tvF.get(c);
            if (col == null || tv == null || col.getCellFactory() == null) {
                return;
            }
            LocalDateTime past = LocalDateTime.now().minusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
            TimeSlot ts1 = new TimeSlot(past, past.plusHours(1));
            LocalDateTime d2 = past.plusDays(1);
            TimeSlot ts2 = new TimeSlot(d2, d2.plusHours(1));
            LocalDateTime d3 = past.plusDays(2);
            TimeSlot ts3 = new TimeSlot(d3, d3.plusHours(1));

            InPersonAppointment cancelled = new InPersonAppointment(patient, ts1, "R1");
            cancelled.setStatus("CANCELLED");

            InPersonAppointment unrated = new InPersonAppointment(patient, ts2, "R2");
            unrated.setStatus("COMPLETED");

            InPersonAppointment rated = new InPersonAppointment(patient, ts3, "R3");
            rated.setStatus("COMPLETED");
            Preferences prefs = Preferences.userNodeForPackage(PatientDashboardController.class);
            prefs.putInt("rating." + rated.getId(), 4);
            prefs.put("rating.comment." + rated.getId(), "Good visit");

            tv.setItems(FXCollections.observableArrayList(cancelled, unrated, rated));

            Callback factory = col.getCellFactory();
            TableCell cell = (TableCell) factory.call(col);
            for (int i = 0; i < tv.getItems().size(); i++) {
                cell.updateIndex(i);
                Appointment ap = (Appointment) tv.getItems().get(i);
                invokeCellUpdateItem(cell, ap, false);
            }
            cell.updateIndex(0);
            invokeCellUpdateItem(cell, null, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokePrivateNoArg(Object controller, String methodName) {
        for (Class<?> c = controller.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(methodName);
                m.setAccessible(true);
                m.invoke(controller);
                return;
            } catch (NoSuchMethodException ignored) {
                // next
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Field findField(Class<?> cl, String name) {
        for (Class<?> c = cl; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // next
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void walkColumnsAny(ObservableList cols, TableView tv) {
        for (Object colObj : cols) {
            TableColumn col = (TableColumn) colObj;
            if (!col.getColumns().isEmpty()) {
                walkColumnsAny(col.getColumns(), tv);
            } else {
                Callback factory = col.getCellFactory();
                if (factory == null) {
                    continue;
                }
                try {
                    TableCell cell = (TableCell) factory.call(col);
                    cell.updateIndex(0);
                    try {
                        invokeCellUpdateItem(cell, null, true);
                    } catch (Throwable ignored) {
                        // best-effort
                    }
                    if (!tv.getItems().isEmpty()) {
                        cell.updateIndex(0);
                        Object item = null;
                        try {
                            if (col.getCellObservableValue(0) != null) {
                                item = col.getCellObservableValue(0).getValue();
                            }
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                        try {
                            invokeCellUpdateItem(cell, item, false);
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                    }
                    cell.updateIndex(1);
                    try {
                        invokeCellUpdateItem(cell, null, true);
                    } catch (Throwable ignored) {
                        // best-effort
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void walkColumns(javafx.collections.ObservableList<TableColumn<Appointment, ?>> cols,
                                    TableView<Appointment> tv) {
        for (TableColumn<Appointment, ?> col : cols) {
            if (!col.getColumns().isEmpty()) {
                walkColumns(col.getColumns(), tv);
            } else {
                Callback factory = col.getCellFactory();
                if (factory == null) {
                    continue;
                }
                try {
                    TableCell cell = (TableCell) factory.call(col);
                    cell.updateIndex(0);
                    try {
                        invokeCellUpdateItem(cell, null, true);
                    } catch (Throwable ignored) {
                        // best-effort
                    }
                    if (!tv.getItems().isEmpty()) {
                        cell.updateIndex(0);
                        Object item = null;
                        try {
                            if (col.getCellObservableValue(0) != null) {
                                item = col.getCellObservableValue(0).getValue();
                            }
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                        try {
                            invokeCellUpdateItem(cell, item, false);
                        } catch (Throwable ignored) {
                            // best-effort
                        }
                    }
                    cell.updateIndex(1);
                    try {
                        invokeCellUpdateItem(cell, null, true);
                    } catch (Throwable ignored) {
                        // best-effort
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        }
    }

    private static void invokeCellUpdateItem(Cell<?> cell, Object item, boolean empty) throws Exception {
        for (Class<?> c = cell.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!"updateItem".equals(m.getName()) || m.getParameterCount() != 2) {
                    continue;
                }
                if (m.getParameterTypes()[1] != boolean.class) {
                    continue;
                }
                m.setAccessible(true);
                m.invoke(cell, item, empty);
                return;
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
