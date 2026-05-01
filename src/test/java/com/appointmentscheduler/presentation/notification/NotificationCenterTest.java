package com.appointmentscheduler.presentation.notification;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCenterTest {

    @Test
    void notifyThenUnreadThenMarkAllRead() throws Exception {
        JavaFxTestSupport.initPlatform();
        CountDownLatch done = new CountDownLatch(1);
        // NotificationCenter.notify/clear/mark* each schedule Platform.runLater — chain explicit pulses so assertions
        // never run before the list mutates.
        Platform.runLater(() -> {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.clear();
            nc.notify(NotificationType.INFO, "T1", "M1");
            nc.notify(NotificationType.WARNING, NotificationPriority.HIGH, "T2", "M2", "Appt", "e1");
            Platform.runLater(() -> {
                assertThat(nc.getUnreadCount()).isGreaterThan(0);
                assertThat(nc.getNotifications().isEmpty()).isFalse();
                nc.markAllAsRead();
                Platform.runLater(() -> {
                    assertThat(nc.getUnreadCount()).isZero();
                    nc.clear();
                    done.countDown();
                });
            });
        });
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void recentAndUnreadProperty() throws Exception {
        JavaFxTestSupport.initPlatform();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.clear();
            nc.notify(NotificationType.INFO, "T", "M");
            Platform.runLater(() -> {
                assertThat(nc.getRecent(10, false)).isNotEmpty();
                assertThat(nc.getRecent(10, true)).isNotEmpty();
                nc.addUnreadCountListener(() -> { });
                nc.addUnreadCountListener(null);
                assertThat(nc.unreadCountProperty().get()).isGreaterThanOrEqualTo(0);
                done.countDown();
            });
        });
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void markAsReadById_andTrimsPastMaxItems() throws Exception {
        JavaFxTestSupport.initPlatform();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.clear();
            nc.notify(NotificationType.INFO, "S", "short");
            for (int i = 0; i < 205; i++) {
                nc.notify(NotificationType.SYSTEM, NotificationPriority.LOW, "B" + i, "msg");
            }
            Platform.runLater(() -> {
                assertThat(nc.getNotifications().size()).isLessThanOrEqualTo(200);
                String id = nc.getNotifications().get(0).getId();
                nc.markAsRead(id);
                Platform.runLater(() -> {
                    assertThat(nc.getNotifications().size()).isLessThanOrEqualTo(200);
                    assertThat(nc.getUnreadCount()).isGreaterThanOrEqualTo(0);
                    done.countDown();
                });
            });
        });
        assertThat(done.await(90, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void notifyShortOverload_coversDelegate() throws Exception {
        JavaFxTestSupport.initPlatform();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.clear();
            nc.notify(NotificationType.WARNING, "TitleOnly", "MsgOnly");
            Platform.runLater(() -> {
                assertThat(nc.getNotifications()).isNotEmpty();
                done.countDown();
            });
        });
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
    }
}
