package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.notifiers.Observer;
import com.appointmentscheduler.persistence.InMemoryUserRepository;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers {@link ClosedDayBroadcast} branches (null guards + patient fan-out) without starting the full UI.
 */
class ClosedDayBroadcastTest {

    private NotificationService notificationService;
    private Observer attachedObserver;
    private AuthService authService;
    private InMemoryUserRepository userRepository;

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @BeforeEach
    void wireContext() {
        notificationService = new NotificationService();
        attachedObserver = mock(Observer.class);
        notificationService.attach(attachedObserver);
        authService = mock(AuthService.class);
        userRepository = new InMemoryUserRepository();
        userRepository.save(new User("p1", "Pat", "p@x.com", "x"));
        userRepository.save(new Administrator("a1", "Admin", "a@x.com", "x"));
        when(authService.getUserRepository()).thenReturn(userRepository);

        ApplicationContext.setNotificationService(notificationService);
        ApplicationContext.setAuthService(authService);
    }

    @AfterEach
    void clearContext() {
        ApplicationContext.setNotificationService(null);
        ApplicationContext.setAuthService(null);
    }

    @Test
    void broadcastDayClosed_nullIsNoOp() {
        ClosedDayBroadcast.broadcastDayClosed(null);
        verify(attachedObserver, never()).notify(any(), anyString());
    }

    @Test
    void broadcastDayReopened_nullIsNoOp() {
        ClosedDayBroadcast.broadcastDayReopened(null);
        verify(attachedObserver, never()).notify(any(), anyString());
    }

    @Test
    void pushToAllPatients_skipsWhenNotificationServiceNull() {
        ApplicationContext.setNotificationService(null);
        ClosedDayBroadcast.broadcastDayClosed(LocalDate.of(2026, 4, 8));
        ApplicationContext.setNotificationService(notificationService);
    }

    @Test
    void pushToAllPatients_skipsWhenAuthNull() {
        ApplicationContext.setAuthService(null);
        ClosedDayBroadcast.broadcastDayClosed(LocalDate.of(2026, 4, 8));
        ApplicationContext.setAuthService(authService);
    }

    @Test
    void pushToAllPatients_skipsWhenUserRepositoryNull() {
        when(authService.getUserRepository()).thenReturn(null);
        ClosedDayBroadcast.broadcastDayClosed(LocalDate.of(2026, 4, 8));
    }

    @Test
    void broadcastDayClosed_notifiesNonAdminUsers() {
        ClosedDayBroadcast.broadcastDayClosed(LocalDate.of(2026, 4, 9));
        verify(attachedObserver, atLeastOnce()).notify(any(User.class), anyString());
    }

    @Test
    void broadcastDayReopened_notifiesNonAdminUsers() {
        ClosedDayBroadcast.broadcastDayReopened(LocalDate.of(2026, 4, 10));
        verify(attachedObserver, atLeastOnce()).notify(any(User.class), anyString());
    }
}
