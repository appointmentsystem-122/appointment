package com.appointmentscheduler.application;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appointmentscheduler.domain.AppointmentType;

/**
 * Manages appointment types (name, duration, max participants). Persisted in Preferences.
 */
public final class AppointmentTypeService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentTypeService.class);
    private static final String PREFS_KEY = "admin.appointmentTypes";
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppointmentTypeService.class);
    private static final String SEP = "|";
    private static final String DEF = "Standard|30|1,New session|60|1,Return visit|15|10,Express|30|1,Extended session|90|1,Preparation|45|1";

    private AppointmentTypeService() {
        // Utility class
    }

    /**
     * Loads all configured appointment types from the user preferences store.
     * Malformed rows are ignored and the built-in defaults are restored when no valid entries remain.
     *
     * @return ordered list of configured appointment types
     */
    public static List<AppointmentType> getAll() {
        String raw = PREFS.get(PREFS_KEY, DEF);
        if (raw == null || raw.isBlank()) raw = DEF;
        List<AppointmentType> list = new ArrayList<>();
        for (String line : raw.split(",")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\" + SEP);
            if (parts.length >= 3) {
                try {
                    String name = parts[0].trim();
                    int dur = Integer.parseInt(parts[1].trim());
                    int max = Integer.parseInt(parts[2].trim());
                    list.add(new AppointmentType(name, dur, max));
                } catch (NumberFormatException ex) {
                    log.debug("Skipping malformed appointment type record: {}", line, ex);
                }
            }
        }
        if (list.isEmpty()) {
            list.addAll(defaultTypes());
        }
        return list;
    }

    /**
     * Persists the supplied appointment-type collection to the preferences store.
     *
     * @param types appointment types to store; empty collections are ignored
     */
    public static void saveAll(List<AppointmentType> types) {
        if (types == null || types.isEmpty()) return;
        String value = types.stream()
                .map(t -> t.getName() + SEP + t.getDurationMinutes() + SEP + t.getMaxParticipants())
                .collect(Collectors.joining(","));
        PREFS.put(PREFS_KEY, value);
    }

    /**
     * Adds or replaces a single appointment type by case-insensitive name.
     *
     * @param type appointment type to add or replace
     */
    public static void add(AppointmentType type) {
        List<AppointmentType> list = new ArrayList<>(getAll());
        list.removeIf(t -> t.getName().equalsIgnoreCase(type.getName()));
        list.add(type);
        saveAll(list);
    }

    /**
     * Removes all appointment-type entries whose names match the supplied value ignoring case.
     *
     * @param name appointment-type name to remove
     */
    public static void remove(String name) {
        List<AppointmentType> list = new ArrayList<>(getAll());
        list.removeIf(t -> t.getName().equalsIgnoreCase(name));
        saveAll(list);
    }

    private static List<AppointmentType> defaultTypes() {
        List<AppointmentType> defaults = new ArrayList<>();
        defaults.add(new AppointmentType("Standard", 30, 1));
        defaults.add(new AppointmentType("New session", 60, 1));
        defaults.add(new AppointmentType("Return visit", 15, 10));
        defaults.add(new AppointmentType("Express", 30, 1));
        defaults.add(new AppointmentType("Extended session", 90, 1));
        defaults.add(new AppointmentType("Preparation", 45, 1));
        return defaults;
    }
}