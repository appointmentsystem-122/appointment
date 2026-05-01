package com.appointmentscheduler.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StaffContactMessageTest {

    @Test
    void constructorCoalescesNulls() {
        StaffContactMessage m = new StaffContactMessage(null, null, null, null, null, null);
        assertThat(m.getSubject()).isEmpty();
        assertThat(m.getBody()).isEmpty();
        assertThat(m.getCustomerId()).isEmpty();
        assertThat(m.getCustomerName()).isEmpty();
        assertThat(m.getCustomerEmail()).isEmpty();
        assertThat(m.getReceivedAt()).isNotNull();
    }

    @Test
    void metaLineFormats() {
        StaffContactMessage m = new StaffContactMessage("S", "B",
                LocalDateTime.of(2026, 4, 6, 14, 30), "id", "N", "e@x.com");
        assertThat(m.metaLine()).contains("2026-04-06 14:30").contains("N").contains("e@x.com");
    }

    @Test
    void bodyPreviewBranches() {
        StaffContactMessage empty = new StaffContactMessage("S", "", LocalDateTime.now(), "i", "n", "e");
        assertThat(empty.bodyPreview(10)).isEmpty();

        StaffContactMessage shortB = new StaffContactMessage("S", "hi", LocalDateTime.now(), "i", "n", "e");
        assertThat(shortB.bodyPreview(10)).isEqualTo("hi");

        String longText = "word ".repeat(20).trim();
        StaffContactMessage longB = new StaffContactMessage("S", longText, LocalDateTime.now(), "i", "n", "e");
        assertThat(longB.bodyPreview(8)).endsWith("…").hasSize(9);

        StaffContactMessage multiline = new StaffContactMessage("S", "a\nb\nc", LocalDateTime.now(), "i", "n", "e");
        assertThat(multiline.bodyPreview(50)).doesNotContain("\n");
    }

    @Test
    void equalsAndHashCodeById() {
        StaffContactMessage a = new StaffContactMessage("s", "b", LocalDateTime.now(), "i", "n", "e");
        assertThat(a).isEqualTo(a).isNotEqualTo(null).isNotEqualTo("x");
        StaffContactMessage b = new StaffContactMessage("s", "b", LocalDateTime.now(), "i", "n", "e");
        assertThat(a).isNotEqualTo(b);
    }
}
