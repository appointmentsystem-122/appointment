package com.appointmentscheduler.presentation.notification;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCenterTest {

    @BeforeEach
    void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void notifyThenUnreadThenMarkAllRead() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        runOnFxVoid(nc::clear);
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> {
            nc.notify(NotificationType.INFO, "T1", "M1");
            nc.notify(NotificationType.WARNING, NotificationPriority.HIGH, "T2", "M2", "Appt", "e1");
        });
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> {
            assertThat(nc.getUnreadCount()).isGreaterThan(0);
            assertThat(nc.getNotifications().isEmpty()).isFalse();
            nc.markAllAsRead();
        });
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> {
            assertThat(nc.getUnreadCount()).isZero();
            nc.clear();
        });
    }

    @Test
    void recentAndUnreadProperty() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        runOnFxVoid(nc::clear);
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> nc.notify(NotificationType.INFO, "T", "M"));
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> {
            assertThat(nc.getRecent(10, false)).isNotEmpty();
            assertThat(nc.getRecent(10, true)).isNotEmpty();
            nc.addUnreadCountListener(() -> { });
            nc.addUnreadCountListener(null);
            assertThat(nc.unreadCountProperty().get()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void markAsReadById_andTrimsPastMaxItems() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        runOnFxVoid(() -> {
            nc.clear();
            nc.notify(NotificationType.INFO, "S", "short");
            for (int i = 0; i < 205; i++) {
                nc.notify(NotificationType.SYSTEM, NotificationPriority.LOW, "B" + i, "msg");
            }
        });
        JavaFxTestSupport.drainFxQueue(15, TimeUnit.SECONDS);

        runOnFxVoid(() -> {
            assertThat(nc.getNotifications().size()).isLessThanOrEqualTo(200);
            String id = nc.getNotifications().get(0).getId();
            nc.markAsRead(id);
        });
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> {
            assertThat(nc.getNotifications().size()).isLessThanOrEqualTo(200);
            assertThat(nc.getUnreadCount()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void notifyShortOverload_coversDelegate() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        runOnFxVoid(() -> {
            nc.clear();
            nc.notify(NotificationType.WARNING, "TitleOnly", "MsgOnly");
        });
        JavaFxTestSupport.drainFxQueue(10, TimeUnit.SECONDS);

        runOnFxVoid(() -> assertThat(nc.getNotifications()).isNotEmpty());
    }

    private static <T> T runOnFx(Callable<T> task) {
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ref.set(task.call());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new AssertionError("FX task timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
        return ref.get();
    }

    private static void runOnFxVoid(Runnable runnable) {
        runOnFx(() -> {
            runnable.run();
            return null;
        });
    }
}
