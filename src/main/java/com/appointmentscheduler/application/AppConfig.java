package com.appointmentscheduler.application;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central application configuration loaded from {@code /application.properties}, then optionally
 * {@code /application-local.properties} (same classpath) for machine-specific secrets such as email
 * credentials. The local file should not be committed; add it to {@code .gitignore}.
 */
public final class AppConfig {

    private AppConfig() {
        throw new AssertionError("No instances");
    }

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties props = new Properties();
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppConfig.class);
    private static final String KEY_SYSTEM_TYPE = "app.systemType";
    private static final String SYSTEM_TYPE_HEALTHCARE = "Healthcare";
    private static boolean loaded;

    static {
        loadClasspathProperties();
        loadLocalOverrideProperties();
    }

    private static void loadClasspathProperties() {
        loadPropertiesFromStream(
                AppConfig.class.getResourceAsStream("/application.properties"),
                "Application configuration loaded",
                "Could not load application.properties, using defaults: {}");
    }

    /** Optional second file (not in VCS) for secrets; overrides keys from application.properties. */
    private static void loadLocalOverrideProperties() {
        loadPropertiesFromStream(
                AppConfig.class.getResourceAsStream("/application-local.properties"),
                "Merged application-local.properties",
                "Could not load application-local.properties: {}");
    }

    private static void loadPropertiesFromStream(InputStream rawIn, String debugMsg, String warnMsgPattern) {
        try (InputStream in = rawIn) {
            if (in != null) {
                props.load(in);
                loaded = true;
                log.debug(debugMsg);
            }
        } catch (Exception e) {
            log.warn(warnMsgPattern, e.getMessage());
        }
    }

    /**
     * Same-package tests only: exercise {@code in == null} and error paths without relying on classpath layout.
     */
    static void loadPropertiesFromStreamForTest(InputStream rawIn, String debugMsg, String warnMsgPattern) {
        loadPropertiesFromStream(rawIn, debugMsg, warnMsgPattern);
    }

    /**
     * Same-package tests only: replace in-memory properties (e.g. pin business hours) without Mockito static mocks.
     */
    static void applyPropertiesForTest(Properties p) {
        props.putAll(p);
        loaded = true;
    }

    /** Same-package tests only: restore {@code /application.properties} from the classpath (first match). */
    static void reloadClasspathPropertiesForTest() {
        props.clear();
        loaded = false;
        loadClasspathProperties();
        loadLocalOverrideProperties();
    }

    public static String get(String key, String defaultValue) {
        return loaded ? props.getProperty(key, defaultValue) : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key, null);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key, null);
        if (v == null) return defaultValue;
        return "true".equalsIgnoreCase(v.trim());
    }

    public static String getAppName() { return get("app.name", "Appointment Booking System"); }
    public static String getAppVersion() { return get("app.version", "3.0.0"); }
    public static String getBrandName() { return get("app.brand.name", getAppName()); }
    public static String getBrandTagline() { return get("app.brand.tagline", "Book and manage appointments for any business or service"); }
    public static String getDomainType() { return get("app.domainType", "generic"); }
    /** System / business type (e.g. General, Healthcare, Salon). Saved in Settings; overrides properties. */
    public static String getSystemType() {
        String v = PREFS.get(KEY_SYSTEM_TYPE, get(KEY_SYSTEM_TYPE, "General"));
        if ("Clinic".equals(v)) {
            PREFS.put(KEY_SYSTEM_TYPE, SYSTEM_TYPE_HEALTHCARE);
            return SYSTEM_TYPE_HEALTHCARE;
        }
        return v;
    }
    public static void setSystemType(String value) {
        if (value != null && !value.isBlank()) PREFS.put(KEY_SYSTEM_TYPE, value);
    }
    /** Options for system type dropdown in Admin Settings (domain-neutral labels). */
    public static String[] getSystemTypeOptions() {
        return new String[] { "General", SYSTEM_TYPE_HEALTHCARE, "Salon", "Consultancy", "Education", "Other" };
    }
    public static boolean isShowDevCredentials() { return getBoolean("app.showDevCredentials", false); }
    public static int getSessionTimeoutMinutes() { return getInt("session.timeoutMinutes", 15); }
    public static int getSessionWarningMinutes() { return getInt("session.warningMinutes", 13); }
    public static int getBusinessHourStart() { return getInt("business.hourStart", 9); }
    public static int getBusinessHourEnd() { return getInt("business.hourEnd", 17); }
    public static int getBookingMaxDurationMinutes() { return getInt("booking.maxDurationMinutes", 120); }
    public static int getBookingCutoffHoursBefore() { return getInt("booking.cutoffHoursBefore", 2); }

    private static String[] splitCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .toArray(String[]::new);
    }
    public static String[] getBookingServiceTypes() {
        String raw = get("booking.serviceTypes", "Remote,In person");
        return splitCommaSeparated(raw);
    }
    public static String getOnlineLocationLabel() { return get("booking.onlineLocationLabel", "Remote link (video or phone)"); }
    public static String getOnsiteLocationLabel() { return get("booking.onsiteLocationLabel", "Your premises or agreed address"); }
    /** Comma-separated service labels for booking (any business). */
    public static String[] getBookingAppointmentTypes() {
        String raw = get("booking.appointmentTypes", "Standard,New session,Return visit,Express,Extended session,Preparation");
        return splitCommaSeparated(raw);
    }
    /** 0-based index in appointment types that maps to consultation/assessment. Default 1 = second type. */
    public static int getBookingConsultationTypeIndex() { return getInt("booking.consultationTypeIndex", 1); }
    /**
     * 0-based index for follow-up type. Default -1 = detect by name containing "follow" (case-insensitive).
     */
    public static int getBookingFollowUpTypeIndex() { return getInt("booking.followUpTypeIndex", -1); }

    public static boolean isDatabaseEnabled() { return getBoolean("database.enabled", false); }
    public static String getDatabaseUrl() { return get("database.url", "jdbc:h2:file:./data/appointment_booking;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE;MODE=LEGACY;DATABASE_TO_LOWER=TRUE"); }
    public static String getDatabaseUsername() { return get("database.username", "sa"); }
    public static String getDatabasePassword() { return get("database.password", ""); }

    // --- Email (Gmail SMTP via Jakarta Mail; credentials via env or properties — see JakartaMailEmailNotificationService)
    /** When false, {@link com.appointmentscheduler.application.email.EmailNotificationPort} calls are no-ops at the transport layer. */
    public static boolean isEmailEnabled() { return getBoolean("email.enabled", false); }
    public static String getEmailSmtpHost() { return get("email.smtp.host", "smtp.gmail.com"); }
    public static int getEmailSmtpPort() { return getInt("email.smtp.port", 587); }
    public static boolean isEmailStartTls() { return getBoolean("email.smtp.starttls", true); }

    /**
     * When true, startup overwrites {@code admin@admin.com} with the documented default password (BCrypt).
     * Use after a DB import whose hash does not match {@code admin123}; set back to false afterward in production.
     */
    public static boolean isForceDefaultAdminPasswordOnStartup() {
        return getBoolean("auth.forceDefaultAdminPassword", false);
    }

    public static String getDefaultAdminEmail() {
        return get("auth.defaultAdminEmail", "admin@admin.com");
    }

    /**
     * Development bootstrap credential used only when the initial administrator account must be created or repaired.
     * In production, override this via configuration or environment-backed property sources before first startup.
     */
    public static String getDefaultAdminPassword() {
        return get("auth.defaultAdminPassword", "admin123");
    }

}
