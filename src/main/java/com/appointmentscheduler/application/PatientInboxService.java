package com.appointmentscheduler.application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-app inbox per user (enterprise: later backed by DB or message bus).
 */
public final class PatientInboxService {

    private static final int MAX_PER_USER = 100;

    private final ConcurrentHashMap<String, Deque<PatientInboxEntry>> byUserId = new ConcurrentHashMap<>();

    public void append(String userId, PatientInboxEntry entry) {
        if (userId == null || entry == null) return;
        Deque<PatientInboxEntry> q = byUserId.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(entry);
            while (q.size() > MAX_PER_USER) {
                q.removeFirst();
            }
        }
    }

    public List<PatientInboxEntry> listRecent(String userId, int max) {
        if (userId == null) return List.of();
        Deque<PatientInboxEntry> q = byUserId.get(userId);
        if (q == null) return List.of();
        synchronized (q) {
            List<PatientInboxEntry> copy = new ArrayList<>(q);
            Collections.reverse(copy);
            if (copy.size() <= max) return List.copyOf(copy);
            return List.copyOf(copy.subList(0, max));
        }
    }
}
