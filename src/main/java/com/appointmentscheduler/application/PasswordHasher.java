package com.appointmentscheduler.application;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Enterprise password hashing using BCrypt.
 */
public final class PasswordHasher {

    private static final int COST = 12;

    /**
     * Hash a plain password for storage.
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty())
            throw new IllegalArgumentException("Password cannot be null or empty");
        return BCrypt.withDefaults().hashToString(COST, plainPassword.toCharArray());
    }

    /**
     * Verify a plain password against a stored hash.
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), storedHash);
        return result.verified;
    }
}
