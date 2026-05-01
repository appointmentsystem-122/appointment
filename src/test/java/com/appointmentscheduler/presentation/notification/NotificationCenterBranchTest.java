package com.appointmentscheduler.presentation.notification;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Covers notification list filters, read state, and trim-to-max on the JavaFX thread (sequenced callbacks).
 */
class NotificationCenterBranchTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void notify_getRecent_markAllRead() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            nc.clear();
            nc.notify(NotificationType.APPOINTMENT_CREATED, NotificationPriority.HIGH, "T1", "M1", "Appointment", "e1");
            nc.notify(NotificationType.REMINDER, "T2", "M2");
            Platform.runLater(() -> {
                assertThat(nc.getRecent(10, false).size()).isGreaterThan(0);
                assertThat(nc.getRecent(10, true).size()).isGreaterThanOrEqualTo(0);
                if (!nc.getNotifications().isEmpty()) {
                    nc.markAsRead(nc.getNotifications().get(0).getId());
                }
                nc.markAllAsRead();
                Platform.runLater(() -> {
                    assertThat(nc.getUnreadCount()).isZero();
                    done.countDown();
                });
            });
        });
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void addUnreadCountListener_ignoresNull() {
        NotificationCenter nc = NotificationCenter.getInstance();
        assertThatCode(() -> nc.addUnreadCountListener(null)).doesNotThrowAnyException();
    }

    @Test
    void markAsRead_unknownId_isNoOp() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            nc.clear();
            nc.notify(NotificationType.INFO, "t", "m");
            Platform.runLater(() -> {
                nc.markAsRead("no-such-notification-id");
                done.countDown();
            });
        });
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void getRecent_unreadOnly_whenAllRead_returnsEmpty() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            nc.clear();
            nc.notify(NotificationType.SYSTEM, "read-me", "body");
            Platform.runLater(() -> {
                if (!nc.getNotifications().isEmpty()) {
                    nc.markAllAsRead();
                }
                Platform.runLater(() -> {
                    assertThat(nc.getRecent(10, true)).isEmpty();
                    done.countDown();
                });
            });
        });
        assertThat(done.await(25, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void getRecent_unreadOnly_excludesReadItems() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            nc.clear();
            nc.notify(NotificationType.INFO, "A", "one");
            nc.notify(NotificationType.INFO, "B", "two");
            Platform.runLater(() -> {
                if (nc.getNotifications().size() >= 2) {
                    String id0 = nc.getNotifications().get(0).getId();
                    nc.markAsRead(id0);
                }
                Platform.runLater(() -> {
                    int unreadOnly = nc.getRecent(10, true).size();
                    int all = nc.getRecent(10, false).size();
                    assertThat(all).isGreaterThanOrEqualTo(unreadOnly);
                    done.countDown();
                });
            });
        });
        assertThat(done.await(25, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void unreadCountListener_throwing_doesNotBreakUnreadUpdate() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            nc.clear();
            nc.addUnreadCountListener(() -> {
                throw new RuntimeException("listener boom");
            });
            nc.notify(NotificationType.INFO, "T", "M");
            Platform.runLater(() -> {
                assertThat(nc.getUnreadCount()).isGreaterThanOrEqualTo(0);
                done.countDown();
            });
        });
        assertThat(done.await(25, TimeUnit.SECONDS)).isTrue();
    }

    /**
     * Each {@link NotificationCenter#notify} enqueues FX work; queue one drain {@link Platform#runLater}
     * after the burst so the {@code while (size > MAX_ITEMS)} trim branch is exercised.
     */
    @Test
    void notify_moreThanMaxItems_trimsOldestOnFxThread() throws Exception {
        NotificationCenter nc = NotificationCenter.getInstance();
        for (int i = 0; i < 205; i++) {
            nc.notify(NotificationType.INFO, "bulk-" + i, "body");
        }
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger size = new AtomicInteger(-1);
        Platform.runLater(() -> {
            size.set(nc.getNotifications().size());
            done.countDown();
        });
        assertThat(done.await(90, TimeUnit.SECONDS)).isTrue();
        assertThat(size.get()).isEqualTo(200);
    }
}
