// package com.appointmentscheduler.presentation;

// import com.appointmentscheduler.application.AppConfig;
// import com.appointmentscheduler.application.AuthService;
// import com.appointmentscheduler.application.PasswordHasher;
// import com.appointmentscheduler.domain.Administrator;
// import com.appointmentscheduler.domain.DoctorUser;
// import com.appointmentscheduler.domain.ReceptionistUser;
// import com.appointmentscheduler.domain.User;
// import com.appointmentscheduler.domain.notifiers.CalendarNotifier;
// import com.appointmentscheduler.domain.notifiers.SMSNotification;
// import com.appointmentscheduler.application.*;
// import com.appointmentscheduler.application.email.EmailNotificationPort;
// import com.appointmentscheduler.application.email.JakartaMailEmailNotificationService;
// import com.appointmentscheduler.domain.events.AppointmentEventPublisher;
// import com.appointmentscheduler.presentation.notification.NotificationCenter;
// import com.appointmentscheduler.presentation.notification.NotificationPriority;
// import com.appointmentscheduler.presentation.notification.NotificationType;
// import com.appointmentscheduler.domain.policy.BookingPolicies;
// import com.appointmentscheduler.domain.Clinic;
// import com.appointmentscheduler.domain.Doctor;
// import com.appointmentscheduler.domain.Room;
// import com.appointmentscheduler.domain.rules.AppointmentTypeRuleStrategy;
// import com.appointmentscheduler.domain.rules.BookingRuleStrategy;
// import com.appointmentscheduler.domain.rules.BookingCutoffRuleStrategy;
// import com.appointmentscheduler.domain.rules.CapacityRuleStrategy;
// import com.appointmentscheduler.domain.rules.DoctorConflictRuleStrategy;
// import com.appointmentscheduler.domain.rules.DurationRuleStrategy;
// import com.appointmentscheduler.domain.rules.FollowUpDependencyRuleStrategy;
// import com.appointmentscheduler.domain.rules.MaxAppointmentsPerDoctorRuleStrategy;
// import com.appointmentscheduler.domain.rules.RoomConflictRuleStrategy;
// import com.appointmentscheduler.domain.rules.WorkingHoursRuleStrategy;
// import com.appointmentscheduler.persistence.AppointmentRepository;
// import com.appointmentscheduler.persistence.ClinicRepository;
// import com.appointmentscheduler.persistence.DoctorRepository;
// import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
// import com.appointmentscheduler.persistence.InMemoryClinicRepository;
// import com.appointmentscheduler.persistence.InMemoryDoctorRepository;
// import com.appointmentscheduler.persistence.InMemoryRoomRepository;
// import com.appointmentscheduler.persistence.InMemoryUserRepository;
// import com.appointmentscheduler.persistence.RoomRepository;
// import com.appointmentscheduler.persistence.UserRepository;
// import com.appointmentscheduler.persistence.database.DatabaseConfig;
// import com.appointmentscheduler.persistence.database.JdbcAppointmentRepository;
// import com.appointmentscheduler.persistence.database.JdbcAuditEntryRepository;
// import com.appointmentscheduler.persistence.database.JdbcClinicRepository;
// import com.appointmentscheduler.persistence.database.JdbcDoctorRepository;
// import com.appointmentscheduler.persistence.database.JdbcRoomRepository;
// import com.appointmentscheduler.persistence.database.JdbcUserRepository;
// import javafx.application.Application;
// import javafx.application.Platform;
// import javafx.fxml.FXMLLoader;
// import javafx.scene.Parent;
// import javafx.scene.Scene;
// import javafx.scene.control.Alert;
// import javafx.scene.control.Alert.AlertType;
// import javafx.stage.Stage;
// import javafx.stage.Window;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.util.Optional;

// import java.util.Arrays;
// import java.util.List;

// /**
//  * Main entry point for the JavaFX Application.
//  * Enterprise configuration: DI, config, logging, audit.
//  */
// public class MainApp extends Application {

//     private static final Logger log = LoggerFactory.getLogger(MainApp.class);
//     private static Stage primaryStage;
    
//     /**
//      * Bootstraps the JavaFX application, initializes shared services, and shows the login screen.
//      *
//      * @param stage primary JavaFX stage supplied by the runtime
//      * @throws Exception propagated when JavaFX startup encounters an unrecoverable error
//      */
//     @Override
//     public void start(Stage stage) throws Exception {
//         Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
//             log.error("Uncaught exception in thread " + thread.getName(), e);
//             String errText = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
//             javafx.application.Platform.runLater(() -> showErrorScene("Uncaught error: " + errText));
//         });
//         primaryStage = stage;
//         primaryStage.setTitle(AppConfig.getAppName() + " — " + AppConfig.getSystemType());

//         try {
//             initializeServices();
//         } catch (Throwable t) {
//             log.error("Service initialization failed", t);
//             showErrorScene("Service init failed: " + t.getMessage());
//             javafx.application.Platform.runLater(() ->
//                 ErrorHandler.handle(primaryStage.getScene() != null ? primaryStage.getScene().getWindow() : null,
//                     "Service initialization failed: " + t.getMessage(), t));
//             return;
//         }

//         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//             try {
//                 DatabaseConfig.shutdown();
//             } catch (Throwable t) {
//                 log.warn("Database shutdown hook failed", t);
//             }
//         }));
//         log.info("Starting {} v{}", AppConfig.getAppName(), AppConfig.getAppVersion());

//         try {
//             loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin());
//         } catch (Throwable t) {
//             log.error("Failed to load initial screen", t);
//             String msg = messageOf(t);
//             showErrorScene("Load screen failed: " + msg);
//             javafx.application.Platform.runLater(() ->
//                 ErrorHandler.handle(primaryStage.getScene() != null ? primaryStage.getScene().getWindow() : null,
//                     "Failed to load screen: " + msg, t));
//             return;
//         }

//         primaryStage.setMaximized(true);
//     }

//     private void initializeServices() {
//         UserRepository userRepository;
//         AppointmentRepository appointmentRepository;
//         boolean useDb = AppConfig.isDatabaseEnabled();
//         Optional<javax.sql.DataSource> dsOpt = Optional.empty();
//         boolean showDbWarning = false;
//         String dbErrorDetail = null;

//         if (useDb) {
//             try {
//                 dsOpt = DatabaseConfig.getDataSource();
//             } catch (Throwable t) {
//                 log.error("Database initialization failed — data will NOT be saved to database. Using in-memory storage. Cause: {}", t.getMessage(), t);
//                 dbErrorDetail = messageOf(t);
//             }
//         }

//         if (dsOpt.isPresent()) {
//             try {
//                 javax.sql.DataSource ds = dsOpt.get();
//                 userRepository = new JdbcUserRepository(ds);
//                 appointmentRepository = new JdbcAppointmentRepository(ds, userRepository);
//                 DoctorRepository doctorRepo = new JdbcDoctorRepository(ds);
//                 RoomRepository roomRepo = new JdbcRoomRepository(ds);
//                 ClinicRepository clinicRepo = new JdbcClinicRepository(ds);
//                 AuditLogService auditSvc = new AuditLogService(new JdbcAuditEntryRepository(ds));
//                 ApplicationContext.setDoctorRepository(doctorRepo);
//                 ApplicationContext.setRoomRepository(roomRepo);
//                 ApplicationContext.setClinicRepository(clinicRepo);
//                 ApplicationContext.setAuditLogService(auditSvc);
//                 // Test-only: Mockito mockConstruction listeners must not throw (Mockito wraps as MockitoException).
//                 // Warm-up calls let tests simulate JDBC wiring failures via stubbed repository methods.
//                 if (Boolean.getBoolean("app.test.jdbcRepositoryWarmup")) {
//                     userRepository.findAll();
//                     appointmentRepository.findAll();
//                 }
//                 ApplicationContext.setUsingDatabase(true);
//                 String dbUrl = AppConfig.getDatabaseUrl();
//                 boolean isPg = dbUrl != null && dbUrl.toLowerCase().contains("postgresql");
//                 boolean isMy = dbUrl != null && dbUrl.toLowerCase().contains("mysql");
//                 String dbName = isPg ? "PostgreSQL" : (isMy ? "MySQL" : "H2");
//                 log.info("Using database persistence ({}). User and appointment data will be saved.", dbName);
//             } catch (Throwable t) {
//                 log.error("Database setup failed — data will NOT persist. Falling back to in-memory. Cause: {}", t.getMessage(), t);
//                 if (dbErrorDetail == null) dbErrorDetail = messageOf(t);
//                 ApplicationContext.setUsingDatabase(false);
//                 if (useDb) showDbWarning = true;
//                 userRepository = new InMemoryUserRepository();
//                 appointmentRepository = new InMemoryAppointmentRepository();
//                 ApplicationContext.setDoctorRepository(new InMemoryDoctorRepository());
//                 ApplicationContext.setRoomRepository(new InMemoryRoomRepository());
//                 ApplicationContext.setClinicRepository(new InMemoryClinicRepository());
//                 ApplicationContext.setAuditLogService(new AuditLogService());
//             }
//         } else {
//             ApplicationContext.setUsingDatabase(false);
//             if (useDb) showDbWarning = true;
//             if (dbErrorDetail == null) dbErrorDetail = "Connection or configuration failed.";
//             userRepository = new InMemoryUserRepository();
//             appointmentRepository = new InMemoryAppointmentRepository();
//             ApplicationContext.setDoctorRepository(new InMemoryDoctorRepository());
//             ApplicationContext.setRoomRepository(new InMemoryRoomRepository());
//             ApplicationContext.setClinicRepository(new InMemoryClinicRepository());
//             ApplicationContext.setAuditLogService(new AuditLogService());
//         }

//         LoginAttemptService loginAttemptSvc = new InMemoryLoginAttemptService();
//         ApplicationContext.setLoginAttemptService(loginAttemptSvc);

//         AuthService authSvc = new AuthService(userRepository, loginAttemptSvc, ApplicationContext.getAuditLogService());
//         ApplicationContext.setAuthService(authSvc);
//         ApplicationContext.setPermissionService(new PermissionService());

//         PatientInboxService patientInboxSvc = new PatientInboxService();
//         StaffInboxService staffInboxSvc = new StaffInboxService();
//         InAppMessagingService inAppMessagingSvc = new InAppMessagingService(
//                 userRepository,
//                 ApplicationContext.getAuditLogService(),
//                 patientInboxSvc,
//                 staffInboxSvc);
//         ApplicationContext.setInAppMessagingService(inAppMessagingSvc);

//         AppointmentReminderService reminderSvc = new AppointmentReminderService();
//         reminderSvc.registerObserver(new SMSNotification());
//         ApplicationContext.setAppointmentReminderPort(reminderSvc);

//         NotificationService notifSvc = new NotificationService(reminderSvc);
//         notifSvc.attach(new CalendarNotifier());
//         ApplicationContext.setNotificationService(notifSvc);

//         ScheduleService scheduleSvc = new ScheduleService(appointmentRepository);
//         ClosedDayService closedDaySvc = new ClosedDayService();
//         scheduleSvc.setClosedDayService(closedDaySvc);
//         ApplicationContext.setScheduleService(scheduleSvc);
//         ApplicationContext.setClosedDayService(closedDaySvc);

//         List<BookingRuleStrategy> rules = Arrays.asList(
//             new DurationRuleStrategy(AppConfig.getBookingMaxDurationMinutes()),
//             new CapacityRuleStrategy(),
//             new AppointmentTypeRuleStrategy(),
//             new WorkingHoursRuleStrategy(),
//             new BookingCutoffRuleStrategy(),
//             new FollowUpDependencyRuleStrategy(appointmentRepository),
//             new DoctorConflictRuleStrategy(appointmentRepository),
//             new RoomConflictRuleStrategy(appointmentRepository),
//             new MaxAppointmentsPerDoctorRuleStrategy(appointmentRepository, ApplicationContext.getDoctorRepository())
//         );

//         AppointmentEventPublisher eventPublisher = new AppointmentEventPublisher();
//         eventPublisher.addListener(new NotificationEventBridge(ApplicationContext.getNotificationService()));

//         PolicyEngine policyEngine = new PolicyEngine();
//         policyEngine.registerPolicy(new BookingPolicies.NoModifyCancelledExpiredPolicy());
//         policyEngine.registerPolicy(new BookingPolicies.RequesterAuthorizationPolicy(true));
//         policyEngine.registerPolicy(new BookingPolicies.StateTransitionPolicy());
//         policyEngine.registerPolicy(new BookingPolicies.NoDoubleBookingPolicy(() ->
//             scheduleSvc.getMasterSchedule().getAllAppointments()));

//         AppointmentExpirationService expirationService = new AppointmentExpirationService(appointmentRepository, ApplicationContext.getAuditLogService());

//         EmailNotificationPort emailNotificationPort = AppConfig.isEmailEnabled()
//                 ? new JakartaMailEmailNotificationService()
//                 : null;

//         BookingService bookingSvc = new BookingService(appointmentRepository, ApplicationContext.getNotificationService(), scheduleSvc, rules, ApplicationContext.getAuditLogService(),
//             ApplicationContext.getPermissionService(), policyEngine, eventPublisher, expirationService, emailNotificationPort);
//         ApplicationContext.setBookingService(bookingSvc);

//         ApplicationContext.setReportingService(new ReportingService(appointmentRepository));
//         ApplicationContext.setSlotRecommendationService(new SlotRecommendationService(scheduleSvc));
//         ApplicationContext.setGlobalSearchService(new GlobalSearchService(appointmentRepository, userRepository));
//         ApplicationContext.setPdfReportService(new PdfReportService(appointmentRepository, ApplicationContext.getReportingService()));
//         ApplicationContext.setAppNotificationStore(new AppNotificationStore());
//         ApplicationContext.setCurrentClinicService(new CurrentClinicService(ApplicationContext.getClinicRepository()));
//         ApplicationContext.setBackupRestoreService(new BackupRestoreService(appointmentRepository, userRepository, ApplicationContext.getDoctorRepository(), ApplicationContext.getRoomRepository(), ApplicationContext.getClinicRepository()));

//         if (showDbWarning) {
//             final String errDetail = dbErrorDetail;
//             Platform.runLater(() -> showDatabaseNotConnectedWarning(errDetail));
//         }

//         if (!useDb || userRepository.findAll().isEmpty()) {
//             setupDummyData(userRepository, appointmentRepository);
//         } else {
//             ensureDefaultAdminUser(userRepository);
//         }

//         eventPublisher.addListener(e -> {
//             if (e == null || e.getAppointment() == null) return;
//             // Only show notification when admin/receptionist performs the action (to notify the user)
//             User actor = e.getActor();
//             if (actor == null || (!(actor instanceof Administrator) && !(actor instanceof ReceptionistUser))) return;
//             String title = "Appointment " + e.getType();
//             String msg = e.getAppointment().getTimeSlot() + " – " + (e.getDetails() != null ? e.getDetails() : "");
//             if (ApplicationContext.getAppNotificationStore() != null) ApplicationContext.getAppNotificationStore().add(title, msg);
//             NotificationType nt = switch (e.getType()) {
//                 case CREATED -> NotificationType.APPOINTMENT_CREATED;
//                 case MODIFIED -> NotificationType.APPOINTMENT_MODIFIED;
//                 case CANCELLED -> NotificationType.APPOINTMENT_CANCELLED;
//                 case COMPLETED -> NotificationType.APPOINTMENT_COMPLETED;
//                 case REMINDER -> NotificationType.REMINDER;
//             };
//             NotificationCenter.getInstance().notify(nt, NotificationPriority.NORMAL, title, msg, "Appointment", e.getAppointment().getId());
//         });
//     }
    

//     /**
//      * If the database was imported or already had users, full {@link #setupDummyData} is skipped.
//      * Ensures {@code admin@admin.com} / {@code admin123} works: creates the admin if missing, or resets the
//      * password when the stored hash is not valid BCrypt (common after SQL imports), or when
//      * {@link AppConfig#isForceDefaultAdminPasswordOnStartup()} is true.
//      */
//     private void ensureDefaultAdminUser(UserRepository userRepository) {
//         final String adminEmail = "admin@admin.com";
//         final String defaultPassword = "admin123";
//         try {
//             Optional<User> opt = userRepository.findByEmail(adminEmail);
//             if (opt.isEmpty()) {
//                 User admin = new Administrator("admin-1", "Admin User", adminEmail, PasswordHasher.hash(defaultPassword));
//                 userRepository.save(admin);
//                 log.info("Default admin user created ({}) — database had users but no admin account.", adminEmail);
//                 return;
//             }
//             User u = opt.get();
//             if (!(u instanceof Administrator)) {
//                 User admin = new Administrator(u.getId(), u.getName(), u.getEmail(), PasswordHasher.hash(defaultPassword));
//                 userRepository.save(admin);
//                 log.info("User {} was {}; converted to ADMINISTRATOR with default password (startup repair).", adminEmail, u.getClass().getSimpleName());
//                 return;
//             }
//             if (PasswordHasher.verify(defaultPassword, u.getPassword())) {
//                 return;
//             }
//             boolean force = AppConfig.isForceDefaultAdminPasswordOnStartup();
//             boolean nonBcrypt = !looksLikeBcryptHash(u.getPassword());
//             if (force || nonBcrypt) {
//                 User admin = new Administrator(u.getId(), u.getName(), u.getEmail(), PasswordHasher.hash(defaultPassword));
//                 userRepository.save(admin);
//                 log.info("Default admin password reset (email={}): force={}, nonBcryptStored={}.", adminEmail, force, nonBcrypt);
//             } else {
//                 log.warn("Stored password for {} does not match {}. Set auth.forceDefaultAdminPassword=true once in application.properties, restart, then set it back to false.",
//                         adminEmail, defaultPassword);
//             }
//         } catch (Exception e) {
//             log.warn("Could not ensure default admin user: {}", e.getMessage());
//         }
//     }

//     private static boolean looksLikeBcryptHash(String stored) {
//         if (stored == null || stored.length() < 7) return false;
//         return stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$");
//     }


//     private void setupDummyData(UserRepository userRepository, AppointmentRepository appointmentRepository) {
//         User admin = new Administrator("admin-1", "Admin User", "admin@admin.com", PasswordHasher.hash("admin123"));
//         User patient = new User("user-1", "Alex Customer", "customer@example.com", PasswordHasher.hash("password123"));
//         User doctorUser = new DoctorUser("doc-1", "Sam Provider", "provider@example.com", PasswordHasher.hash("doctor123"));
//         User receptionist = new ReceptionistUser("rec-1", "Jordan Staff", "staff@example.com", PasswordHasher.hash("reception123"));
//         userRepository.save(admin);
//         userRepository.save(patient);
//         userRepository.save(doctorUser);
//         userRepository.save(receptionist);

//         ApplicationContext.getClinicRepository().save(new Clinic("clinic-1", "Main office", "123 Example Street", "UTC"));
//         ApplicationContext.getClinicRepository().save(new Clinic("clinic-2", "North office", "456 Sample Road", "UTC"));
//         ApplicationContext.getCurrentClinicService().setCurrentClinicId("clinic-1");

//         Doctor doc = new Doctor("doc-1", "Sam Provider", "provider@example.com", "General services", 12, "clinic-1");
//         ApplicationContext.getDoctorRepository().save(doc);
//         ApplicationContext.getDoctorRepository().save(new Doctor("doc-2", "Riley Specialist", "riley@example.com", "Specialist", 8, "clinic-2"));
//         ApplicationContext.getRoomRepository().save(new Room("room-1", "Room A", "clinic-1"));
//         ApplicationContext.getRoomRepository().save(new Room("room-2", "Room B", "clinic-1"));
//     }

//     /**
//      * Returns the primary stage (for window owner in dialogs).
//      */
//     public static Stage getPrimaryStage() {
//         return primaryStage;
//     }

//     /**
//      * Centralized logout: confirmation, audit log, then redirect to login.
//      * Use from any window (Admin, Patient, Book, Modify) for consistent enterprise behavior.
//      */
//     public static void performLogout(Window owner, com.appointmentscheduler.domain.User currentUser) {
//         if (owner == null) owner = primaryStage != null ? primaryStage.getScene() != null ? primaryStage.getScene().getWindow() : null : null;
//         boolean confirm = DialogHelper.showLogoutConfirmation(
//             com.appointmentscheduler.application.AppConfig.getAppName()
//         );
//         if (!confirm) return;
//         if (currentUser != null && ApplicationContext.getAuditLogService() != null) {
//             ApplicationContext.getAuditLogService().log(currentUser, "LOGOUT", "User logged out from application");
//         }
//         ApplicationContext.getAuthService().logout();
//         loadScreen(ScreenConstants.FXML_LOGIN, ScreenConstants.titleLogin());
//     }

//     private static volatile boolean loadScreenInProgress;

//     /**
//      * Loads an FXML screen into the primary stage and applies the configured stylesheet set.
//      *
//      * @param fxmlString FXML file name resolved relative to the presentation resources path
//      * @param title stage title to apply after the scene is loaded
//      */
//     public static void loadScreen(String fxmlString, String title) {
//         if (loadScreenInProgress) {
//             log.warn("loadScreen re-entry ignored: {}", fxmlString);
//             return;
//         }
//         loadScreenInProgress = true;
//         try {
//             String resourcePath = ScreenConstants.BASE_PATH + fxmlString;
//             String altPath = "com/appointmentscheduler/presentation/" + fxmlString;
//             String relPath = fxmlString;
//             java.net.URL fxmlUrl = MainApp.class.getResource(resourcePath);
//             if (fxmlUrl == null) fxmlUrl = MainApp.class.getResource(altPath);
//             if (fxmlUrl == null) fxmlUrl = MainApp.class.getResource(relPath);
//             if (fxmlUrl == null) {
//                 throw new IllegalStateException("FXML not found. Tried: " + resourcePath + ", " + altPath + ", " + relPath + ". Check that src/main/resources is on classpath.");
//             }
//             FXMLLoader loader = new FXMLLoader(fxmlUrl);
//             Parent root = loader.load();
//             Scene scene = new Scene(root, 1280, 800);
//             addStylesheetsSafely(scene);
//             primaryStage.setScene(scene);
//             primaryStage.setTitle(title);
//             primaryStage.show();

//             javafx.application.Platform.runLater(() -> {
//                 try {
//                     SessionManager.getInstance().registerScene(scene);
//                 } finally {
//                     loadScreenInProgress = false;
//                 }
//             });
//         } catch (Throwable ex) {
//             loadScreenInProgress = false;
//             log.error("Failed to load screen: {}", fxmlString, ex);
//             final String errMsg = messageOf(ex);
//             final Throwable err = ex;
//             showErrorScene("Screen load failed: " + errMsg);
//             javafx.application.Platform.runLater(() ->
//                 ErrorHandler.handle(primaryStage != null && primaryStage.getScene() != null ? primaryStage.getScene().getWindow() : null,
//                     "Failed to load screen: " + errMsg, err));
//         }
//     }

//     /** Builds a user-readable message from a throwable (never null). */
//     private static String messageOf(Throwable t) {
//         if (t == null) return "Unknown error";
//         String m = t.getMessage();
//         if (m != null && !m.isBlank()) return m;
//         Throwable cause = t.getCause();
//         if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank())
//             return cause.getMessage();
//         return t.getClass().getSimpleName();
//     }

//     /** Shows a warning when database.enabled=true but connection failed (data will not persist). */
//     private void showDatabaseNotConnectedWarning(String errorDetail) {
//         try {
//             Alert alert = new Alert(AlertType.WARNING);
//             alert.setTitle(I18n.get("db.warning.not_connected.title"));
//             alert.setHeaderText(null);
//             String msg = I18n.get("db.warning.not_connected.message");
//             if (errorDetail != null && !errorDetail.isBlank()) {
//                 msg += "\n\n" + I18n.get("db.warning.error_detail") + " " + errorDetail;
//             }
//             alert.setContentText(msg);
//             alert.getDialogPane().setPrefWidth(520);
//             if (primaryStage != null && primaryStage.getScene() != null) {
//                 alert.initOwner(primaryStage.getScene().getWindow());
//             }
//             if (!DialogHelper.isAutoDialogs()) {
//                 alert.showAndWait();
//             }
//         } catch (Throwable t) {
//             log.warn("Could not show database warning dialog", t);
//         }
//     }

//     /** Shows a simple error scene so the user always sees a window with the error message. */
//     private static void showErrorScene(String message) {
//         if (primaryStage == null) return;
//         try {
//             javafx.scene.control.Label label = new javafx.scene.control.Label(message);
//             label.setWrapText(true);
//             label.setMaxWidth(600);
//             label.setStyle("-fx-font-size: 14px; -fx-padding: 20;");
//             javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(label);
//             box.setAlignment(javafx.geometry.Pos.CENTER);
//             javafx.scene.Scene errScene = new javafx.scene.Scene(box, 640, 200);
//             primaryStage.setScene(errScene);
//             primaryStage.setTitle("Error - " + AppConfig.getAppName());
//             primaryStage.show();
//         } catch (Throwable t) {
//             log.error("Could not show error scene", t);
//         }
//     }

//     private static final boolean USE_FULL_THEME = false;
//     private static final boolean USE_MINIMAL_THEME = true;

//     /** Loads CSS: full theme, or minimal (no lookups/effects), or none. Avoids StackOverflow. */
//     private static void addStylesheetsSafely(Scene scene) {
//         if (USE_FULL_THEME) {
//             addStylesheetIfPresent(scene, "/com/appointmentscheduler/presentation/application.css");
//             addStylesheetIfPresent(scene, "/com/appointmentscheduler/presentation/enterprise-additions.css");
//             addStylesheetIfPresent(scene, "/com/appointmentscheduler/presentation/enterprise-ui.css");
//         } else if (USE_MINIMAL_THEME) {
//             addStylesheetIfPresent(scene, "/com/appointmentscheduler/presentation/application-minimal.css");
//         }
//     }

//     private static void addStylesheetIfPresent(Scene scene, String path) {
//         java.net.URL url = MainApp.class.getResource(path);
//         if (url != null) {
//             try {
//                 scene.getStylesheets().add(url.toExternalForm());
//             } catch (Exception e) {
//                 log.warn("Could not load stylesheet: {}", path, e);
//             }
//         }
//     }

//     /**
//      * Launches the JavaFX desktop application.
//      *
//      * @param args command-line arguments forwarded to JavaFX
//      */
//     public static void main(String[] args) {
//         launch(args);
//     }
// }
