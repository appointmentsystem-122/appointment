package com.appointmentscheduler.testsupport;

import javafx.application.Platform;
import org.opentest4j.TestAbortedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Shared JavaFX toolkit startup for headless/unit tests that touch presentation code.
 */
public final class JavaFxTestSupport {

    private static volatile boolean started;

    private JavaFxTestSupport() {}

    public static synchronized void initPlatform() {
        if (started) {
            verifyFxPulse();
            return;
        }
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // toolkit already started
        } catch (Throwable startupFailure) {
            throw new TestAbortedException("JavaFX platform is unavailable in this environment", startupFailure);
        }
        verifyFxPulse();
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

    public static void runOnFxThread(Runnable action) {
        initPlatform();
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        try {
            task.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for FX task", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("FX task failed", cause);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new AssertionError("FX task timed out", e);
        }
    }

    private static void verifyFxPulse() {
        CountDownLatch pulse = new CountDownLatch(1);
        try {
            Platform.runLater(pulse::countDown);
            if (!pulse.await(3, TimeUnit.SECONDS)) {
                throw new TestAbortedException("JavaFX event loop is not responsive in this environment");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestAbortedException("Interrupted while waiting for JavaFX initialization", e);
        } catch (IllegalStateException e) {
            throw new TestAbortedException("JavaFX toolkit is not initialized", e);
        }
    }
}
