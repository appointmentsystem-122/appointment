package com.appointmentscheduler.presentation;

import com.appointmentscheduler.presentation.notification.NotificationType;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;

class ToastNotificationTest {

    @Test
    void show_withNullOwner_returnsImmediately() {
        assertThatCode(() -> ToastNotification.show("hello", null, false)).doesNotThrowAnyException();
    }

    @Test
    void show_stringOverload_errorFlag_usesErrorType() throws Exception {
        JavaFxTestSupport.initPlatform();
        Stage owner = awaitStage();
        assertThatCode(() -> ToastNotification.show("msg", owner, true)).doesNotThrowAnyException();
        drainFxQueue();
    }

    @Test
    void show_withOwner_doesNotThrow() throws Exception {
        JavaFxTestSupport.initPlatform();

        // Stage must be created on FX thread.
        CountDownLatch stageLatch = new CountDownLatch(1);
        final javafx.stage.Stage[] ownerRef = new javafx.stage.Stage[1];
        Platform.runLater(() -> {
            javafx.stage.Stage owner = new javafx.stage.Stage();
            owner.setWidth(800);
            owner.setHeight(600);
            ownerRef[0] = owner;
            stageLatch.countDown();
        });
        if (!stageLatch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("FX stage was not created in time");
        }

        assertThatCode(() -> ToastNotification.show(ownerRef[0], com.appointmentscheduler.presentation.notification.NotificationType.INFO, "T", "M"))
                .doesNotThrowAnyException();

        // Let queued runLater callbacks execute.
        drainFxQueue();
    }

    @Test
    void show_coversToastStyleSwitchBranches() throws Exception {
        JavaFxTestSupport.initPlatform();
        Stage owner = awaitStage();

        NotificationType[] types = {
                NotificationType.SUCCESS,
                NotificationType.WARNING,
                NotificationType.ERROR,
                NotificationType.APPOINTMENT_CREATED,
                null
        };
        for (NotificationType t : types) {
            assertThatCode(() -> ToastNotification.show(owner, t, null, "body")).doesNotThrowAnyException();
        }
        assertThatCode(() -> ToastNotification.show(owner, NotificationType.INFO, "Title", "body")).doesNotThrowAnyException();

        drainFxQueue();
    }

    @Test
    void show_emptyTitle_skipsTitlePrefix() throws Exception {
        JavaFxTestSupport.initPlatform();
        Stage owner = awaitStage();
        assertThatCode(() -> ToastNotification.show(owner, NotificationType.INFO, "", "body only"))
                .doesNotThrowAnyException();
        drainFxQueue();
    }

    private static Stage awaitStage() throws InterruptedException {
        CountDownLatch stageLatch = new CountDownLatch(1);
        final Stage[] ownerRef = new Stage[1];
        Platform.runLater(() -> {
            Stage owner = new Stage();
            owner.setWidth(400);
            owner.setHeight(300);
            ownerRef[0] = owner;
            stageLatch.countDown();
        });
        if (!stageLatch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("FX stage was not created in time");
        }
        return ownerRef[0];
    }

    private static void drainFxQueue() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(done::countDown);
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("FX callbacks did not execute in time");
        }
    }
}

