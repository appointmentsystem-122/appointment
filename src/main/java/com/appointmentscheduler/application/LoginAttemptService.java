package com.appointmentscheduler.application;

/**
 * Tracks login attempts and enforces account lockout after too many failures.
 */
public interface LoginAttemptService {

    /**
     * Records a failed login attempt for the given email.
     * @param email the attempted email
     */
    void recordFailure(String email);

    /**
     * Clears any failure count for the given email (e.g. after successful login).
     * @param email the email
     */
    void clearFailures(String email);

    /**
     * Returns true if the account is currently locked due to too many failed attempts.
     * @param email the email
     * @return true if locked
     */
    boolean isLocked(String email);

    /**
     * Returns remaining lockout time in minutes, or 0 if not locked.
     */
    int getRemainingLockMinutes(String email);

    /**
     * Returns the number of failed attempts for the given email.
     */
    int getFailureCount(String email);
}
