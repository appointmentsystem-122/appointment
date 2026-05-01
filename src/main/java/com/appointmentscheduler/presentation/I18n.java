package com.appointmentscheduler.presentation;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.text.MessageFormat;

/**
 * Centralized internationalization for the application.
 * Supports English and Arabic; locale can be set from user preferences.
 */
public final class I18n {

    private static final String BUNDLE_BASE = "com.appointmentscheduler.presentation.messages";
    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle bundle;

    static {
        reload();
    }

    public static void setLocale(Locale locale) {
        if (locale != null) {
            currentLocale = locale;
            reload();
        }
    }

    public static Locale getLocale() {
        return currentLocale;
    }

    public static void setLanguage(String languageTag) {
        if (languageTag == null || languageTag.isEmpty()) return;
        if ("ar".equalsIgnoreCase(languageTag) || "ar_SA".equalsIgnoreCase(languageTag)) {
            setLocale(new Locale("ar"));
        } else {
            setLocale(Locale.ENGLISH);
        }
    }

    private static void reload() {
        try {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH);
        }
    }

    /**
     * Returns the localized string for the key, or the key itself if missing.
     */
    public static String get(String key) {
        if (key == null) return "";
        try {
            return bundle != null ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the localized string with parameters (e.g. {0}).
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        if (args == null || args.length == 0) return pattern;
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }
}
