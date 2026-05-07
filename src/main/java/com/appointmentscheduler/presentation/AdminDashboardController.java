package com.appointmentscheduler.presentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import com.appointmentscheduler.application.ApplicationContext;
import com.appointmentscheduler.application.ClosedDayBroadcast;
import com.appointmentscheduler.application.DispatchSummary;
import com.appointmentscheduler.application.ExecutiveKpis;
import com.appointmentscheduler.application.InAppMessagingService;
import com.appointmentscheduler.application.StaffContactMessage;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.presentation.notification.NotificationCenter;
import com.appointmentscheduler.presentation.notification.NotificationCenterView;
import com.appointmentscheduler.presentation.notification.NotificationType;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label adminInitialsLabel;
    @FXML private Label dashboardLastUpdatedLabel;
    @FXML private Label adminFooterVersionLabel;
    @FXML private Label appointmentsCountAdminLabel;
    @FXML private Label totalApptsLabel;
    @FXML private Label todayApptsLabel;
    @FXML private Label weekApptsLabel;
    @FXML private Label cancellationRateLabel;
    @FXML private ProgressBar cancellationRateProgressBar;
    @FXML private Label cancellationRateStatusLabel;
    @FXML private Label peakHourLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label cancelledApptsLabel;
    @FXML private Label urgentApptsLabel;
    @FXML private Label todayTrendLabel;
    @FXML private Label weekTrendLabel;
    @FXML private Label adminSessionLabel;

    @FXML private VBox adminAlertsPanel;
    @FXML private HBox adminAlertsContent;
    @FXML private Button btnExportHtmlReport;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> colDate;
    @FXML private TableColumn<Appointment, String> colPatient;
    @FXML private TableColumn<Appointment, String> colType;
    @FXML private TableColumn<Appointment, String> colStatus;
    @FXML private TableColumn<Appointment, String> colRequests;
    @FXML private TableColumn<Appointment, Appointment> colActions;
    @FXML private Pagination appointmentsPagination;
    
    @FXML private ListView<PatronBookingSummary> usersList;
    @FXML private Label messageLabel;

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavAppointments;
    @FXML private Button btnNavUsers;
    @FXML private Button btnNavReports;
    @FXML private Button btnNavSettings;
    @FXML private Button btnNavAudit;
    @FXML private Button btnThemeToggle;
    @FXML private Button btnExportCsv;
    @FXML private Button btnExportAudit;
    @FXML private Button btnRefreshAdmin;
    @FXML private Button btnRefreshAppointments;
    @FXML private Button btnShortcutsAdmin;
    @FXML private Button btnLogout;
    @FXML private Button btnBlockUser;
    @FXML private Button btnDeleteUser;
    @FXML private Button btnChangeRole;

    @FXML private ComboBox<String> clinicSelectorCombo;
    @FXML private HBox executiveKpiBox;
    @FXML private HBox notificationBarPlaceholder;

    @FXML private VBox dashboardView;
    @FXML private VBox appointmentsView;
    @FXML private VBox usersView;
    @FXML private VBox reportsView;
    @FXML private VBox auditView;
    @FXML private VBox settingsView;
    @FXML private VBox appointmentTypesView;
    @FXML private VBox branchesView;
    @FXML private VBox messagingView;
    @FXML private Button btnNavMessaging;
    @FXML private ComboBox<String> adminMessagingAudienceCombo;
    @FXML private TextField adminMessagingSubjectField;
    @FXML private TextArea adminMessagingBodyArea;
    @FXML private Label adminMessagingStatusLabel;
    @FXML private Button btnAdminSendBroadcast;
    @FXML private ListView<StaffContactMessage> staffContactInboxList;

    @FXML private Label reportApptsPerTypeValue;
    @FXML private Label reportCancellationRateLabel;
    @FXML private Label reportPeakHourLabel;
    @FXML private Label reportSummaryLabel;

    // Settings controls (enterprise-level configuration)
    @FXML private ComboBox<String> settingsSystemTypeCombo;
    @FXML private ComboBox<String> settingsTableDensityCombo;
    @FXML private CheckBox settingsShowInactiveCheck;
    @FXML private ComboBox<String> settingsDefaultLandingCombo;
    @FXML private CheckBox settingsInAppToastsCheck;
    @FXML private ComboBox<String> settingsCutoffHoursCombo;
    @FXML private ComboBox<String> settingsTimeFormatCombo;

    @FXML private TableView<AuditEntry> auditTable;
    @FXML private TableColumn<AuditEntry, String> colAuditTime;
    @FXML private TableColumn<AuditEntry, String> colAuditUser;
    @FXML private TableColumn<AuditEntry, String> colAuditAction;
    @FXML private TableColumn<AuditEntry, String> colAuditDetails;

    @FXML private TableView<com.appointmentscheduler.application.AppointmentTypeConfig.Type> appointmentTypesTable;
    @FXML private TableColumn<com.appointmentscheduler.application.AppointmentTypeConfig.Type, String> colTypeName;
    @FXML private TableColumn<com.appointmentscheduler.application.AppointmentTypeConfig.Type, String> colTypeDuration;
    @FXML private TableColumn<com.appointmentscheduler.application.AppointmentTypeConfig.Type, String> colTypeMaxPart;
    @FXML private TableColumn<com.appointmentscheduler.application.AppointmentTypeConfig.Type, com.appointmentscheduler.application.AppointmentTypeConfig.Type> colTypeActions;
    @FXML private TextField appointmentTypeName;
    @FXML private TextField appointmentTypeDuration;
    @FXML private TextField appointmentTypeMaxParticipants;
    @FXML private Button btnAddAppointmentType;
    @FXML private Button btnNavAppointmentTypes;
    @FXML private Button btnNavBranches;
    @FXML private TableView<Clinic> branchesTable;
    @FXML private TableColumn<Clinic, String> colBranchName;
    @FXML private TableColumn<Clinic, String> colBranchAddress;
    @FXML private TableColumn<Clinic, String> colBranchStaff;
    @FXML private TableColumn<Clinic, String> colBranchAppts;

    @FXML private Label appVersionLabel;
    @FXML private Label settingsWorkingHoursLabel;
    @FXML private Label settingsMaxDurationLabel;
    @FXML private Label settingsCutoffInfoLabel;
    @FXML private Label settingsSessionTimeoutLabel;

    @FXML private StackPane contentArea;
    @FXML private VBox calendarContainer;
    @FXML private ComboBox<String> calendarViewModeCombo;
    @FXML private Label calendarRangeLabel;
    @FXML private DatePicker closedDayDatePicker;
    @FXML private Button btnCloseDay;
    @FXML private Button btnReopenDay;
    @FXML private Button btnCalendarPrev;
    @FXML private Button btnCalendarNext;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterStatusCombo;

    private User currentUser;
    private LoadingSpinnerOverlay loadingOverlay;
    private FilteredList<Appointment> filteredAppointments;
    
    private Preferences prefs = Preferences.userNodeForPackage(AdminDashboardController.class);
    private boolean isDarkMode = false;
    private boolean showInactiveAppointments = true;
    private LocalDate calendarAnchorDate = LocalDate.now();

    @FXML
    public void initialize() {
        currentUser = ApplicationContext.getAuthService().getCurrentUser();
        setTimeBasedGreeting();
        setAdminInitials();
        setAdminFooterVersion();
        if (adminMessagingAudienceCombo != null) {
            adminMessagingAudienceCombo.getItems().setAll(
                    "All registered customers",
                    "Customers in current directory (location filter)",
                    "Selected customer (pick one in Booking clients)"
            );
            adminMessagingAudienceCombo.getSelectionModel().selectFirst();
        }
        loadingOverlay = new LoadingSpinnerOverlay();
        // Need to wait until contentArea is added to scene, but for now we'll attach directly in fxml or programmatically
        Platform.runLater(() -> {
            loadingOverlay.attachTo(contentArea);
            if (notificationBarPlaceholder != null) {
                NotificationCenterView.install(NotificationCenter.getInstance(), notificationBarPlaceholder);
            }
            // Set up Theme
            isDarkMode = prefs.getBoolean("dark_mode", false);
            updateThemeUI();
            
            // Keyboard Shortcuts
            if (welcomeLabel.getScene() != null) {
                welcomeLabel.getScene().getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F, javafx.scene.input.KeyCombination.CONTROL_DOWN),
                    () -> {
                        handleNavAppointments();
                        if (searchField != null) searchField.requestFocus();
                    }
                );
                welcomeLabel.getScene().getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Q, javafx.scene.input.KeyCombination.CONTROL_DOWN),
                    this::handleLogout
                );
                welcomeLabel.getScene().getAccelerators().put(
                    new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F5),
                    this::handleRefreshAdmin
                );
            }
            setupTooltips();
            updateSessionLabel();
        });

        setupFilters();
        setupTableColumns();
        setupAuditTable();
        setupSettingsControls();
        setupCalendarAndClosedDay();
        setupAppointmentTypesView();
        setupBranchesView();
        setupClinicSelector();
        if (appVersionLabel != null) appVersionLabel.setText("Version: " + com.appointmentscheduler.application.AppConfig.getAppVersion());
        refreshAllData();
    }

    private void setupTooltips() {
        javafx.scene.control.Tooltip t;
        if (btnNavDashboard != null) { t = new javafx.scene.control.Tooltip("Overview, KPIs, and calendar"); btnNavDashboard.setTooltip(t); }
        if (btnNavAppointments != null) { t = new javafx.scene.control.Tooltip("Manage all appointments (Ctrl+F to search)"); btnNavAppointments.setTooltip(t); }
        if (btnNavUsers != null) { t = new javafx.scene.control.Tooltip("Customers with at least one booking (respects location filter)"); btnNavUsers.setTooltip(t); }
        if (btnNavReports != null) { t = new javafx.scene.control.Tooltip("Reports and analytics"); btnNavReports.setTooltip(t); }
        if (btnNavAudit != null) { t = new javafx.scene.control.Tooltip("Activity audit trail (compliance)"); btnNavAudit.setTooltip(t); }
        if (btnNavSettings != null) { t = new javafx.scene.control.Tooltip("System settings and backup"); btnNavSettings.setTooltip(t); }
        if (clinicSelectorCombo != null) { t = new javafx.scene.control.Tooltip("Filter bookings and directory by site / location"); clinicSelectorCombo.setTooltip(t); }
        if (btnExportCsv != null) { t = new javafx.scene.control.Tooltip("Export appointments to CSV"); btnExportCsv.setTooltip(t); }
        if (btnExportAudit != null) { t = new javafx.scene.control.Tooltip("Export audit log to CSV"); btnExportAudit.setTooltip(t); }
        if (btnRefreshAdmin != null) {
            btnRefreshAdmin.setText(I18n.get("action.refresh"));
            t = new javafx.scene.control.Tooltip(I18n.get("action.refresh.tooltip"));
            btnRefreshAdmin.setTooltip(t);
        }
        if (btnRefreshAppointments != null) {
            btnRefreshAppointments.setText(I18n.get("action.refresh"));
            t = new javafx.scene.control.Tooltip(I18n.get("action.refresh.tooltip"));
            btnRefreshAppointments.setTooltip(t);
        }
        if (btnLogout != null) { t = new javafx.scene.control.Tooltip("Sign out (Ctrl+Q)"); btnLogout.setTooltip(t); }
        if (btnThemeToggle != null) { t = new javafx.scene.control.Tooltip("Switch light/dark theme"); btnThemeToggle.setTooltip(t); }
    }

    private void setupClinicSelector() {
        if (clinicSelectorCombo == null || ApplicationContext.getClinicRepository() == null || ApplicationContext.getCurrentClinicService() == null) return;
        clinicSelectorCombo.getItems().clear();
        clinicSelectorCombo.getItems().add("All sites");
        for (Clinic c : ApplicationContext.getClinicRepository().findAll()) {
            clinicSelectorCombo.getItems().add(c.getName() + " (" + c.getId() + ")");
        }
        String currentId = ApplicationContext.getCurrentClinicService().getCurrentClinicId();
        Optional<Clinic> currentClinic = currentId != null
                ? ApplicationContext.getClinicRepository().findById(currentId)
                : Optional.empty();
        if (currentClinic.isPresent()) {
            Clinic cur = currentClinic.get();
            clinicSelectorCombo.getSelectionModel().select(cur.getName() + " (" + cur.getId() + ")");
        } else {
            clinicSelectorCombo.getSelectionModel().selectFirst();
        }
        clinicSelectorCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || "All sites".equals(newVal)) {
                ApplicationContext.getCurrentClinicService().setCurrentClinicId(null);
            } else {
                int paren = newVal.lastIndexOf('(');
                if (paren >= 0 && newVal.endsWith(")")) {
                    String id = newVal.substring(paren + 1, newVal.length() - 1).trim();
                    ApplicationContext.getCurrentClinicService().setCurrentClinicId(id);
                }
            }
            refreshAllData();
        });
    }

    private void setupAuditTable() {
        if (auditTable == null) return;
        if (colAuditTime != null) colAuditTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTimestampFormatted()));
        if (colAuditUser != null) colAuditUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserName()));
        if (colAuditAction != null) colAuditAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        if (colAuditDetails != null) colAuditDetails.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetails()));
    }

    private void setupCalendarAndClosedDay() {
        if (calendarViewModeCombo != null) {
            calendarViewModeCombo.getItems().setAll("Daily", "Weekly", "Monthly");
            calendarViewModeCombo.getSelectionModel().select("Weekly");
            calendarViewModeCombo.valueProperty().addListener((obs, o, n) -> buildCalendarView());
        }
        if (closedDayDatePicker != null) closedDayDatePicker.setValue(LocalDate.now());
        updateCalendarRangeLabel();
        buildCalendarView();
        Platform.runLater(this::setupClosedDayEnterprisePresentation);
    }

    /** Layout + guaranteed button colors (Modena-safe) for the close-day module. */
    private void setupClosedDayEnterprisePresentation() {
        if (btnCloseDay != null && btnReopenDay != null && btnCloseDay.getParent() instanceof HBox) {
            HBox.setHgrow(btnCloseDay, Priority.ALWAYS);
            HBox.setHgrow(btnReopenDay, Priority.ALWAYS);
        }
        wireClosedDayButtonStyles();
    }

    private void wireClosedDayButtonStyles() {
        if (btnCloseDay == null || btnReopenDay == null) return;
        applyEnterpriseCloseButton(btnCloseDay);
        applyEnterpriseOpenButton(btnReopenDay);
    }

    private static void applyEnterpriseCloseButton(Button b) {
        String normal = "-fx-background-color: #dc2626; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-font-size: 13px; "
            + "-fx-background-radius: 8px; -fx-border-width: 0; -fx-padding: 12 16; -fx-min-height: 44; -fx-background-insets: 0; "
            + "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.35), 8, 0, 0, 2);";
        String hover = normal.replace("#dc2626", "#b91c1c");
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
    }

    private static void applyEnterpriseOpenButton(Button b) {
        String normal = "-fx-background-color: #ffffff; -fx-text-fill: #047857; -fx-font-weight: 700; -fx-font-size: 13px; "
            + "-fx-border-color: #059669; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px; "
            + "-fx-padding: 10 16; -fx-min-height: 44; -fx-background-insets: 0;";
        String hover = normal.replace("#ffffff", "rgba(5,150,105,0.1)");
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
    }

    private void updateCalendarRangeLabel() {
        if (calendarRangeLabel == null) return;
        String mode = calendarViewModeCombo != null ? calendarViewModeCombo.getValue() : "Weekly";
        if ("Daily".equals(mode)) {
            calendarRangeLabel.setText(calendarAnchorDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")));
        } else if ("Monthly".equals(mode)) {
            calendarRangeLabel.setText(calendarAnchorDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        } else {
            java.time.DayOfWeek dow = calendarAnchorDate.getDayOfWeek();
            int offset = dow.getValue() - 1;
            LocalDate mon = calendarAnchorDate.minusDays(offset);
            LocalDate sun = mon.plusDays(6);
            calendarRangeLabel.setText(mon.format(DateTimeFormatter.ofPattern("MMM d")) + " – " + sun.format(DateTimeFormatter.ofPattern("MMM d")));
        }
    }

    private void buildCalendarView() {
        if (calendarContainer == null) return;
        List<Appointment> allAppts;
        if (ApplicationContext.getScheduleService() != null) {
            allAppts = ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments()
                    .stream().filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            allAppts = java.util.Collections.emptyList();
        }
        String mode = calendarViewModeCombo != null ? calendarViewModeCombo.getValue() : "Weekly";
        CalendarViewComponent.ViewMode vmode = "Daily".equals(mode) ? CalendarViewComponent.ViewMode.DAILY
            : "Monthly".equals(mode) ? CalendarViewComponent.ViewMode.MONTHLY : CalendarViewComponent.ViewMode.WEEKLY;
        calendarContainer.getChildren().clear();
        calendarContainer.getChildren().add(new CalendarViewComponent(allAppts, calendarAnchorDate, vmode));
    }

    @FXML
    public void handleCalendarPrev() {
        String mode = calendarViewModeCombo != null ? calendarViewModeCombo.getValue() : "Weekly";
        if ("Daily".equals(mode)) calendarAnchorDate = calendarAnchorDate.minusDays(1);
        else if ("Monthly".equals(mode)) calendarAnchorDate = calendarAnchorDate.minusMonths(1);
        else calendarAnchorDate = calendarAnchorDate.minusWeeks(1);
        updateCalendarRangeLabel();
        buildCalendarView();
    }

    @FXML
    public void handleCalendarNext() {
        String mode = calendarViewModeCombo != null ? calendarViewModeCombo.getValue() : "Weekly";
        if ("Daily".equals(mode)) calendarAnchorDate = calendarAnchorDate.plusDays(1);
        else if ("Monthly".equals(mode)) calendarAnchorDate = calendarAnchorDate.plusMonths(1);
        else calendarAnchorDate = calendarAnchorDate.plusWeeks(1);
        updateCalendarRangeLabel();
        buildCalendarView();
    }

    @FXML
    public void handleCloseDay() {
        if (closedDayDatePicker == null || ApplicationContext.getClosedDayService() == null) return;
        LocalDate d = closedDayDatePicker.getValue();
        if (d == null) return;
        var cds = ApplicationContext.getClosedDayService();
        boolean alreadyClosed = cds.isDayClosed(d);
        cds.addClosedDay(d);
        if (!alreadyClosed) {
            ClosedDayBroadcast.broadcastDayClosed(d);
        }
        if (ApplicationContext.getAuditLogService() != null && ApplicationContext.getAuthService() != null
                && ApplicationContext.getAuthService().getCurrentUser() != null) {
            ApplicationContext.getAuditLogService().log(
                ApplicationContext.getAuthService().getCurrentUser(),
                "DAY_CLOSED",
                "Closed calendar day: " + d.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        String iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        ToastNotification.show(
            welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null,
            alreadyClosed ? NotificationType.INFO : NotificationType.SUCCESS,
            null,
            alreadyClosed
                ? "هذا اليوم مغلق مسبقاً / Already closed: " + iso
                : "تم إغلاق اليوم وإشعار العملاء / Day closed — clients notified: " + iso);
        refreshAllData();
    }

    @FXML
    public void handleReopenDay() {
        if (closedDayDatePicker == null || ApplicationContext.getClosedDayService() == null) return;
        LocalDate d = closedDayDatePicker.getValue();
        if (d == null) return;
        var cds = ApplicationContext.getClosedDayService();
        boolean wasClosed = cds.isDayClosed(d);
        cds.removeClosedDay(d);
        if (wasClosed) {
            ClosedDayBroadcast.broadcastDayReopened(d);
        }
        if (ApplicationContext.getAuditLogService() != null && ApplicationContext.getAuthService() != null
                && ApplicationContext.getAuthService().getCurrentUser() != null) {
            ApplicationContext.getAuditLogService().log(
                ApplicationContext.getAuthService().getCurrentUser(),
                "DAY_REOPENED",
                "Reopened calendar day: " + d.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        String iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        ToastNotification.show(
            welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null,
            wasClosed ? NotificationType.SUCCESS : NotificationType.INFO,
            null,
            wasClosed
                ? "تم فتح اليوم وإشعار العملاء / Day reopened — clients notified: " + iso
                : "هذا اليوم لم يكن مغلقاً / Day was not closed: " + iso);
        refreshAllData();
    }

    private void setupAppointmentTypesView() {
        if (appointmentTypesTable == null) return;
        appointmentTypesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colTypeName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? c.getValue().getName() : ""));
        colTypeDuration.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? String.valueOf(c.getValue().getDurationMinutes()) : ""));
        colTypeMaxPart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? String.valueOf(c.getValue().getMaxParticipants()) : ""));
        colTypeActions.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        colTypeActions.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("حذف");
            { del.getStyleClass().add("button-danger"); del.setOnAction(e -> {
                var t = getItem();
                if (t != null) {
                    com.appointmentscheduler.application.AppointmentTypeConfig.remove(t.getName());
                    loadAppointmentTypesTable();
                }
            }); }
            @Override
            protected void updateItem(com.appointmentscheduler.application.AppointmentTypeConfig.Type item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : del);
            }
        });
    }

    private void loadAppointmentTypesTable() {
        if (appointmentTypesTable == null) return;
        appointmentTypesTable.setItems(FXCollections.observableArrayList(com.appointmentscheduler.application.AppointmentTypeConfig.getAll()));
    }

    @FXML
    public void handleAddAppointmentType() {
        String name = appointmentTypeName != null ? appointmentTypeName.getText() : null;
        if (name == null || name.isBlank()) {
            ToastNotification.show(welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null, NotificationType.WARNING, null, "أدخل اسم النوع");
            return;
        }
        int dur = 60;
        int max = 1;
        try {
            if (appointmentTypeDuration != null && !appointmentTypeDuration.getText().isBlank()) dur = Integer.parseInt(appointmentTypeDuration.getText().trim());
            if (appointmentTypeMaxParticipants != null && !appointmentTypeMaxParticipants.getText().isBlank()) max = Integer.parseInt(appointmentTypeMaxParticipants.getText().trim());
        } catch (NumberFormatException e) {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.WARNING, null, "المدة والحد الأقصى يجب أن يكونا رقماً");
            return;
        }
        com.appointmentscheduler.application.AppointmentTypeConfig.add(new com.appointmentscheduler.application.AppointmentTypeConfig.Type(name, dur, max));
        if (appointmentTypeName != null) appointmentTypeName.clear();
        if (appointmentTypeDuration != null) appointmentTypeDuration.clear();
        if (appointmentTypeMaxParticipants != null) appointmentTypeMaxParticipants.clear();
        loadAppointmentTypesTable();
        ToastNotification.show(welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null, NotificationType.SUCCESS, null, "تمت إضافة النوع");
    }

    private void setupBranchesView() {
        if (branchesTable == null) return;
        colBranchName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? c.getValue().getName() : ""));
        colBranchAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? c.getValue().getAddress() : ""));
        colBranchStaff.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? String.valueOf(countDoctorsForClinic(c.getValue().getId())) : ""));
        colBranchAppts.setCellValueFactory(c -> new SimpleStringProperty(c.getValue() != null ? String.valueOf(countAppointmentsForClinic(c.getValue().getId())) : ""));
    }

    private int countDoctorsForClinic(String clinicId) {
        if (ApplicationContext.getDoctorRepository() == null) return 0;
        return (int) ApplicationContext.getDoctorRepository().findAll().stream().filter(d -> clinicId != null && clinicId.equals(d.getClinicId())).count();
    }

    private long countAppointmentsForClinic(String clinicId) {
        if (ApplicationContext.getScheduleService() == null) return 0;
        return ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments().stream()
                .filter(Objects::nonNull)
                .filter(a -> !a.isDeleted() && clinicId != null && clinicId.equals(a.getClinicId())).count();
    }

    private void loadBranchesTable() {
        if (branchesTable == null || ApplicationContext.getClinicRepository() == null) return;
        branchesTable.setItems(FXCollections.observableArrayList(ApplicationContext.getClinicRepository().findAll()));
    }
    
    private void setupFilters() {
        if (filterTypeCombo != null) {
            filterTypeCombo.getItems().addAll("All Types", "Assessment", "FollowUp", "Urgent", "Virtual", "InPerson", "Group", "Individual");
            filterTypeCombo.getSelectionModel().selectFirst();
        }
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().addAll("All Statuses", "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED");
            filterStatusCombo.getSelectionModel().selectFirst();
        }
        
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (filterTypeCombo != null) {
            filterTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (filterStatusCombo != null) {
            filterStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }
    
    private void applyFilters() {
        if (filteredAppointments == null) return;
        
        filteredAppointments.setPredicate(appt -> {
            // 1. Search Text
            String search = searchField != null ? searchField.getText().toLowerCase() : "";
            boolean matchesSearch = search.isEmpty() 
                                    || appt.getPatient().getName().toLowerCase().contains(search) 
                                    || appt.getTimeSlot().toString().toLowerCase().contains(search);
            
            // 2. Type Filter
            String type = filterTypeCombo != null ? filterTypeCombo.getValue() : "All Types";
            boolean matchesType = "All Types".equals(type) 
                                  || appt.getClass().getSimpleName().contains(type);
                                  
            // 3. Status Filter
            String status = filterStatusCombo != null ? filterStatusCombo.getValue() : "All Statuses";
            boolean matchesStatus = "All Statuses".equals(status) 
                                    || appt.getStatus().equalsIgnoreCase(status);

            boolean matches = matchesSearch && matchesType && matchesStatus;

            if (!showInactiveAppointments) {
                String s = appt.getStatus();
                if ("CANCELLED".equalsIgnoreCase(s) || "EXPIRED".equalsIgnoreCase(s)) {
                    return false;
                }
            }
            
            return matches;
        });
    }

    private void setupSettingsControls() {
        // System / Business type (saved in Preferences; used across the app)
        if (settingsSystemTypeCombo != null) {
            settingsSystemTypeCombo.getItems().setAll(com.appointmentscheduler.application.AppConfig.getSystemTypeOptions());
            String current = com.appointmentscheduler.application.AppConfig.getSystemType();
            if (!settingsSystemTypeCombo.getItems().contains(current)) {
                settingsSystemTypeCombo.getItems().add(0, current);
            }
            settingsSystemTypeCombo.getSelectionModel().select(current);
            settingsSystemTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    com.appointmentscheduler.application.AppConfig.setSystemType(newVal);
                    if (MainApp.getPrimaryStage() != null) {
                        MainApp.getPrimaryStage().setTitle(com.appointmentscheduler.application.AppConfig.getAppName() + " — " + newVal);
                    }
                }
            });
        }

        // Business rules (read-only from config)
        if (settingsWorkingHoursLabel != null) {
            int start = com.appointmentscheduler.application.AppConfig.getBusinessHourStart();
            int end = com.appointmentscheduler.application.AppConfig.getBusinessHourEnd();
            settingsWorkingHoursLabel.setText("Working hours: " + start + ":00 – " + end + ":00 (24h)");
        }
        if (settingsMaxDurationLabel != null) {
            settingsMaxDurationLabel.setText("Max appointment duration: " + com.appointmentscheduler.application.AppConfig.getBookingMaxDurationMinutes() + " minutes");
        }
        if (settingsCutoffInfoLabel != null) {
            settingsCutoffInfoLabel.setText("Booking cutoff (min. hours before): " + com.appointmentscheduler.application.AppConfig.getBookingCutoffHoursBefore() + " hours");
        }
        if (settingsSessionTimeoutLabel != null) {
            settingsSessionTimeoutLabel.setText("Session timeout: " + com.appointmentscheduler.application.AppConfig.getSessionTimeoutMinutes() + " minutes");
        }

        // Table density
        if (settingsTableDensityCombo != null) {
            settingsTableDensityCombo.getItems().setAll("Comfortable", "Compact");
            String density = prefs.get("settings.tableDensity", "Comfortable");
            if (!settingsTableDensityCombo.getItems().contains(density)) {
                density = "Comfortable";
            }
            settingsTableDensityCombo.getSelectionModel().select(density);
            applyTableDensity(density);
            settingsTableDensityCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    prefs.put("settings.tableDensity", newVal);
                    applyTableDensity(newVal);
                }
            });
        }

        // Show cancelled / expired
        if (settingsShowInactiveCheck != null) {
            boolean showInactive = prefs.getBoolean("settings.showInactive", true);
            settingsShowInactiveCheck.setSelected(showInactive);
            showInactiveAppointments = showInactive;
            settingsShowInactiveCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
                showInactiveAppointments = newVal;
                prefs.putBoolean("settings.showInactive", newVal);
                applyFilters();
            });
        }

        // Default landing view
        if (settingsDefaultLandingCombo != null) {
            settingsDefaultLandingCombo.getItems().setAll("Dashboard", "Appointments", "Users", "Reports", "Audit", "Settings");
            String def = prefs.get("settings.defaultLanding", "Dashboard");
            if (!settingsDefaultLandingCombo.getItems().contains(def)) {
                def = "Dashboard";
            }
            settingsDefaultLandingCombo.getSelectionModel().select(def);
            applyDefaultLanding(def);
            settingsDefaultLandingCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    prefs.put("settings.defaultLanding", newVal);
                    applyDefaultLanding(newVal);
                }
            });
        }

        // In-app toasts toggle
        if (settingsInAppToastsCheck != null) {
            boolean enabled = prefs.getBoolean("settings.inAppToasts", true);
            settingsInAppToastsCheck.setSelected(enabled);
            settingsInAppToastsCheck.selectedProperty().addListener((obs, oldVal, newVal) ->
                    prefs.putBoolean("settings.inAppToasts", newVal));
        }

        // Booking cutoff hours
        if (settingsCutoffHoursCombo != null) {
            settingsCutoffHoursCombo.getItems().setAll("1", "2", "3", "4", "6", "12", "24");
            String cutoff = Integer.toString(com.appointmentscheduler.application.AppConfig.getInt("booking.cutoffHoursBefore", 2));
            if (!settingsCutoffHoursCombo.getItems().contains(cutoff)) {
                cutoff = "2";
            }
            settingsCutoffHoursCombo.getSelectionModel().select(cutoff);
            settingsCutoffHoursCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    prefs.put("settings.cutoffHoursBefore", newVal);
                }
            });
        }

        // Time format preference
        if (settingsTimeFormatCombo != null) {
            settingsTimeFormatCombo.getItems().setAll("24-hour", "12-hour");
            String tf = prefs.get("settings.timeFormat", "24-hour");
            if (!settingsTimeFormatCombo.getItems().contains(tf)) {
                tf = "24-hour";
            }
            settingsTimeFormatCombo.getSelectionModel().select(tf);
            settingsTimeFormatCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    prefs.put("settings.timeFormat", newVal);
                }
            });
        }
    }

    private void applyTableDensity(String density) {
        double size = "Compact".equalsIgnoreCase(density) ? 26.0 : 36.0;
        if (appointmentsTable != null) {
            appointmentsTable.setFixedCellSize(size);
        }
        if (auditTable != null) {
            auditTable.setFixedCellSize(size);
        }
    }

    private void applyDefaultLanding(String view) {
        if (view == null) return;
        switch (view) {
            case "Appointments":
                handleNavAppointments();
                break;
            case "Users":
                handleNavUsers();
                break;
            case "Reports":
                handleNavReports();
                break;
            case "Audit":
                handleNavAudit();
                break;
            case "Settings":
                handleNavSettings();
                break;
            default:
                handleNavDashboard();
        }
    }

    private static String summarizeBookingRequests(Appointment a) {
        if (a == null) return "";
        StringBuilder sb = new StringBuilder();
        if (a.getCustomerNotes() != null && !a.getCustomerNotes().isBlank()) {
            sb.append(a.getCustomerNotes().trim());
        }
        if (a.getContactPhone() != null && !a.getContactPhone().isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("☎ ").append(a.getContactPhone().trim());
        }
        if (a.getReminderChannel() != null && !a.getReminderChannel().isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(BookingExtrasUi.reminderChannelLabel(a.getReminderChannel()));
        }
        if (a.getPreferredLanguage() != null && !a.getPreferredLanguage().isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(BookingExtrasUi.preferredLanguageLabel(a.getPreferredLanguage()));
        }
        if (a.getAccessibilityNeeds() != null && !a.getAccessibilityNeeds().isBlank()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(a.getAccessibilityNeeds().trim());
        }
        if (a.getParticipantCount() > 1) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("Party: ").append(a.getParticipantCount());
        }
        String s = sb.toString().trim();
        if (s.length() <= 160) return s;
        return s.substring(0, 157) + "…";
    }

    private void setupTableColumns() {
        if (appointmentsTable == null) return;
        appointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimeSlot().toString()));
        colPatient.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPatient().getName()));
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));
        
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(status);
                    lbl.getStyleClass().add("badge");
                    if (status.equals("CONFIRMED")) lbl.getStyleClass().add("badge-success");
                    else if (status.equals("CANCELLED")) lbl.getStyleClass().add("badge-danger");
                    else if (status.equals("EXPIRED") || status.equals("COMPLETED")) lbl.getStyleClass().add("badge-muted");
                    else lbl.getStyleClass().add("badge-warning");
                    setGraphic(lbl);
                }
            }
        });

        if (colRequests != null) {
            colRequests.setCellValueFactory(c -> new SimpleStringProperty(summarizeBookingRequests(c.getValue())));
            colRequests.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String text, boolean empty) {
                    super.updateItem(text, empty);
                    if (empty || text == null || text.isBlank()) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        setText(null);
                        Label lbl = new Label(text);
                        lbl.setWrapText(true);
                        lbl.setMaxWidth(280);
                        lbl.getStyleClass().add("admin-requests-cell");
                        setGraphic(lbl);
                    }
                }
            });
        }

        colActions.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button completeBtn = new Button();
            private final Button cancelBtn = new Button();
            private final HBox actionBox = new HBox(8);

            {
                SVGPath checkIcon = new SVGPath();
                checkIcon.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
                checkIcon.setStyle("-fx-fill: white;");
                completeBtn.setGraphic(checkIcon);
                completeBtn.getStyleClass().add("button-complete-appt");
                completeBtn.setStyle("-fx-padding: 5px; -fx-min-width: 32px; -fx-min-height: 32px;");
                completeBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("admin.appointment.complete.tooltip")));

                SVGPath cancelIcon = new SVGPath();
                cancelIcon.setContent("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
                cancelIcon.setStyle("-fx-fill: white;");
                cancelBtn.setGraphic(cancelIcon);
                cancelBtn.getStyleClass().add("button-danger");
                cancelBtn.setStyle("-fx-padding: 5px; -fx-min-width: 32px; -fx-min-height: 32px;");
                cancelBtn.setTooltip(new javafx.scene.control.Tooltip("Force Cancel Appointment"));

                actionBox.setAlignment(Pos.CENTER_LEFT);
                actionBox.getChildren().addAll(completeBtn, cancelBtn);

                completeBtn.setOnAction(event -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    handleCompleteAppt(appt);
                });
                cancelBtn.setOnAction(event -> {
                    Appointment appt = getTableView().getItems().get(getIndex());
                    handleCancelAppt(appt);
                });
            }

            @Override
            protected void updateItem(Appointment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                String st = item.getStatus();
                boolean terminal = "COMPLETED".equals(st) || "CANCELLED".equals(st) || "EXPIRED".equals(st);
                if (terminal) {
                    setGraphic(null);
                    return;
                }
                boolean canComplete = "CONFIRMED".equals(st) || "PENDING".equals(st);
                completeBtn.setVisible(canComplete);
                completeBtn.setManaged(canComplete);
                setGraphic(actionBox);
            }
        });

        
        if (usersList != null) {
            Label emptyPatrons = new Label("لا يوجد عملاء بحجوزات ضمن الفلتر الحالي.\nNo customers with bookings for the current location filter.");
            emptyPatrons.getStyleClass().add("empty-state-label");
            emptyPatrons.setWrapText(true);
            usersList.setPlaceholder(emptyPatrons);

            usersList.setCellFactory(param -> new ListCell<PatronBookingSummary>() {
                @Override
                protected void updateItem(PatronBookingSummary item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        User u = item.getUser();
                        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(6);
                        card.setStyle("-fx-padding: 12px 14px;");

                        javafx.scene.layout.HBox top = new javafx.scene.layout.HBox(15);
                        Label nameLbl = new Label(u.getName() + "  ·  " + u.getEmail());
                        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");
                        Label typeLbl = new Label(u instanceof Administrator ? "ADMIN" : "CUSTOMER");
                        typeLbl.getStyleClass().add("badge");
                        typeLbl.getStyleClass().add(u instanceof Administrator ? "badge-warning" : "badge-category");

                        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                        top.getChildren().addAll(nameLbl, spacer, typeLbl);

                        Label ar = new Label(item.arabicStatsLine());
                        ar.setWrapText(true);
                        ar.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-secondary;");
                        Label en = new Label(item.englishStatsLine());
                        en.setWrapText(true);
                        en.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-secondary;");

                        card.getChildren().addAll(top, ar, en);
                        setGraphic(card);
                    }
                }
            });
        }
    }

    private void refreshAllData() {
        refreshAllData(null);
    }

    private void refreshAllData(Runnable onComplete) {
        if (loadingOverlay != null) loadingOverlay.show();
        
        // Simulate background loading to show spinner
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ApplicationContext.getScheduleService().loadSchedule();
                Thread.sleep(200);
                return null;
            }
            @Override
            protected void succeeded() {
                List<Appointment> allAppts = ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments()
                        .stream().filter(Objects::nonNull).collect(Collectors.toList());
                List<User> allUsers = ApplicationContext.getAuthService().getUserRepository().getAllUsers();

                String clinicId = ApplicationContext.getCurrentClinicService() != null ? ApplicationContext.getCurrentClinicService().getCurrentClinicId() : null;
                // Dashboard Stats (real analytics from ReportingService when available)
                if (ApplicationContext.getReportingService() != null) {
                    var rs = ApplicationContext.getReportingService();
                    long todayCount = rs.getTodayAppointmentsCount(clinicId);
                    long weekCount = rs.getThisWeekAppointmentsCount(clinicId);
                    if (totalApptsLabel != null) totalApptsLabel.setText(String.valueOf(rs.getTotalAppointmentsCount(clinicId)));
                    if (todayApptsLabel != null) todayApptsLabel.setText(String.valueOf(todayCount));
                    if (weekApptsLabel != null) weekApptsLabel.setText(String.valueOf(weekCount));
                    double cancelRate = rs.getCancellationRate(clinicId);
                    applyCancellationRateKpi(cancelRate);
                    int peakHour = rs.getPeakBookingHour();
                    if (peakHourLabel != null) peakHourLabel.setText(String.format("%02d:00", peakHour));
                    // Trend vs last period (enterprise KPI)
                    if (todayTrendLabel != null) {
                        long yesterday = rs.getYesterdayAppointmentsCount(clinicId);
                        todayTrendLabel.setText(formatTrend(yesterday, todayCount, "yesterday"));
                    }
                    if (weekTrendLabel != null) {
                        long lastWeek = rs.getLastWeekAppointmentsCount(clinicId);
                        weekTrendLabel.setText(formatTrend(lastWeek, weekCount, "last week"));
                    }
                    Platform.runLater(() -> updateAlertsPanel(clinicId, todayCount, weekCount, cancelRate));
                } else {
                    if (totalApptsLabel != null) totalApptsLabel.setText(String.valueOf(allAppts.size()));
                    LocalDate today = LocalDate.now();
                    long todayCount = allAppts.stream()
                            .filter(a -> a.getTimeSlot().getStartTime().toLocalDate().equals(today))
                            .count();
                    if (todayApptsLabel != null) todayApptsLabel.setText(String.valueOf(todayCount));
                    if (weekApptsLabel != null) {
                        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L);
                        LocalDate endOfWeek = startOfWeek.plusDays(6);
                        long weekCount = allAppts.stream()
                                .filter(a -> !a.getTimeSlot().getStartTime().toLocalDate().isBefore(startOfWeek)
                                        && !a.getTimeSlot().getStartTime().toLocalDate().isAfter(endOfWeek))
                                .count();
                        weekApptsLabel.setText(String.valueOf(weekCount));
                    }
                    {
                        long cancelled = allAppts.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count();
                        double rate = allAppts.isEmpty() ? 0 : 100.0 * cancelled / allAppts.size();
                        applyCancellationRateKpi(rate);
                    }
                    if (peakHourLabel != null) peakHourLabel.setText("—");
                    if (todayTrendLabel != null) todayTrendLabel.setText("");
                    if (weekTrendLabel != null) weekTrendLabel.setText("");
                    Platform.runLater(() -> updateAlertsPanel(null, 0, 0, 0.0));
                }
                Map<String, User> usersById = allUsers.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
                List<PatronBookingSummary> patrons = PatronBookingSummary.build(allAppts, usersById, clinicId, LocalDateTime.now());
                if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(patrons.size()));
                
                long cancelledCount = allAppts.stream()
                        .filter(a -> "CANCELLED".equals(a.getStatus()))
                        .count();
                if (cancelledApptsLabel != null) {
                    cancelledApptsLabel.setText(String.valueOf(cancelledCount));
                    cancelledApptsLabel.setTextFill(cancelledCount == 0 ? Color.web("#15803d") : Color.web("#b91c1c"));
                }
                
                long urgentCount = allAppts.stream()
                        .filter(a -> a instanceof UrgentAppointment)
                        .count();
                if (urgentApptsLabel != null) urgentApptsLabel.setText(String.valueOf(urgentCount));
                
                // Lists
                if (appointmentsTable != null) {
                    ObservableList<Appointment> obsAppts = FXCollections.observableArrayList(allAppts);
                    filteredAppointments = new FilteredList<>(obsAppts, p -> true);
                    appointmentsTable.setItems(filteredAppointments);
                    applyFilters(); // Re-apply if any exist
                }
                
                if (usersList != null) {
                    usersList.setItems(FXCollections.observableArrayList(patrons));
                }
                
                // Calendar Render
                if (calendarContainer != null) buildCalendarView();

                // Executive KPIs (with thresholds) — same visual language as Key Metrics strip
                if (executiveKpiBox != null && ApplicationContext.getReportingService() != null && dashboardView != null && dashboardView.isVisible()) {
                    executiveKpiBox.getChildren().clear();
                    List<ExecutiveKpis.KpiRow> kpis = ExecutiveKpis.build(ApplicationContext.getReportingService(), clinicId);
                    for (int i = 0; i < kpis.size(); i++) {
                        ExecutiveKpis.KpiRow row = kpis.get(i);
                        VBox card = new VBox(8);
                        card.getStyleClass().add("executive-kpi-card");
                        if (i < kpis.size() - 1) {
                            card.getStyleClass().add("enterprise-executive-cell-divider");
                        }
                        Label lbl = new Label(row.label);
                        lbl.getStyleClass().add("enterprise-metric-caption");
                        lbl.setWrapText(true);
                        Label val = new Label(row.value);
                        val.getStyleClass().add("enterprise-metric-value");
                        if (row.status == ExecutiveKpis.Status.CRITICAL) {
                            val.getStyleClass().add("enterprise-metric-value-critical");
                        } else if (row.status == ExecutiveKpis.Status.WARNING) {
                            val.getStyleClass().add("enterprise-metric-value-warning");
                        }
                        if (row.thresholdInfo != null && !row.thresholdInfo.isEmpty()) {
                            Label th = new Label(row.thresholdInfo);
                            th.getStyleClass().add("enterprise-metric-footnote");
                            th.setWrapText(true);
                            card.getChildren().addAll(lbl, val, th);
                        } else {
                            card.getChildren().addAll(lbl, val);
                        }
                        HBox.setHgrow(card, Priority.ALWAYS);
                        card.setMinWidth(168);
                        executiveKpiBox.getChildren().add(card);
                    }
                }
                
                updateDashboardLastUpdated();
                updateAdminAppointmentsCount();
                updateSessionLabel();
                if (loadingOverlay != null) loadingOverlay.hide();
                if (onComplete != null) Platform.runLater(onComplete);
            }
        };
        new Thread(loadTask).start();
    }

    @FXML
    public void handleShowShortcutsAdmin() {
        javafx.stage.Window w = welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null;
        DialogHelper.showKeyboardShortcutsAdmin(w);
    }

    private void setTimeBasedGreeting() {
        if (welcomeLabel == null || currentUser == null) return;
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? I18n.get("greeting.morning") : hour < 17 ? I18n.get("greeting.afternoon") : I18n.get("greeting.evening");
        welcomeLabel.setText(greeting + ", " + currentUser.getName());
    }

    private void setAdminInitials() {
        if (adminInitialsLabel == null || currentUser == null) return;
        String name = currentUser.getName().trim();
        if (name.isEmpty()) {
            adminInitialsLabel.setText("A");
            return;
        }
        String[] parts = name.split("\\s+", 2);
        String initials;
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            initials = (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase();
        } else {
            initials = (name.length() >= 2 ? name.substring(0, 2) : name).toUpperCase();
        }
        adminInitialsLabel.setText(initials);
    }

    private void setAdminFooterVersion() {
        if (adminFooterVersionLabel != null) {
            adminFooterVersionLabel.setText(I18n.get("footer.version") + " " + com.appointmentscheduler.application.AppConfig.getAppVersion());
        }
    }

    private void updateDashboardLastUpdated() {
        if (dashboardLastUpdatedLabel != null) {
            dashboardLastUpdatedLabel.setText(I18n.get("last.updated") + ": " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    private void updateAdminAppointmentsCount() {
        if (appointmentsCountAdminLabel != null && appointmentsTable != null) {
            int n = appointmentsTable.getItems().size();
            appointmentsCountAdminLabel.setText(I18n.get("showing.count", String.valueOf(n)));
        }
    }

    private static final String[] CANCELLATION_RATE_TIER_CLASSES = {
        "cancellation-rate-tier-excellent",
        "cancellation-rate-tier-good",
        "cancellation-rate-tier-moderate",
        "cancellation-rate-tier-warning",
        "cancellation-rate-tier-critical"
    };

    /**
     * Updates cancellation rate KPI: percentage, tier-colored value, slim progress bar, and bilingual status.
     * Tier thresholds align with the admin alerts panel (15% / 20%).
     */
    private void applyCancellationRateKpi(double rate) {
        double r = rate;
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            r = 0.0;
        }
        r = Math.max(0.0, Math.min(100.0, r));

        if (cancellationRateProgressBar != null) {
            cancellationRateProgressBar.setProgress(Math.min(1.0, r / 100.0));
        }
        for (String s : CANCELLATION_RATE_TIER_CLASSES) {
            if (cancellationRateLabel != null) {
                cancellationRateLabel.getStyleClass().remove(s);
            }
            if (cancellationRateProgressBar != null) {
                cancellationRateProgressBar.getStyleClass().remove(s);
            }
            if (cancellationRateStatusLabel != null) {
                cancellationRateStatusLabel.getStyleClass().remove(s);
            }
        }

        String tier;
        String statusArEn;
        if (r < 5.0) {
            tier = "cancellation-rate-tier-excellent";
            statusArEn = "ممتاز · Excellent";
        } else if (r < 10.0) {
            tier = "cancellation-rate-tier-good";
            statusArEn = "جيد · Good";
        } else if (r < 15.0) {
            tier = "cancellation-rate-tier-moderate";
            statusArEn = "متوسط · Moderate";
        } else if (r < 20.0) {
            tier = "cancellation-rate-tier-warning";
            statusArEn = "يحتاج متابعة · Needs attention";
        } else {
            tier = "cancellation-rate-tier-critical";
            statusArEn = "مرتفع · High risk";
        }

        if (cancellationRateLabel != null) {
            cancellationRateLabel.setText(String.format("%.1f%%", r));
            cancellationRateLabel.getStyleClass().add(tier);
        }
        if (cancellationRateProgressBar != null) {
            cancellationRateProgressBar.getStyleClass().add(tier);
        }
        if (cancellationRateStatusLabel != null) {
            cancellationRateStatusLabel.setText(statusArEn);
            cancellationRateStatusLabel.getStyleClass().add(tier);
        }
    }

    private static String formatTrend(long previous, long current, String periodLabel) {
        if (previous == 0 && current == 0) return "";
        if (previous == 0) return "↑ vs " + periodLabel;
        double pct = 100.0 * (current - previous) / previous;
        if (Math.abs(pct) < 0.5) return "→ vs " + periodLabel;
        return (pct > 0 ? "↑ " : "↓ ") + String.format("%.0f%%", Math.abs(pct)) + " vs " + periodLabel;
    }

    private void updateAlertsPanel(String clinicId, long todayCount, long weekCount, double cancelRate) {
        if (adminAlertsPanel == null || adminAlertsContent == null) return;
        adminAlertsContent.getChildren().clear();
        java.util.List<String> alerts = new java.util.ArrayList<>();
        if (cancelRate >= 20.0) alerts.add("نسبة الإلغاء مرتفعة / High cancellation rate (" + String.format("%.1f%%", cancelRate) + "). Consider follow-up or reminders.");
        if (cancelRate >= 15.0 && cancelRate < 20.0) alerts.add("نسبة الإلغاء أعلى من المعتاد / Cancellation rate above usual (" + String.format("%.1f%%", cancelRate) + ").");
        if (todayCount == 0 && weekCount > 0) alerts.add("لا مواعيد اليوم / No appointments today. Next slots may need promotion.");
        if (alerts.isEmpty()) {
            adminAlertsPanel.setVisible(false);
            adminAlertsPanel.setManaged(false);
            return;
        }
        for (String msg : alerts) {
            Label l = new Label("• " + msg);
            l.getStyleClass().add("admin-alert-item");
            l.setWrapText(true);
            adminAlertsContent.getChildren().add(l);
        }
        adminAlertsPanel.setVisible(true);
        adminAlertsPanel.setManaged(true);
    }

    private void updateSessionLabel() {
        if (adminSessionLabel == null) return;
        int timeoutMin = com.appointmentscheduler.application.AppConfig.getSessionTimeoutMinutes();
        java.time.LocalDateTime expires = java.time.LocalDateTime.now().plusMinutes(timeoutMin);
        String role = currentUser instanceof Administrator ? "Admin" : "User";
        adminSessionLabel.setText(role + " · Session until " + expires.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
    }

    @FXML
    public void handleExportHtmlReport() {
        if (ApplicationContext.getPdfReportService() == null) return;
        java.io.File dir = new java.io.File(System.getProperty("user.home"), "AppointmentReports");
        if (!dir.exists()) dir.mkdirs();
        java.time.LocalDate today = java.time.LocalDate.now();
        String fileName = "daily-report-" + today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".html";
        java.io.File file = new java.io.File(dir, fileName);
        try {
            ApplicationContext.getPdfReportService().writeDailyReport(today, file.getAbsolutePath());
            javafx.stage.Window w = welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null;
            if (w != null) ToastNotification.show(w, NotificationType.SUCCESS, null, "Report saved: " + file.getAbsolutePath());
        } catch (Exception e) {
            DialogHelper.showError("Export Report", "Could not save report: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @FXML
    public void handleRefreshAdmin() {
        refreshAllData(() -> {
            if (welcomeLabel != null && welcomeLabel.getScene() != null && welcomeLabel.getScene().getWindow() != null) {
                ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.INFO, null, I18n.get("refresh.toast"));
            }
        });
    }

    @FXML public void handleNavDashboard() { switchView(dashboardView, btnNavDashboard); }
    @FXML public void handleNavAppointments() { switchView(appointmentsView, btnNavAppointments); }
    @FXML public void handleNavUsers() { switchView(usersView, btnNavUsers); }
    @FXML public void handleNavReports() { switchView(reportsView, btnNavReports); }
    @FXML public void handleNavAudit() { switchView(auditView, btnNavAudit); }
    @FXML public void handleNavSettings() {
        switchView(settingsView, btnNavSettings);
    }
    @FXML public void handleNavAppointmentTypes() { switchView(appointmentTypesView, btnNavAppointmentTypes); loadAppointmentTypesTable(); }
    @FXML public void handleNavBranches() { switchView(branchesView, btnNavBranches); loadBranchesTable(); }
    @FXML public void handleNavMessaging() {
        switchView(messagingView, btnNavMessaging);
        if (adminMessagingStatusLabel != null) {
            adminMessagingStatusLabel.setText("");
        }
        refreshStaffContactInbox();
    }

    private void refreshStaffContactInbox() {
        if (staffContactInboxList == null || currentUser == null) return;
        InAppMessagingService svc = ApplicationContext.getInAppMessagingService();
        if (svc == null) return;
        if (!InAppMessagingService.canViewStaffInbox(currentUser)) {
            staffContactInboxList.setItems(FXCollections.observableArrayList());
            return;
        }
        List<StaffContactMessage> items = svc.getStaffContactInbox(100);
        staffContactInboxList.setItems(FXCollections.observableArrayList(items));
        staffContactInboxList.setFixedCellSize(-1);
        staffContactInboxList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(StaffContactMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                Label subj = new Label(item.getSubject());
                subj.setWrapText(true);
                subj.setMaxWidth(720);
                subj.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
                Label preview = new Label(item.bodyPreview(280));
                preview.setWrapText(true);
                preview.setMaxWidth(720);
                preview.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
                Label meta = new Label(item.metaLine());
                meta.setWrapText(true);
                meta.setMaxWidth(720);
                meta.getStyleClass().add("stat-label");
                VBox box = new VBox(6, subj, preview, meta);
                box.setPadding(new javafx.geometry.Insets(4, 8, 10, 4));
                setGraphic(box);
                Tooltip t = new Tooltip(item.getSubject() + "\n\n" + item.getBody());
                t.setWrapText(true);
                t.setMaxWidth(480);
                setTooltip(t);
            }
        });
    }

    @FXML
    public void handleAdminSendBroadcast() {
        InAppMessagingService svc = ApplicationContext.getInAppMessagingService();
        javafx.stage.Window w = welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null;
        if (svc == null) {
            if (w != null) ToastNotification.show(w, NotificationType.ERROR, null, "Messaging service unavailable.");
            return;
        }
        if (!InAppMessagingService.canBroadcast(currentUser)) {
            if (w != null) ToastNotification.show(w, NotificationType.ERROR, null, "Your role cannot send broadcast messages.");
            return;
        }
        String subject = adminMessagingSubjectField != null ? adminMessagingSubjectField.getText() : "";
        String body = adminMessagingBodyArea != null ? adminMessagingBodyArea.getText() : "";
        if (subject == null || subject.isBlank() || body == null || body.isBlank()) {
            if (w != null) ToastNotification.show(w, NotificationType.WARNING, null, "Enter subject and message.");
            return;
        }
        int audIdx = adminMessagingAudienceCombo != null ? adminMessagingAudienceCombo.getSelectionModel().getSelectedIndex() : 0;
        List<User> recipients = new ArrayList<>();
        if (audIdx <= 0) {
            recipients.addAll(svc.listPatients());
        } else if (audIdx == 1) {
            if (usersList != null) {
                for (PatronBookingSummary p : usersList.getItems()) {
                    if (p != null && p.getUser() != null) recipients.add(p.getUser());
                }
            }
        } else {
            PatronBookingSummary sel = usersList != null ? usersList.getSelectionModel().getSelectedItem() : null;
            if (sel == null || sel.getUser() == null) {
                if (w != null) ToastNotification.show(w, NotificationType.WARNING, null, "Select a customer in Booking clients, or choose another audience.");
                return;
            }
            recipients.add(sel.getUser());
        }
        DispatchSummary result = svc.broadcastToPatients(currentUser, recipients, subject.trim(), body.trim());
        if (result.isForbidden()) {
            if (w != null) ToastNotification.show(w, NotificationType.ERROR, null, result.getMessage());
            return;
        }
        if (adminMessagingStatusLabel != null) {
            adminMessagingStatusLabel.setText(result.getMessage());
        }
        if (w != null) {
            ToastNotification.show(w, NotificationType.SUCCESS, null,
                    "Sent: " + result.getSuccessCount() + ", failed: " + result.getFailureCount() + ", skipped: " + result.getSkipped());
        }
    }

    private void switchView(VBox targetView, Button activeBtn) {
        if (targetView == null || activeBtn == null) return;
        if (dashboardView != null) dashboardView.setVisible(false);
        if (appointmentsView != null) appointmentsView.setVisible(false);
        if (usersView != null) usersView.setVisible(false);
        if (reportsView != null) reportsView.setVisible(false);
        if (auditView != null) auditView.setVisible(false);
        if (settingsView != null) settingsView.setVisible(false);
        if (appointmentTypesView != null) appointmentTypesView.setVisible(false);
        if (branchesView != null) branchesView.setVisible(false);
        if (messagingView != null) messagingView.setVisible(false);

        targetView.setVisible(true);

        if (btnNavDashboard != null) btnNavDashboard.getStyleClass().remove("sidebar-btn-active");
        if (btnNavAppointments != null) btnNavAppointments.getStyleClass().remove("sidebar-btn-active");
        if (btnNavUsers != null) btnNavUsers.getStyleClass().remove("sidebar-btn-active");
        if (btnNavReports != null) btnNavReports.getStyleClass().remove("sidebar-btn-active");
        if (btnNavAudit != null) btnNavAudit.getStyleClass().remove("sidebar-btn-active");
        if (btnNavSettings != null) btnNavSettings.getStyleClass().remove("sidebar-btn-active");
        if (btnNavAppointmentTypes != null) btnNavAppointmentTypes.getStyleClass().remove("sidebar-btn-active");
        if (btnNavBranches != null) btnNavBranches.getStyleClass().remove("sidebar-btn-active");
        if (btnNavMessaging != null) btnNavMessaging.getStyleClass().remove("sidebar-btn-active");

        activeBtn.getStyleClass().add("sidebar-btn-active");
        if (targetView == auditView && ApplicationContext.getAuditLogService() != null && auditTable != null) {
            List<AuditEntry> recent = ApplicationContext.getAuditLogService().getRecentEntries(500);
            auditTable.setItems(FXCollections.observableArrayList(recent));
        }
        if (targetView == reportsView) {
            refreshReportsData();
        }
        refreshAllData();
    }

    private void refreshReportsData() {
        if (ApplicationContext.getReportingService() == null) return;
        var perType = ApplicationContext.getReportingService().getAppointmentsPerType();
        StringBuilder sb = new StringBuilder();
        perType.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        if (reportApptsPerTypeValue != null) reportApptsPerTypeValue.setText(sb.length() > 0 ? sb.toString() : "No data");
        if (reportCancellationRateLabel != null) reportCancellationRateLabel.setText(String.format("%.1f%%", ApplicationContext.getReportingService().getCancellationRate()));
        int peak = ApplicationContext.getReportingService().getPeakBookingHour();
        String peakStr = peak <= 12 ? peak + ":00 AM" : (peak == 12 ? "12:00 PM" : (peak - 12) + ":00 PM");
        if (reportPeakHourLabel != null) reportPeakHourLabel.setText(peakStr);
        if (reportSummaryLabel != null) {
            reportSummaryLabel.setText("Total appointments: " + ApplicationContext.getReportingService().getTotalAppointmentsCount()
                    + " | Today: " + ApplicationContext.getReportingService().getTodayAppointmentsCount()
                    + " | Peak hour: " + peakStr);
        }
    }

    private void handleCompleteAppt(Appointment selected) {
        if (selected == null) return;
        String st = selected.getStatus();
        if ("COMPLETED".equals(st) || "CANCELLED".equals(st) || "EXPIRED".equals(st)) return;

        boolean confirmed = DialogHelper.showConfirmation(
                I18n.get("admin.appointment.complete.title"),
                I18n.get("admin.appointment.complete.title"),
                I18n.get("admin.appointment.complete.confirm"));
        if (!confirmed) return;

        Optional<String> err = ApplicationContext.getBookingService().tryCompleteAppointmentWithReason(selected.getId(), currentUser);
        if (err.isEmpty()) {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.SUCCESS, null,
                    I18n.get("admin.appointment.complete.success"));
            refreshAllData();
        } else {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.ERROR, null,
                    I18n.get("admin.appointment.complete.fail"));
        }
    }

    private void handleCancelAppt(Appointment selected) {
        if (selected == null) return;

        boolean confirmed = DialogHelper.showConfirmation(
            "Cancel Appointment", 
            "Are you sure you want to force cancel this appointment?",
            "Patient: " + selected.getPatient().getName() + "\nTime: " + selected.getTimeSlot().toString()
        );
        
        if (!confirmed) return;

        boolean success = ApplicationContext.getBookingService().cancelAppointment(selected.getId(), currentUser);
        if (success) {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.SUCCESS, null, "Appointment force cancelled.");
            refreshAllData();
        } else {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.ERROR, null, "Could not cancel. It might already be cancelled or restricted.");
        }
    }
    
    private static File createSecureAutoDialogFile(String prefix, String suffix) throws IOException {
        File directory = new File(System.getProperty("user.home"), "AppointmentSchedulerAutoExports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create export directory: " + directory.getAbsolutePath());
        }
        return File.createTempFile(prefix, suffix, directory);
    }

    @FXML
    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Appointment Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("Appointments_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");

        File file;
        if (DialogHelper.isAutoDialogs()) {
            try {
                file = createSecureAutoDialogFile("appointmentscheduler-admin-export-", ".csv");
            } catch (IOException ignored) {
                file = null;
            }
        } else {
            file = fileChooser.showSaveDialog(welcomeLabel.getScene().getWindow());
        }
        if (file != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write("ID,Patient Name,Start Time,End Time,Status,Type\n");
                for (Appointment appt : ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments()) {
                    if (appt == null) continue;
                    writer.write(String.join(",",
                        CsvUtils.escape(appt.getId()),
                        CsvUtils.escape(appt.getPatient().getName()),
                        CsvUtils.escape(appt.getTimeSlot().getStartTime()),
                        CsvUtils.escape(appt.getTimeSlot().getEndTime()),
                        CsvUtils.escape(appt.getStatus()),
                        CsvUtils.escape(appt.getClass().getSimpleName())
                    ));
                    writer.write("\n");
                }
                NotificationCenter.getInstance().notify(
                    NotificationType.SUCCESS,
                    "Export",
                    I18n.get("export.success.report")
                );
                ToastNotification.show(
                    welcomeLabel.getScene().getWindow(),
                    NotificationType.SUCCESS,
                    null,
                    I18n.get("export.success.report")
                );
            } catch (IOException ex) {
                DialogHelper.showError(
                    I18n.get("export.error.title"),
                    I18n.get("export.error.message")
                );
            }
        }
    }
    
    @FXML
    public void handleToggleTheme() {
        isDarkMode = !isDarkMode;
        prefs.putBoolean("dark_mode", isDarkMode);
        updateThemeUI();
    }
    
    private void updateThemeUI() {
        if (welcomeLabel == null || welcomeLabel.getScene() == null || welcomeLabel.getScene().getRoot() == null) return;
        
        if (isDarkMode) {
            welcomeLabel.getScene().getRoot().getStyleClass().add("dark-mode");
            if (btnThemeToggle != null) btnThemeToggle.setText("Switch to Light Mode");
        } else {
            welcomeLabel.getScene().getRoot().getStyleClass().remove("dark-mode");
            if (btnThemeToggle != null) btnThemeToggle.setText("Switch to Dark Mode");
        }
        wireClosedDayButtonStyles();
    }

    @FXML
    public void handleExportAudit() {
        if (ApplicationContext.getAuditLogService() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Audit Log");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("audit_log_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");
        File f;
        if (DialogHelper.isAutoDialogs()) {
            try {
                f = createSecureAutoDialogFile("appointmentscheduler-admin-audit-", ".csv");
            } catch (IOException ignored) {
                f = null;
            }
        } else {
            f = fc.showSaveDialog(welcomeLabel != null ? welcomeLabel.getScene().getWindow() : null);
        }
        if (f != null) {
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                w.write("Time,User ID,User Name,Action,Details,Entity Type,Entity ID,Old Value,New Value\n");
                for (AuditEntry e : ApplicationContext.getAuditLogService().getRecentEntries(10000)) {
                    w.write(String.join(",",
                        CsvUtils.escape(e.getTimestampFormatted()),
                        CsvUtils.escape(e.getUserId()),
                        CsvUtils.escape(e.getUserName()),
                        CsvUtils.escape(e.getAction()),
                        CsvUtils.escape(e.getDetails()),
                        CsvUtils.escape(e.getEntityType()),
                        CsvUtils.escape(e.getEntityId()),
                        CsvUtils.escape(e.getOldValue()),
                        CsvUtils.escape(e.getNewValue())
                    ));
                    w.write("\n");
                }
                NotificationCenter.getInstance().notify(
                    NotificationType.SUCCESS,
                    "Export",
                    I18n.get("export.success.audit")
                );
                ToastNotification.show(
                    welcomeLabel.getScene().getWindow(),
                    NotificationType.SUCCESS,
                    null,
                    I18n.get("export.success.audit")
                );
            } catch (IOException ex) {
                DialogHelper.showError(
                    I18n.get("export.error.title"),
                    I18n.get("export.error.message")
                );
            }
        }
    }

    @FXML
    public void handleExportBackupManifest() {
        if (ApplicationContext.getBackupRestoreService() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Backup Manifest");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        fc.setInitialFileName("backup_manifest_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt");
        File f;
        if (DialogHelper.isAutoDialogs()) {
            try {
                f = createSecureAutoDialogFile("appointmentscheduler-admin-backup-", ".txt");
            } catch (IOException ignored) {
                f = null;
            }
        } else {
            f = fc.showSaveDialog(welcomeLabel != null ? welcomeLabel.getScene().getWindow() : null);
        }
        if (f != null) {
            try {
                ApplicationContext.getBackupRestoreService().exportBackupManifest(f.getAbsolutePath());
                NotificationCenter.getInstance().notify(
                    NotificationType.SUCCESS,
                    "Export",
                    I18n.get("export.success.backup")
                );
                ToastNotification.show(
                    welcomeLabel.getScene().getWindow(),
                    NotificationType.SUCCESS,
                    null,
                    I18n.get("export.success.backup")
                );
            } catch (IOException e) {
                DialogHelper.showError(
                    I18n.get("export.error.title"),
                    I18n.get("export.error.message")
                );
            }
        }
    }

    @FXML
    public void handleExportAppointmentsCsv() {
        if (ApplicationContext.getBackupRestoreService() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Appointments CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("appointments_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");
        File f;
        if (DialogHelper.isAutoDialogs()) {
            try {
                f = createSecureAutoDialogFile("appointmentscheduler-admin-appointments-", ".csv");
            } catch (IOException ignored) {
                f = null;
            }
        } else {
            f = fc.showSaveDialog(welcomeLabel != null ? welcomeLabel.getScene().getWindow() : null);
        }
        if (f != null) {
            try {
                ApplicationContext.getBackupRestoreService().exportAppointmentsCsv(f.getAbsolutePath());
                NotificationCenter.getInstance().notify(
                    NotificationType.SUCCESS,
                    "Export",
                    I18n.get("export.success.appointments")
                );
                ToastNotification.show(
                    welcomeLabel.getScene().getWindow(),
                    NotificationType.SUCCESS,
                    null,
                    I18n.get("export.success.appointments")
                );
            } catch (IOException e) {
                DialogHelper.showError(
                    I18n.get("export.error.title"),
                    I18n.get("export.error.message")
                );
            }
        }
    }

    @FXML
    public void handleReportAppointments() {
        handleExportAppointmentsCsv();
    }

    @FXML
    public void handleReportUsers() {
        if (ApplicationContext.getAuthService() == null || ApplicationContext.getAuthService().getUserRepository() == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("تقرير المستخدمين / Users Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("users_report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");
        File f;
        if (DialogHelper.isAutoDialogs()) {
            try {
                f = createSecureAutoDialogFile("appointmentscheduler-admin-users-", ".csv");
            } catch (IOException ignored) {
                f = null;
            }
        } else {
            f = fc.showSaveDialog(welcomeLabel != null ? welcomeLabel.getScene().getWindow() : null);
        }
        if (f != null) {
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                w.write("id,name,email,admin\n");
                for (User u : ApplicationContext.getAuthService().getUserRepository().getAllUsers()) {
                    w.write(CsvUtils.escape(u.getId()) + "," + CsvUtils.escape(u.getName()) + "," + CsvUtils.escape(u.getEmail()) + "," + u.isAdmin() + "\n");
                }
                ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.SUCCESS, null, "Users report saved.");
            } catch (IOException e) {
                DialogHelper.showError(I18n.get("export.error.title"), I18n.get("export.error.message"));
            }
        }
    }

    @FXML
    public void handleReportCancellations() {
        FileChooser fc = new FileChooser();
        fc.setTitle("تقرير الإلغاءات / Cancellations Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("cancellations_report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");
        File f;
        if (DialogHelper.isAutoDialogs()) {
            try {
                f = createSecureAutoDialogFile("appointmentscheduler-admin-cancellations-", ".csv");
            } catch (IOException ignored) {
                f = null;
            }
        } else {
            f = fc.showSaveDialog(welcomeLabel != null ? welcomeLabel.getScene().getWindow() : null);
        }
        if (f != null) {
            try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
                List<Appointment> all = ApplicationContext.getScheduleService().getMasterSchedule().getAllAppointments();
                w.write("appointment_id,patient_id,date,status\n");
                for (Appointment a : all) {
                    if (a == null || !"CANCELLED".equals(a.getStatus())) continue;
                    String id = a.getId() != null ? a.getId() : "";
                    String pid = a.getPatient() != null ? a.getPatient().getId() : "";
                    String date = a.getTimeSlot() != null && a.getTimeSlot().getStartTime() != null ? a.getTimeSlot().getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "";
                    w.write(CsvUtils.escape(id) + "," + CsvUtils.escape(pid) + "," + CsvUtils.escape(date) + ",CANCELLED\n");
                }
                ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.SUCCESS, null, "Cancellations report saved.");
            } catch (IOException e) {
                DialogHelper.showError(I18n.get("export.error.title"), I18n.get("export.error.message"));
            }
        }
    }

    @FXML
    public void handleBlockUser() {
        PatronBookingSummary row = usersList != null ? usersList.getSelectionModel().getSelectedItem() : null;
        User selected = row != null ? row.getUser() : null;
        if (selected == null) {
            ToastNotification.show(welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null,
                NotificationType.WARNING, null, "Select a user first.");
            return;
        }
        if (selected.isAdmin()) {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.WARNING, null, "Cannot block administrator.");
            return;
        }
        boolean ok = DialogHelper.showConfirmation("حظر مستخدم", "Block user?", "Block '" + selected.getName() + "'? They will not be able to log in.");
        if (ok) {
            // TODO: persist blocked state when User/Repository support it
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.INFO, null, "User blocked (persistence to be wired).");
            refreshAllData();
        }
    }

    @FXML
    public void handleDeleteUser() {
        PatronBookingSummary row = usersList != null ? usersList.getSelectionModel().getSelectedItem() : null;
        User selected = row != null ? row.getUser() : null;
        if (selected == null) {
            ToastNotification.show(welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null,
                NotificationType.WARNING, null, "Select a user first.");
            return;
        }
        if (selected.isAdmin()) {
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.WARNING, null, "Cannot delete administrator.");
            return;
        }
        boolean ok = DialogHelper.showConfirmation("حذف مستخدم", "Delete user?", "Permanently delete '" + selected.getName() + "'? This cannot be undone.");
        if (ok) {
            // TODO: UserRepository.deleteById(selected.getId()) when supported
            ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.INFO, null, "Delete user (repository delete not yet implemented).");
            refreshAllData();
        }
    }

    @FXML
    public void handleChangeRole() {
        PatronBookingSummary row = usersList != null ? usersList.getSelectionModel().getSelectedItem() : null;
        User selected = row != null ? row.getUser() : null;
        if (selected == null) {
            ToastNotification.show(welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null,
                NotificationType.WARNING, null, "Select a user first.");
            return;
        }
        // TODO: open role picker (PATIENT, DOCTOR, ADMINISTRATOR) and persist when User has role field
        ToastNotification.show(welcomeLabel.getScene().getWindow(), NotificationType.INFO, null, "Change role (role field not yet on User entity).");
    }

    @FXML
    public void handleLogout() {
        MainApp.performLogout(welcomeLabel != null && welcomeLabel.getScene() != null ? welcomeLabel.getScene().getWindow() : null, currentUser);
    }
}
