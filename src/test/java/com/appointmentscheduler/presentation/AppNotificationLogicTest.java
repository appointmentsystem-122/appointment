package com.appointmentscheduler.presentation;

import com.appointmentscheduler.presentation.notification.AppNotification;
import com.appointmentscheduler.presentation.notification.NotificationPriority;
import com.appointmentscheduler.presentation.notification.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppNotificationLogicTest {

    @Test
    void appNotificationConstructors() {
        AppNotification a = new AppNotification(NotificationType.INFO, "T", "M");
        assertThat(a.getType()).isEqualTo(NotificationType.INFO);
        AppNotification b = new AppNotification(null, null, null, null, "E", "id1");
        assertThat(b.getEntityType()).isEqualTo("E");
        b.setRead(true);
        assertThat(b.isRead()).isTrue();
        AppNotification c = new AppNotification(NotificationType.WARNING, NotificationPriority.HIGH, "x", "y");
        assertThat(c.getPriority()).isEqualTo(NotificationPriority.HIGH);
    }

    @Test
    void enumSmoke() {
        assertThat(NotificationType.SYSTEM).isNotNull();
        assertThat(NotificationPriority.LOW).isNotNull();
    }
}
