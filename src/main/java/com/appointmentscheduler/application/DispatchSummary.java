package com.appointmentscheduler.application;

/**
 * Result of a bulk messaging operation (admin broadcast or patient contact).
 */
public final class DispatchSummary {

    private final int successCount;
    private final int failureCount;
    private final int skipped;
    private final String message;
    private final boolean forbidden;

    private DispatchSummary(int successCount, int failureCount, int skipped, String message, boolean forbidden) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.skipped = skipped;
        this.message = message;
        this.forbidden = forbidden;
    }

    public static DispatchSummary forbidden() {
        return new DispatchSummary(0, 0, 0, "Not authorized for this action.", true);
    }

    public static DispatchSummary empty(String reason) {
        return new DispatchSummary(0, 0, 0, reason, false);
    }

    public static DispatchSummary of(int success, int failure, int skipped, String message) {
        return new DispatchSummary(success, failure, skipped, message, false);
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getSkipped() {
        return skipped;
    }

    public String getMessage() {
        return message;
    }

    public boolean isForbidden() {
        return forbidden;
    }
}
