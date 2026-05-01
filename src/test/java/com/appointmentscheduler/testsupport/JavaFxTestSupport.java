package com.appointmentscheduler.testsupport;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Shared JavaFX toolkit startup for headless/unit tests that touch presentation code.
 */
public final class JavaFxTestSupport {

    private static volatile boolean started;

    private JavaFxTestSupport() {}

    public static synchronized void initPlatform() {
        if (started) {
            return;
        }
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // toolkit already started
        }
        started = true;
    }

    /**
     * Runs one no-op on the FX thread and waits for it to complete so prior {@code runLater} work drains.
     */
    public static void drainFxQueue(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        if (!latch.await(timeout, unit)) {
            throw new AssertionError("FX task timed out");
        }
    }
}
