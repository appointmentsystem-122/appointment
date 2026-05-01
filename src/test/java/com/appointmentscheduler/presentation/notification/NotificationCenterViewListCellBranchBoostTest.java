package com.appointmentscheduler.presentation.notification;

import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.scene.control.ListCell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@link NotificationCenterView} popover list row branches (empty, read vs unread title style)
 * via {@link NotificationCenterView#notificationPopoverListCellUpdateItem} — no JPMS reflection on {@code Cell}.
 */
@ResourceLock("ApplicationContextServices")
class NotificationCenterViewListCellBranchBoostTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @BeforeEach
    void clearNotifications() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                NotificationCenter.getInstance().clear();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    }

    @AfterEach
    void drainFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void notificationPopoverListCell_empty_unread_and_read_branches() {
        ListCell<AppNotification> cell = new ListCell<>();

        NotificationCenterView.notificationPopoverListCellUpdateItem(cell, null, true);
        assertThat(cell.getGraphic()).isNull();
        assertThat(cell.getText()).isNull();

        NotificationCenterView.notificationPopoverListCellUpdateItem(cell, null, false);
        assertThat(cell.getGraphic()).isNull();

        AppNotification unread = new AppNotification(NotificationType.INFO, "U-title", "U-msg");
        NotificationCenterView.notificationPopoverListCellUpdateItem(cell, unread, false);
        assertThat(cell.getGraphic()).isNotNull();

        AppNotification read = new AppNotification(NotificationType.INFO, "R-title", "R-msg");
        read.setRead(true);
        NotificationCenterView.notificationPopoverListCellUpdateItem(cell, read, false);
        assertThat(cell.getGraphic()).isNotNull();
    }
}
