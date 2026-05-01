package com.appointmentscheduler.presentation;

/**
 * Utility methods for generating CSV output in a safe, consistent way.
 * Uses RFC4180-style escaping with quotes.
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    /**
     * Escapes a value for inclusion in a CSV file.
     * Nulls become empty, values are wrapped in quotes and inner quotes are doubled.
     */
    public static String escape(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String s = value.toString();
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

