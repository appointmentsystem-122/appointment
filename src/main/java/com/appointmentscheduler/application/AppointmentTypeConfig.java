package com.appointmentscheduler.application;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages appointment types (name, duration minutes, max participants).
 * Persisted in Preferences; used by admin and booking.
 */
public final class AppointmentTypeConfig {

    private static final String PREFS_KEY = "admin.appointmentTypes";
    private static final String SEP = "|";
    private static final String SUB = ":";
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppointmentTypeConfig.class);

    public static final class Type {
        private final String name;
        private final int durationMinutes;
        private final int maxParticipants;

        public Type(String name, int durationMinutes, int maxParticipants) {
            this.name = name != null ? name : "General";
            this.durationMinutes = durationMinutes <= 0 ? 60 : durationMinutes;
            this.maxParticipants = maxParticipants <= 0 ? 1 : maxParticipants;
        }

        public String getName() { return name; }
        public int getDurationMinutes() { return durationMinutes; }
        public int getMaxParticipants() { return maxParticipants; }
    }

    public static List<Type> getAll() {
        String raw = PREFS.get(PREFS_KEY, "");
        if (raw == null || raw.isBlank()) {
            List<Type> def = new ArrayList<>();
            for (String s : AppConfig.getBookingAppointmentTypes()) {
                def.add(new Type(s, 60, 10));
            }
            return def;
        }
        List<Type> list = new ArrayList<>();
        for (String part : raw.split("\\" + SEP)) {
            String[] kv = part.split(SUB);
            if (kv.length >= 3) {
                String typeName = kv[0].trim();
                int durationMinutes = parsePositiveIntOrDefault(kv[1], 60);
                int maxParticipants = parsePositiveIntOrDefault(kv[2], 1);
                list.add(new Type(typeName, durationMinutes, maxParticipants));
            }
        }
        return list;
    }

    private static int parsePositiveIntOrDefault(String rawValue, int defaultValue) {
        if (rawValue == null) return defaultValue;
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static void save(List<Type> types) {
        if (types == null || types.isEmpty()) return;
        String value = types.stream()
                .map(t -> t.getName() + SUB + t.getDurationMinutes() + SUB + t.getMaxParticipants())
                .reduce((a, b) -> a + SEP + b).orElse("");
        PREFS.put(PREFS_KEY, value);
    }

    public static void add(Type type) {
        List<Type> all = new ArrayList<>(getAll());
        all.removeIf(t -> t.getName().equalsIgnoreCase(type.getName()));
        all.add(type);
        save(all);
    }

    public static void remove(String name) {
        List<Type> all = new ArrayList<>(getAll());
        all.removeIf(t -> t.getName().equalsIgnoreCase(name));
        save(all);
    }

    /** Returns type names for combo boxes (e.g. booking form). */
    public static String[] getTypeNames() {
        return getAll().stream().map(Type::getName).toArray(String[]::new);
    }
}
