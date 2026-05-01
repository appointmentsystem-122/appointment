package com.appointmentscheduler.application;

/**
 * No-op implementation of LoginAttemptService (no lockout, no tracking).
 */
public class NoOpLoginAttemptService implements LoginAttemptService {

    @Override
    public void recordFailure(String email) {}

    @Override
    public void clearFailures(String email) {}

    @Override
    public boolean isLocked(String email) {
        return false;
    }

    @Override
    public int getRemainingLockMinutes(String email) {
        return 0;
    }

    @Override
    public int getFailureCount(String email) {
        return 0;
    }
}
