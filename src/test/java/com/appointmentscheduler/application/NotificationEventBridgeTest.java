package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.events.AppointmentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Event-driven bridge: every {@link AppointmentEvent.Type} must produce a deterministic patient message.
 */
@DisplayName("NotificationEventBridge")
class NotificationEventBridgeTest {

    private NotificationService notificationService;
    private NotificationEventBridge bridge;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        bridge = new NotificationEventBridge(notificationService);
    }

    @Nested
    @DisplayName("Guards")
    class Guards {

        @Test
        @DisplayName("Null event does not notify")
        void nullEvent() {
            bridge.onAppointmentEvent(null);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Event without appointment does not notify")
        void nullAppointment() {
            bridge.onAppointmentEvent(new AppointmentEvent(AppointmentEvent.Type.CREATED, null, null, ""));
            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("Message content by event type")
    class Messages {

        private User patient;
        private InPersonAppointment appt;
        private TimeSlot slot;

        @BeforeEach
        void fixture() {
            patient = new User("p1", "P", "p@t.com", "x");
            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);
            slot = new TimeSlot(start, start.plusHours(1));
            appt = new InPersonAppointment("a1", patient, slot, "Loc");
        }

        @Test
        @DisplayName("CREATED confirms slot text from appointment")
        void created() {
            User actor = new User("adm", "A", "a@t.com", "x");
            bridge.onAppointmentEvent(new AppointmentEvent(AppointmentEvent.Type.CREATED, appt, actor, "ignored"));

            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notifyAllObservers(eq(patient), msg.capture());
            assertThat(msg.getValue()).isEqualTo("Your appointment has been CONFIRMED for " + appt.getTimeSlot());
        }

        @Test
        @DisplayName("MODIFIED uses event details for the new slot text")
        void modified() {
            bridge.onAppointmentEvent(new AppointmentEvent(AppointmentEvent.Type.MODIFIED, appt, patient, "new-slot-detail"));
            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notifyAllObservers(eq(patient), msg.capture());
            assertThat(msg.getValue()).isEqualTo("Your appointment has been MODIFIED to new-slot-detail");
        }

        @Test
        @DisplayName("CANCELLED uses fixed copy")
        void cancelled() {
            bridge.onAppointmentEvent(new AppointmentEvent(AppointmentEvent.Type.CANCELLED, appt, patient, ""));
            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notifyAllObservers(eq(patient), msg.capture());
            assertThat(msg.getValue()).isEqualTo("Your appointment has been CANCELLED.");
        }

        @Test
        @DisplayName("COMPLETED uses fixed copy")
        void completed() {
            bridge.onAppointmentEvent(new AppointmentEvent(AppointmentEvent.Type.COMPLETED, appt, patient, ""));
            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notifyAllObservers(eq(patient), msg.capture());
            assertThat(msg.getValue()).isEqualTo("Your appointment has been marked COMPLETED.");
        }

        @Test
        @DisplayName("REMINDER prefixes details")
        void reminder() {
            bridge.onAppointmentEvent(new AppointmentEvent(AppointmentEvent.Type.REMINDER, appt, patient, "tomorrow 9am"));
            ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notifyAllObservers(eq(patient), msg.capture());
            assertThat(msg.getValue()).isEqualTo("REMINDER: tomorrow 9am");
        }
    }
}
