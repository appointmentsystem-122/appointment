package com.appointmentscheduler.domain.notifiers;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotifiersNotificationMessageTest {

    @Test
    void getters() {
        LocalDateTime t = LocalDateTime.of(2026, 1, 2, 3, 4);
        NotificationMessage m = new NotificationMessage("c", t);
        assertThat(m.getContent()).isEqualTo("c");
        assertThat(m.getSendTime()).isEqualTo(t);
    }

    @Test
    void rejectsNullContent() {
        assertThatThrownBy(() -> new NotificationMessage(null, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSendTime() {
        assertThatThrownBy(() -> new NotificationMessage("x", null))
                .isInstanceOf(NullPointerException.class);
    }
}
