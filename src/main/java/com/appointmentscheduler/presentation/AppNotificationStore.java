package com.appointmentscheduler.presentation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory store of recent app notifications for the notification bar.
 * Thread-safe; capped at MAX_ITEMS.
 */
public class AppNotificationStore {

    private static final int MAX_ITEMS = 50;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public static final class Entry {
        private final String title;
        private final String message;
        private final LocalDateTime at;
        private final boolean isError;

        public Entry(String title, String message, LocalDateTime at, boolean isError) {
            this.title = title != null ? title : "";
            this.message = message != null ? message : "";
            this.at = at != null ? at : LocalDateTime.now();
            this.isError = isError;
        }

        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getTimeFormatted() { return at.format(FMT); }
        public boolean isError() { return isError; }
    }

    public void add(String title, String message) {
        add(title, message, false);
    }

    public void add(String title, String message, boolean isError) {
        entries.add(new Entry(title, message, LocalDateTime.now(), isError));
        while (entries.size() > MAX_ITEMS) {
            entries.remove(0);
        }
    }

    public List<Entry> getRecent(int max) {
        int size = entries.size();
        if (size <= max) return List.copyOf(entries);
        return entries.subList(size - max, size).stream().collect(Collectors.toList());
    }

    public ObservableList<Entry> getObservableRecent(int max) {
        return FXCollections.observableArrayList(getRecent(max));
    }

    public int getUnreadCount() {
        return Math.min(entries.size(), 99);
    }

    public void clear() {
        entries.clear();
    }
}
