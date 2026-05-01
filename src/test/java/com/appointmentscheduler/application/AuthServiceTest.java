package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.InMemoryUserRepository;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Authentication: credential validation, lockout integration, audit hooks, and session state.
 */
@DisplayName("AuthService")
class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        authService = new AuthService(userRepository);

        User patient = new User("1", "John", "john@test.com", PasswordHasher.hash("pass"));
        User admin = new Administrator("2", "Admin", "admin@test.com", PasswordHasher.hash("adminpass"));

        userRepository.save(patient);
        userRepository.save(admin);
    }

    @Nested
    @DisplayName("Successful authentication")
    class Success {

        @Test
        @DisplayName("Valid patient credentials set current user and session flags")
        void patientLogin() {
            assertThat(authService.login("john@test.com", "pass")).isTrue();
            assertThat(authService.isLoggedIn()).isTrue();
            assertThat(authService.getCurrentUser()).isNotNull();
            assertThat(authService.getCurrentUser().getName()).isEqualTo("John");
            assertThat(authService.isCurrentUserAdmin()).isFalse();
        }

        @Test
        @DisplayName("Valid admin credentials mark admin session")
        void adminLogin() {
            assertThat(authService.login("admin@test.com", "adminpass")).isTrue();
            assertThat(authService.isCurrentUserAdmin()).isTrue();
        }

        @Test
        @DisplayName("Email is trimmed; matching is case-insensitive via repository")
        void loginTrimsEmail() {
            assertThat(authService.login("  JOHN@TEST.COM  ", "pass")).isTrue();
            assertThat(authService.getCurrentUser().getEmail()).isEqualTo("john@test.com");
        }

        @Test
        @DisplayName("Successful login with lockout service clears failure counter and writes audit")
        void clearsFailuresAndAudits() {
            LoginAttemptService attempts = mock(LoginAttemptService.class);
            AuditLogService audit = mock(AuditLogService.class);
            AuthService svc = new AuthService(userRepository, attempts, audit);

            assertThat(svc.login("john@test.com", "pass")).isTrue();

            verify(attempts).clearFailures("john@test.com");
            verify(audit).log(any(User.class), eq("LOGIN_SUCCESS"), anyString());
        }
    }

    @Nested
    @DisplayName("Failed authentication")
    class Failure {

        @Test
        @DisplayName("Wrong password does not set current user and records failure when lockout service present")
        void wrongPassword() {
            LoginAttemptService attempts = mock(LoginAttemptService.class);
            AuditLogService audit = mock(AuditLogService.class);
            AuthService svc = new AuthService(userRepository, attempts, audit);

            assertThat(svc.login("john@test.com", "wrongpass")).isFalse();
            assertThat(svc.getCurrentUser()).isNull();
            verify(attempts).recordFailure("john@test.com");
            verify(audit).log(eq(""), eq("john@test.com"), eq("LOGIN_FAILED"), anyString());
        }

        @Test
        @DisplayName("Unknown email follows the same failure path as wrong password")
        void unknownEmail() {
            LoginAttemptService attempts = mock(LoginAttemptService.class);
            AuditLogService audit = mock(AuditLogService.class);
            AuthService svc = new AuthService(userRepository, attempts, audit);

            assertThat(svc.login("nobody@here.com", "x")).isFalse();
            verify(attempts).recordFailure("nobody@here.com");
            verify(audit).log(eq(""), eq("nobody@here.com"), eq("LOGIN_FAILED"), anyString());
        }

        @Test
        @DisplayName("Blank or null email is rejected before repository lookup")
        void blankEmail() {
            assertThat(authService.login("", "pass")).isFalse();
            assertThat(authService.login("   ", "pass")).isFalse();
            assertThat(authService.login(null, "pass")).isFalse();
        }
    }

    @Nested
    @DisplayName("Account lockout")
    class Lockout {

        @Test
        @DisplayName("Locked account short-circuits password verification and audits LOGIN_BLOCKED")
        void lockedAccount() {
            LoginAttemptService attempts = mock(LoginAttemptService.class);
            AuditLogService audit = mock(AuditLogService.class);
            when(attempts.isLocked("john@test.com")).thenReturn(true);
            AuthService svc = new AuthService(userRepository, attempts, audit);

            assertThat(svc.login("john@test.com", "pass")).isFalse();
            verify(attempts, never()).clearFailures(anyString());
            verify(audit).log(eq(""), eq("john@test.com"), eq("LOGIN_BLOCKED"), anyString());
        }

        @Test
        @DisplayName("Helpers normalize email and delegate to LoginAttemptService")
        void helpersDelegate() {
            LoginAttemptService attempts = mock(LoginAttemptService.class);
            when(attempts.isLocked("a@b.com")).thenReturn(true);
            when(attempts.getRemainingLockMinutes("a@b.com")).thenReturn(7);
            AuthService svc = new AuthService(userRepository, attempts, mock(AuditLogService.class));

            assertThat(svc.isAccountLocked("A@B.COM")).isTrue();
            assertThat(svc.getRemainingLockMinutes(" a@b.com ")).isEqualTo(7);
            assertThat(svc.getRemainingLockMinutes(null)).isZero();
        }

        @Test
        @DisplayName("getLoginAttemptService exposes the injected implementation")
        void exposesLoginAttemptService() {
            LoginAttemptService attempts = mock(LoginAttemptService.class);
            AuthService svc = new AuthService(userRepository, attempts, null);
            assertThat(svc.getLoginAttemptService()).isSameAs(attempts);
        }
    }

    @Nested
    @DisplayName("Session lifecycle")
    class Session {

        @Test
        @DisplayName("logout clears current user")
        void logout() {
            assertThat(authService.login("john@test.com", "pass")).isTrue();
            authService.logout();
            assertThat(authService.getCurrentUser()).isNull();
            assertThat(authService.isLoggedIn()).isFalse();
        }
    }
}
