package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {

    @Test
    void toStringContainsParts() {
        NotificationMessage m = new NotificationMessage("Subj", "Body");
        assertThat(m.getSubject()).isEqualTo("Subj");
        assertThat(m.getContent()).isEqualTo("Body");
        assertThat(m.getTimestamp()).isNotNull();
        assertThat(m.toString()).contains("Subj").contains("Body");
    }
}
