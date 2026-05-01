package com.appointmentscheduler.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Manages closed days (days with no availability). Persisted in Preferences.
 */
public final class ClosedDayService {

    private static final String PREFS_KEY = "admin.closedDays";
    private static final String SEP = ",";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final Preferences prefs;

    public ClosedDayService() {
        this.prefs = Preferences.userNodeForPackage(ClosedDayService.class);
    }

    public Set<LocalDate> getClosedDays() {
        String raw = prefs.get(PREFS_KEY, "");
        if (raw == null || raw.isBlank()) return new HashSet<>();
        return Arrays.stream(raw.split(SEP))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return LocalDate.parse(s, FMT);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(d -> d != null)
                .collect(Collectors.toSet());
    }

    public void addClosedDay(LocalDate date) {
        Set<LocalDate> set = new HashSet<>(getClosedDays());
        set.add(date);
        save(set);
    }

    public void removeClosedDay(LocalDate date) {
        Set<LocalDate> set = new HashSet<>(getClosedDays());
        set.remove(date);
        save(set);
    }

    public boolean isDayClosed(LocalDate date) {
        return getClosedDays().contains(date);
    }

    private void save(Set<LocalDate> dates) {
        String value = dates.stream()
                .sorted()
                .map(d -> d.format(FMT))
                .collect(Collectors.joining(SEP));
        prefs.put(PREFS_KEY, value);
    }
}
