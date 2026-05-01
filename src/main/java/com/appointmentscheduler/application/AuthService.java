package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service handling authentication operations.
 * Integrates login attempt tracking, lockout, and audit logging.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private User currentUser;

    public AuthService(UserRepository userRepository) {
        this(userRepository, null, null);
    }

    public AuthService(UserRepository userRepository, LoginAttemptService loginAttemptService,
                      AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService != null ? loginAttemptService : new NoOpLoginAttemptService();
        this.auditLogService = auditLogService;
    }

    /**
     * Attempts to log in a user with the given credentials.
     * Enforces lockout and records audit for success/failure.
     *
     * @param email    the user email
     * @param password the password
     * @return true if login is successful, false otherwise
     */
    public boolean login(String email, String password) {
        if (email == null || email.isBlank()) return false;
        String normalizedEmail = email.trim().toLowerCase();

        if (loginAttemptService.isLocked(normalizedEmail)) {
            log.warn("Login attempt for locked account: {}", normalizedEmail);
            if (auditLogService != null) {
                auditLogService.log("", normalizedEmail, "LOGIN_BLOCKED",
                    "Account temporarily locked due to too many failed attempts");
            }
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordHasher.verify(password, user.getPassword())) {
                loginAttemptService.clearFailures(normalizedEmail);
                this.currentUser = user;
                if (auditLogService != null) {
                    auditLogService.log(user, "LOGIN_SUCCESS", "Signed in from " + normalizedEmail);
                }
                log.info("Login successful for user: {}", user.getEmail());
                return true;
            }
        }

        loginAttemptService.recordFailure(normalizedEmail);
        if (auditLogService != null) {
            auditLogService.log("", normalizedEmail, "LOGIN_FAILED", "Invalid credentials");
        }
        log.warn("Login failed for email: {}", normalizedEmail);
        return false;
    }

    /**
     * Returns true if the given email is currently locked due to failed attempts.
     */
    public boolean isAccountLocked(String email) {
        return email != null && loginAttemptService.isLocked(email.trim().toLowerCase());
    }

    /**
     * Returns remaining lockout time in minutes for the given email.
     */
    public int getRemainingLockMinutes(String email) {
        return email == null ? 0 : loginAttemptService.getRemainingLockMinutes(email.trim().toLowerCase());
    }

    public LoginAttemptService getLoginAttemptService() {
        return loginAttemptService;
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Retrieves the currently logged-in user.
     * @return the current User or null if no user is logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if the current tracked user is logged in.
     * @return true if logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks if the currently logged in user is an administrator.
     * @return true if admin
     */
    public boolean isCurrentUserAdmin() {
        return isLoggedIn() && currentUser.isAdmin();
    }

    /**
     * Retrieves the UserRepository.
     * @return the UserRepository instance
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }
}
