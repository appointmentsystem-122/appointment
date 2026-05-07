package com.appointmentscheduler.application;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final AtomicReference<AuthService> authService = new AtomicReference<>();
    private static final AtomicReference<BookingService> bookingService = new AtomicReference<>();
    private static final AtomicReference<ScheduleService> scheduleService = new AtomicReference<>();
    private static final AtomicReference<NotificationService> notificationService = new AtomicReference<>();
    private static final AtomicReference<AppointmentReminderPort> appointmentReminderPort = new AtomicReference<>();
    private static final AtomicReference<InAppMessagingService> inAppMessagingService = new AtomicReference<>();
    private static final AtomicReference<AuditLogService> auditLogService = new AtomicReference<>();
    private static final AtomicReference<PermissionService> permissionService = new AtomicReference<>();
    private static final AtomicReference<ReportingService> reportingService = new AtomicReference<>();
    private static final AtomicReference<SlotRecommendationService> slotRecommendationService = new AtomicReference<>();
    private static final AtomicReference<LoginAttemptService> loginAttemptService = new AtomicReference<>();
    private static final AtomicReference<GlobalSearchService> globalSearchService = new AtomicReference<>();
    private static final AtomicReference<PdfReportService> pdfReportService = new AtomicReference<>();
    private static final AtomicReference<AppNotificationStore> appNotificationStore = new AtomicReference<>();
    private static final AtomicReference<DoctorRepository> doctorRepository = new AtomicReference<>();
    private static final AtomicReference<RoomRepository> roomRepository = new AtomicReference<>();
    private static final AtomicReference<ClinicRepository> clinicRepository = new AtomicReference<>();
    private static final AtomicReference<CurrentClinicService> currentClinicService = new AtomicReference<>();
    private static final AtomicReference<BackupRestoreService> backupRestoreService = new AtomicReference<>();
    private static final AtomicReference<ClosedDayService> closedDayService = new AtomicReference<>();

    /** True when persistence is via database (PostgreSQL/H2); false when in-memory only. */
    private static final AtomicBoolean usingDatabase = new AtomicBoolean();

    private ApplicationContext() { }

    public static boolean isUsingDatabase() { return usingDatabase.get(); }

    // --- Getters (public API for rest of app)
    public static AuthService getAuthService() { return authService.get(); }
    public static BookingService getBookingService() { return bookingService.get(); }
    public static ScheduleService getScheduleService() { return scheduleService.get(); }
    public static NotificationService getNotificationService() { return notificationService.get(); }
    public static AppointmentReminderPort getAppointmentReminderPort() { return appointmentReminderPort.get(); }
    public static InAppMessagingService getInAppMessagingService() { return inAppMessagingService.get(); }
    public static AuditLogService getAuditLogService() { return auditLogService.get(); }
    public static PermissionService getPermissionService() { return permissionService.get(); }
    public static ReportingService getReportingService() { return reportingService.get(); }
    public static SlotRecommendationService getSlotRecommendationService() { return slotRecommendationService.get(); }
    public static LoginAttemptService getLoginAttemptService() { return loginAttemptService.get(); }
    public static GlobalSearchService getGlobalSearchService() { return globalSearchService.get(); }
    public static PdfReportService getPdfReportService() { return pdfReportService.get(); }
    public static AppNotificationStore getAppNotificationStore() { return appNotificationStore.get(); }
    public static DoctorRepository getDoctorRepository() { return doctorRepository.get(); }
    public static RoomRepository getRoomRepository() { return roomRepository.get(); }
    public static ClinicRepository getClinicRepository() { return clinicRepository.get(); }
    public static CurrentClinicService getCurrentClinicService() { return currentClinicService.get(); }
    public static BackupRestoreService getBackupRestoreService() { return backupRestoreService.get(); }
    public static ClosedDayService getClosedDayService() { return closedDayService.get(); }

    /** Used by MainApp only to populate the context at startup. */
    public static void setAuthService(AuthService v) { authService.set(v); }
    public static void setBookingService(BookingService v) { bookingService.set(v); }
    public static void setScheduleService(ScheduleService v) { scheduleService.set(v); }
    public static void setNotificationService(NotificationService v) { notificationService.set(v); }
    public static void setAppointmentReminderPort(AppointmentReminderPort v) { appointmentReminderPort.set(v); }
    public static void setInAppMessagingService(InAppMessagingService v) { inAppMessagingService.set(v); }
    public static void setAuditLogService(AuditLogService v) { auditLogService.set(v); }
    public static void setPermissionService(PermissionService v) { permissionService.set(v); }
    public static void setReportingService(ReportingService v) { reportingService.set(v); }
    public static void setSlotRecommendationService(SlotRecommendationService v) { slotRecommendationService.set(v); }
    public static void setLoginAttemptService(LoginAttemptService v) { loginAttemptService.set(v); }
    public static void setGlobalSearchService(GlobalSearchService v) { globalSearchService.set(v); }
    public static void setPdfReportService(PdfReportService v) { pdfReportService.set(v); }
    public static void setAppNotificationStore(AppNotificationStore v) { appNotificationStore.set(v); }
    public static void setDoctorRepository(DoctorRepository v) { doctorRepository.set(v); }
    public static void setRoomRepository(RoomRepository v) { roomRepository.set(v); }
    public static void setClinicRepository(ClinicRepository v) { clinicRepository.set(v); }
    public static void setCurrentClinicService(CurrentClinicService v) { currentClinicService.set(v); }
    public static void setBackupRestoreService(BackupRestoreService v) { backupRestoreService.set(v); }
    public static void setClosedDayService(ClosedDayService v) { closedDayService.set(v); }

    /** Set by MainApp at startup. */
    public static void setUsingDatabase(boolean v) { usingDatabase.set(v); }
}