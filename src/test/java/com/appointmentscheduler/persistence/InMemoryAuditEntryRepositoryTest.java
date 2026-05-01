package com.appointmentscheduler.persistence;

import com.appointmentscheduler.domain.AuditEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAuditEntryRepositoryTest {

    @Test
    void appendNullIgnored() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        repo.append(null);
        assertThat(repo.findAll()).isEmpty();
    }

    @Test
    void findRecentAndFilters() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        for (int i = 0; i < 5; i++) {
            repo.append(new AuditEntry(LocalDateTime.now(), "u1", "N", "A" + i, "d"));
        }
        repo.append(new AuditEntry(LocalDateTime.now(), "u2", "N2", "X", "d", "T", "e1", "o", "n"));
        assertThat(repo.findRecent(2)).hasSize(2);
        assertThat(repo.findByUserId("u2")).hasSize(1);
        assertThat(repo.findByEntityType("T")).hasSize(1);
        assertThat(repo.findByEntityType("")).size().isGreaterThanOrEqualTo(6);
    }

    @Test
    void trimsOverMax() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        for (int i = 0; i < 2005; i++) {
            repo.append(new AuditEntry(LocalDateTime.now(), "", "S", "act", "d"));
        }
        assertThat(repo.findAll().size()).isLessThanOrEqualTo(2000);
    }

    @Test
    void findRecent_whenMaxExceedsSize_returnsAll() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        repo.append(new AuditEntry(LocalDateTime.now(), "u1", "N", "A", "d"));
        assertThat(repo.findRecent(10)).hasSize(1);
    }

    @Test
    void findRecent_whenSizeEqualsMax_usesFullListBranch() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        repo.append(new AuditEntry(LocalDateTime.of(2026, 1, 1, 8, 0), "a", "N", "A1", "d"));
        repo.append(new AuditEntry(LocalDateTime.of(2026, 1, 1, 9, 0), "b", "N", "A2", "d"));
        assertThat(repo.findRecent(2)).hasSize(2);
        assertThat(repo.findRecent(2).get(0).getAction()).isEqualTo("A1");
    }

    @Test
    void nullAndEmptyFilters_returnAll() {
        InMemoryAuditEntryRepository repo = new InMemoryAuditEntryRepository();
        repo.append(new AuditEntry(LocalDateTime.now(), "u1", "N", "A", "d", "APPT", "e1", "o", "n"));
        assertThat(repo.findByUserId(null)).hasSize(1);
        assertThat(repo.findByUserId("")).hasSize(1);
        assertThat(repo.findByEntityType(null)).hasSize(1);
    }
}
