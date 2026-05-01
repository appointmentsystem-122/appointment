package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PatientInboxEntryTest {

    @Test
    void constructorCoalescesNulls() {
        PatientInboxEntry e = new PatientInboxEntry(null, null, null, null);
        assertThat(e.getTitle()).isEmpty();
        assertThat(e.getBody()).isEmpty();
        assertThat(e.getSenderLabel()).isEqualTo("Organization");
        assertThat(e.getReceivedAt()).isNotNull();
    }

    @Test
    void summaryAndMetaLines() {
        PatientInboxEntry e = new PatientInboxEntry("T", "B",
                LocalDateTime.of(2026, 4, 6, 14, 30), "Sender");
        assertThat(e.summaryLine()).contains(" · T");
        // summaryLine uses LocalDateTime.toString() with 'T' replaced by space (no META_TIME seconds)
        assertThat(e.summaryLine()).contains("2026-04-06 14:30");
        assertThat(e.metaLine()).contains("Sender").contains("2026-04-06 14:30");
    }

    @Test
    void bodyPreviewBranches() {
        PatientInboxEntry empty = new PatientInboxEntry("T", "", LocalDateTime.now(), "S");
        assertThat(empty.bodyPreview(5)).isEmpty();

        PatientInboxEntry p = new PatientInboxEntry("T", "hello world", LocalDateTime.now(), "S");
        assertThat(p.bodyPreview(0)).isEqualTo("hello world"); // maxChars<=0 returns body as-is
        assertThat(p.bodyPreview(5)).isEqualTo("hello…"); // first 5 code units + ellipsis when longer than maxChars

        assertThat(new PatientInboxEntry("T", "x\ny", LocalDateTime.now(), "S").bodyPreview(20)).doesNotContain("\n");
    }

    @Test
    void equalsById() {
        PatientInboxEntry a = new PatientInboxEntry("T", "B", LocalDateTime.now(), "S");
        assertThat(a).isEqualTo(a).isNotEqualTo(null).isNotEqualTo("x");
        PatientInboxEntry b = new PatientInboxEntry("T", "B", LocalDateTime.now(), "S");
        assertThat(a).isNotEqualTo(b);
    }
}
