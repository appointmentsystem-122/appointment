package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEntryTest {

    @Test
    void shortAndFullConstructor() {
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 12, 0);
        AuditEntry a = new AuditEntry(ts, "u", "N", "act", "det");
        assertThat(a.getTimestampFormatted()).contains("2026");
        AuditEntry b = new AuditEntry(null, null, null, null, null, "E", "id", "old", "new");
        assertThat(b.getUserId()).isEmpty();
        assertThat(b.getEntityType()).isEqualTo("E");
        assertThat(b.getOldValue()).isEqualTo("old");
    }
}
