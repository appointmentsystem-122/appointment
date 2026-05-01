package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AuditEntryRepository;
import com.appointmentscheduler.persistence.InMemoryAuditEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enterprise audit trail: records user actions for compliance and debugging.
 * Delegates to AuditEntryRepository (in-memory or JDBC).
 */
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditEntryRepository repository;

    public AuditLogService() {
        this(null);
    }

    public AuditLogService(AuditEntryRepository repository) {
        this.repository = repository != null ? repository : new InMemoryAuditEntryRepository();
    }

    public void log(String action, String details) {
        log(null, null, action, details);
    }

    public void log(User user, String action, String details) {
        String uid = user != null ? user.getId() : "";
        String name = user != null ? user.getName() : "System";
        log(uid, name, action, details);
    }

    public void log(String userId, String userName, String action, String details) {
        AuditEntry e = new AuditEntry(LocalDateTime.now(), userId, userName, action, details);
        repository.append(e);
        log.debug("Audit: {} | {} | {}", action, userName, details);
    }

    public List<AuditEntry> getRecentEntries(int max) {
        return repository.findRecent(max);
    }

    public List<AuditEntry> getAllEntries() {
        return repository.findAll();
    }

    public void log(User user, String action, String details, String entityType, String entityId,
                    String oldValue, String newValue) {
        String uid = user != null ? user.getId() : "";
        String name = user != null ? user.getName() : "System";
        AuditEntry e = new AuditEntry(LocalDateTime.now(), uid, name, action, details,
                entityType, entityId, oldValue, newValue);
        repository.append(e);
        log.debug("Audit: {} | {} | {} | entity={}", action, name, details, entityId);
    }

    public List<AuditEntry> getEntriesByEntityType(String entityType) {
        return repository.findByEntityType(entityType);
    }

    public List<AuditEntry> getEntriesByUser(String userId) {
        return repository.findByUserId(userId);
    }
}
