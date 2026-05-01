package com.appointmentscheduler.presentation.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppNotificationTest {

    @Test
    void constructor_singleArg_nullType_defaultsToInfo() {
        AppNotification n = new AppNotification(null, "t", "m");
        assertThat(n.getType()).isEqualTo(NotificationType.INFO);
        assertThat(n.getPriority()).isEqualTo(NotificationPriority.NORMAL);
    }

    @Test
    void constructor_twoArg_nullPriority_defaultsToNormal() {
        AppNotification n = new AppNotification(NotificationType.SUCCESS, null, "t", "m");
        assertThat(n.getPriority()).isEqualTo(NotificationPriority.NORMAL);
    }

    @Test
    void constructor_nullTitleAndMessage_becomeEmptyStrings() {
        AppNotification n = new AppNotification(NotificationType.ERROR, NotificationPriority.HIGH, null, null);
        assertThat(n.getTitle()).isEmpty();
        assertThat(n.getMessage()).isEmpty();
    }

    @Test
    void sixArg_constructor_keepsEntityFields() {
        AppNotification n = new AppNotification(
                NotificationType.APPOINTMENT_CREATED,
                NotificationPriority.LOW,
                "a",
                "b",
                "ENTITY",
                "e1");
        assertThat(n.getEntityType()).isEqualTo("ENTITY");
        assertThat(n.getEntityId()).isEqualTo("e1");
    }

    @Test
    void timeFormatted_and_dateTimeFormatted_nonEmpty() {
        AppNotification n = new AppNotification(NotificationType.INFO, "t", "m");
        assertThat(n.getTimeFormatted()).matches("\\d{2}:\\d{2}");
        assertThat(n.getDateTimeFormatted()).matches("\\d{2}/\\d{2} \\d{2}:\\d{2}");
    }

    @Test
    void setRead_togglesState() {
        AppNotification n = new AppNotification(NotificationType.INFO, "t", "m");
        assertThat(n.isRead()).isFalse();
        n.setRead(true);
        assertThat(n.isRead()).isTrue();
    }

    @Test
    void equals_contract() {
        AppNotification n = new AppNotification(NotificationType.INFO, "t", "m");
        assertThat(n.equals(n)).isTrue();
        assertThat(n.equals(null)).isFalse();
        assertThat(n.equals("other")).isFalse();
        AppNotification other = new AppNotification(NotificationType.INFO, "t", "m");
        assertThat(n.equals(other)).isFalse();
        assertThat(n.hashCode()).isEqualTo(n.hashCode());
    }
}
