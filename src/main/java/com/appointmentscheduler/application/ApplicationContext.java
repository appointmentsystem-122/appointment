package com.appointmentscheduler.application;

import com.appointmentscheduler.persistence.ClinicRepository;
import com.appointmentscheduler.persistence.DoctorRepository;
import com.appointmentscheduler.persistence.RoomRepository;
import com.appointmentscheduler.presentation.AppNotificationStore;

/**
 * Central application context (service locator) for enterprise dependency access.
 * Populated at startup by MainApp; controllers and services obtain dependencies via getters.
 * Enables consistent access, testability, and future replacement with full DI.
 */
public final class ApplicationContext {

    private static volatile AuthService authService;
    private static volatile BookingService bookingService;
    private static volatile ScheduleService scheduleService;
    private static volatile NotificationService notificationService;
    private static volatile AppointmentReminderPort appointmentReminderPort;
    private static volatile InAppMessagingService inAppMessagingService;
    private static volatile AuditLogService auditLogService;
    private static volatile PermissionService permissionService;
    private static volatile ReportingService reportingService;
    private static volatile SlotRecommendationService slotRecommendationService;
    private static volatile LoginAttemptService loginAttemptService;
    private static volatile GlobalSearchService globalSearchService;
    private static volatile PdfReportService pdfReportService;
    private static volatile AppNotificationStore appNotificationStore;
    private static volatile DoctorRepository doctorRepository;
    private static volatile RoomRepository roomRepository;
    private static volatile ClinicRepository clinicRepository;
    private static volatile CurrentClinicService currentClinicService;
    private static volatile BackupRestoreService backupRestoreService;
    private static volatile ClosedDayService closedDayService;
    /** True when persistence is via database (PostgreSQL/H2); false when in-memory only. */
    private static volatile boolean usingDatabase;

    private ApplicationContext() { }

    public static boolean isUsingDatabase() { return usingDatabase; }

    // --- Getters (public API for rest of app)
    public static AuthService getAuthService() { return authService; }
    public static BookingService getBookingService() { return bookingService; }
    public static ScheduleService getScheduleService() { return scheduleService; }
    public static NotificationService getNotificationService() { return notificationService; }
    public static AppointmentReminderPort getAppointmentReminderPort() { return appointmentReminderPort; }
    public static InAppMessagingService getInAppMessagingService() { return inAppMessagingService; }
    public static AuditLogService getAuditLogService() { return auditLogService; }
    public static PermissionService getPermissionService() { return permissionService; }
    public static ReportingService getReportingService() { return reportingService; }
    public static SlotRecommendationService getSlotRecommendationService() { return slotRecommendationService; }
    public static LoginAttemptService getLoginAttemptService() { return loginAttemptService; }
    public static GlobalSearchService getGlobalSearchService() { return globalSearchService; }
    public static PdfReportService getPdfReportService() { return pdfReportService; }
    public static AppNotificationStore getAppNotificationStore() { return appNotificationStore; }
    public static DoctorRepository getDoctorRepository() { return doctorRepository; }
    public static RoomRepository getRoomRepository() { return roomRepository; }
    public static ClinicRepository getClinicRepository() { return clinicRepository; }
    public static CurrentClinicService getCurrentClinicService() { return currentClinicService; }
    public static BackupRestoreService getBackupRestoreService() { return backupRestoreService; }
    public static ClosedDayService getClosedDayService() { return closedDayService; }

    /** Used by MainApp only to populate the context at startup. */
    public static void setAuthService(AuthService v) { authService = v; }
    public static void setBookingService(BookingService v) { bookingService = v; }
    public static void setScheduleService(ScheduleService v) { scheduleService = v; }
    public static void setNotificationService(NotificationService v) { notificationService = v; }
    public static void setAppointmentReminderPort(AppointmentReminderPort v) { appointmentReminderPort = v; }
    public static void setInAppMessagingService(InAppMessagingService v) { inAppMessagingService = v; }
    public static void setAuditLogService(AuditLogService v) { auditLogService = v; }
    public static void setPermissionService(PermissionService v) { permissionService = v; }
    public static void setReportingService(ReportingService v) { reportingService = v; }
    public static void setSlotRecommendationService(SlotRecommendationService v) { slotRecommendationService = v; }
    public static void setLoginAttemptService(LoginAttemptService v) { loginAttemptService = v; }
    public static void setGlobalSearchService(GlobalSearchService v) { globalSearchService = v; }
    public static void setPdfReportService(PdfReportService v) { pdfReportService = v; }
    public static void setAppNotificationStore(AppNotificationStore v) { appNotificationStore = v; }
    public static void setDoctorRepository(DoctorRepository v) { doctorRepository = v; }
    public static void setRoomRepository(RoomRepository v) { roomRepository = v; }
    public static void setClinicRepository(ClinicRepository v) { clinicRepository = v; }
    public static void setCurrentClinicService(CurrentClinicService v) { currentClinicService = v; }
    public static void setBackupRestoreService(BackupRestoreService v) { backupRestoreService = v; }
    public static void setClosedDayService(ClosedDayService v) { closedDayService = v; }
    /** Set by MainApp at startup. */
    public static void setUsingDatabase(boolean v) { usingDatabase = v; }
}
