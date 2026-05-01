package com.appointmentscheduler.application;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of login attempt tracking and lockout.
 */
public class InMemoryLoginAttemptService implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private static final class AttemptRecord {
        int count;
        Instant lockUntil;

        void recordFailure() {
            count++;
            if (count >= MAX_ATTEMPTS) {
                lockUntil = Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L);
            }
        }

        boolean isLocked() {
            return lockUntil != null && Instant.now().isBefore(lockUntil);
        }

        int remainingLockMinutes() {
            if (lockUntil == null) return 0;
            long sec = lockUntil.getEpochSecond() - Instant.now().getEpochSecond();
            return sec <= 0 ? 0 : (int) Math.ceil(sec / 60.0);
        }
    }

    private final Map<String, AttemptRecord> byEmail = new ConcurrentHashMap<>();

    @Override
    public void recordFailure(String email) {
        if (email == null || email.isEmpty()) return;
        byEmail.computeIfAbsent(email.toLowerCase(), k -> new AttemptRecord()).recordFailure();
    }

    @Override
    public void clearFailures(String email) {
        if (email != null && !email.isEmpty()) {
            byEmail.remove(email.toLowerCase());
        }
    }

    @Override
    public boolean isLocked(String email) {
        if (email == null || email.isEmpty()) return false;
        AttemptRecord r = byEmail.get(email.toLowerCase());
        return r != null && r.isLocked();
    }

    @Override
    public int getRemainingLockMinutes(String email) {
        if (email == null || email.isEmpty()) return 0;
        AttemptRecord r = byEmail.get(email.toLowerCase());
        return r == null ? 0 : r.remainingLockMinutes();
    }

    @Override
    public int getFailureCount(String email) {
        if (email == null || email.isEmpty()) return 0;
        AttemptRecord r = byEmail.get(email.toLowerCase());
        return r == null ? 0 : r.count;
    }
}
