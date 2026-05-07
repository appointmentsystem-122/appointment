package com.appointmentscheduler.presentation.notification;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Focused regression tests for the SonarCloud new-code lines in NotificationCenter. */
@ResourceLock("NotificationCenterSingleton")
class NotificationCenterScreenshotCoverageBoostTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void notify_overloadsUnreadListenersRecentReadClearAndMaxTrim_areCovered() throws Exception {
        NotificationCenter center = NotificationCenter.getInstance();
        center.clear();
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);

        AtomicInteger listenerCalls = new AtomicInteger();
        center.addUnreadCountListener(listenerCalls::incrementAndGet);
        center.addUnreadCountListener(() -> {
            throw new IllegalStateException("listener failure must be swallowed");
        });
        center.addUnreadCountListener(null);

        center.notify(NotificationType.INFO, "basic", "body");
        center.notify(NotificationType.WARNING, NotificationPriority.HIGH, "priority", "body");
        center.notify(NotificationType.ERROR, NotificationPriority.URGENT, "entity", "body", "appointment", "a-1");
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);

        assertThat(center.getUnreadCount()).isEqualTo(3);
        assertThat(center.unreadCountProperty().get()).isEqualTo(3);
        assertThat(listenerCalls.get()).isGreaterThanOrEqualTo(3);
        assertThat(center.getRecent(10, false)).hasSize(3);
        assertThat(center.getRecent(10, true)).hasSize(3);

        ObservableList<AppNotification> readOnly = center.getNotifications();
        assertThatThrownBy(() -> readOnly.add(new AppNotification(NotificationType.INFO, "x", "y")))
                .isInstanceOf(UnsupportedOperationException.class);

        String firstId = readOnly.get(0).getId();
        center.markAsRead(firstId);
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);
        assertThat(center.getUnreadCount()).isEqualTo(2);
        assertThat(center.getRecent(10, true)).allMatch(n -> !n.isRead());

        center.markAsRead("missing-id");
        center.markAllAsRead();
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);
        assertThat(center.getUnreadCount()).isZero();

        for (int i = 0; i < 205; i++) {
            center.notify(NotificationType.INFO, NotificationPriority.NORMAL, "n" + i, "m" + i, null, null);
        }
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);
        assertThat(center.getNotifications()).hasSize(200);

        center.clear();
        JavaFxTestSupport.drainFxQueue(5, TimeUnit.SECONDS);
        assertThat(center.getNotifications()).isEmpty();
        assertThat(center.getUnreadCount()).isZero();
    }
}
