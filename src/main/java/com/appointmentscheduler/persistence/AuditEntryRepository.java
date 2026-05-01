package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.AuditEntry;

import java.util.List;

/**
 * Repository for audit log entries (append-only, query by user/entity).
 */
public interface AuditEntryRepository {

    /**
     * Append an audit entry. Must not fail under normal operation.
     */
    void append(AuditEntry entry);

    /**
     * Recent entries (newest first), up to max.
     */
    List<AuditEntry> findRecent(int max);

    /**
     * All entries (for export); use with care on large datasets.
     */
    List<AuditEntry> findAll();

    List<AuditEntry> findByUserId(String userId);
    List<AuditEntry> findByEntityType(String entityType);
}
