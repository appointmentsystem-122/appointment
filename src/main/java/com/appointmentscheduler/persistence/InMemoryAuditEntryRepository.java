package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.AuditEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory audit log (capped). For tests or when DB is disabled.
 */
public class InMemoryAuditEntryRepository implements AuditEntryRepository {

    private static final int MAX = 2000;
    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void append(AuditEntry entry) {
        if (entry != null) {
            entries.add(entry);
            while (entries.size() > MAX) entries.remove(0);
        }
    }

    @Override
    public List<AuditEntry> findRecent(int max) {
        int size = entries.size();
        if (size <= max) return new ArrayList<>(entries);
        return new ArrayList<>(entries.subList(size - max, size));
    }

    @Override
    public List<AuditEntry> findAll() {
        return new ArrayList<>(entries);
    }

    @Override
    public List<AuditEntry> findByUserId(String userId) {
        if (userId == null || userId.isEmpty()) return findAll();
        return entries.stream().filter(e -> userId.equals(e.getUserId())).collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> findByEntityType(String entityType) {
        if (entityType == null || entityType.isEmpty()) return findAll();
        return entries.stream().filter(e -> entityType.equals(e.getEntityType())).collect(Collectors.toList());
    }
}
