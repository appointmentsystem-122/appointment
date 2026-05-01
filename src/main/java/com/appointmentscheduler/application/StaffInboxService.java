package com.appointmentscheduler.application;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe in-memory inbox of customer-to-staff contact messages.
 */
public final class StaffInboxService {

    private static final int MAX_GLOBAL = 500;

    private final Deque<StaffContactMessage> messages = new ArrayDeque<>();

    public void append(StaffContactMessage entry) {
        if (entry == null) {
            return;
        }
        synchronized (messages) {
            messages.addLast(entry);
            while (messages.size() > MAX_GLOBAL) {
                messages.removeFirst();
            }
        }
    }

    public List<StaffContactMessage> listRecent(int max) {
        if (max <= 0) {
            return Collections.emptyList();
        }
        synchronized (messages) {
            List<StaffContactMessage> copy = new ArrayList<>(messages);
            Collections.reverse(copy);
            if (copy.size() <= max) {
                return Collections.unmodifiableList(new ArrayList<>(copy));
            }
            return Collections.unmodifiableList(new ArrayList<>(copy.subList(0, max)));
        }
    }
}
