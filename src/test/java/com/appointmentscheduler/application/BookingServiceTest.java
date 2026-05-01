package com.appointmentscheduler.application;

import com.appointmentscheduler.application.email.EmailNotificationPort;
import com.appointmentscheduler.domain.*;
import com.appointmentscheduler.domain.authorization.Permission;
import com.appointmentscheduler.domain.events.AppointmentEvent;
import com.appointmentscheduler.domain.events.AppointmentEventPublisher;
import com.appointmentscheduler.domain.policy.BookingPolicies;
import com.appointmentscheduler.domain.policy.Policy;
import com.appointmentscheduler.domain.rules.BookingRuleStrategy;
import com.appointmentscheduler.persistence.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingService}: booking rules, authorization, policies, schedule conflicts,
 * and notification/event branches. Assertions use production message constants where the API is stable.
 */
@DisplayName("BookingService")
class BookingServiceTest {

    private static final String MSG_INVALID_APPT_OR_SLOT = "Invalid appointment or time slot.";
    private static final String MSG_NO_PERM_BOOK = "No permission to book.";
    private static final String MSG_POLICY_BOOKING_DENIED = "Booking policy did not allow this reservation.";
    private static final String MSG_SLOT_TAKEN_OR_OVERLAP = "This slot is already taken or overlaps with another appointment.";
    private static final String PREFIX_CUTOFF = "This slot is too soon.";
    private static final String MSG_WORKING_HOURS = "This time is outside working hours (check business hours).";
    private static final String MSG_DURATION = "Duration or capacity rule failed.";

    private AppointmentRepository appointmentRepository;
    private NotificationService notificationService;
    private ScheduleService scheduleService;
    private BookingRuleStrategy passesAllRule;
    private BookingService bookingService;
    private EmailNotificationPort emailNotificationPort;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        notificationService = mock(NotificationService.class);
        emailNotificationPort = mock(EmailNotificationPort.class);
        scheduleService = new ScheduleService(appointmentRepository);
        passesAllRule = mock(BookingRuleStrategy.class);
        when(passesAllRule.isValid(any(Appointment.class))).thenReturn(true);
        when(appointmentRepository.findBlockingBookingsForPatient(anyString())).thenReturn(Collections.emptyList());

        bookingService = new BookingService(
                appointmentRepository,
                notificationService,
                scheduleService,
                List.of(passesAllRule),
                null,
                null, null, null, null,
                emailNotificationPort);
    }

    private BookingService withEventPublisher(AppointmentEventPublisher publisher) {
        return new BookingService(
                appointmentRepository,
                notificationService,
                scheduleService,
                List.of(passesAllRule),
                new AuditLogService(),
                null,
                null,
                publisher,
                null,
                emailNotificationPort);
    }

    private BookingService withPermission(PermissionService perm) {
        return new BookingService(
                appointmentRepository, notificationService, scheduleService,
                List.of(passesAllRule), new AuditLogService(),
                perm, null, null, null, emailNotificationPort);
    }

    private BookingService withPolicy(PolicyEngine engine) {
        return new BookingService(
                appointmentRepository, notificationService, scheduleService,
                List.of(passesAllRule), new AuditLogService(),
                null, engine, null, null, emailNotificationPort);
    }

    private static TimeSlot slotStartingDaysFromNow(int days) {
        LocalDateTime start = LocalDateTime.now().plusDays(days).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return new TimeSlot(start, start.plusHours(1));
    }

    private static User patient(String id) {
        return new User(id, "Pat", id + "@test.com", "x");
    }

    @Nested
    @DisplayName("Booking and scheduling")
    class Booking {

        @Test
        @DisplayName("bookAppointment mirrors tryBookWithReason: success confirms and notifies when no event publisher")
        void book_success_confirmsNotifiesAndAudits() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment(p, slot, "Room 1");

            assertTrue(bookingService.bookAppointment(appt));

            assertThat(appt.getStatus()).isEqualTo("CONFIRMED");
            verify(appointmentRepository).save(appt);
            verify(notificationService).notifyAllObservers(eq(p), contains("CONFIRMED"));
            verify(emailNotificationPort).sendBookingConfirmation(appt);
        }

        @Test
        @DisplayName("booking still succeeds when confirmation email throws")
        void book_success_emailExceptionDoesNotFailBooking() {
            doThrow(new RuntimeException("smtp unavailable")).when(emailNotificationPort).sendBookingConfirmation(any(Appointment.class));
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment(p, slot, "Room 1");

            assertTrue(bookingService.bookAppointment(appt));

            assertThat(appt.getStatus()).isEqualTo("CONFIRMED");
            verify(emailNotificationPort).sendBookingConfirmation(appt);
        }

        @Test
        @DisplayName("bookAppointment returns false when validation rule fails")
        void book_ruleFails_noPersist() {
            when(passesAllRule.isValid(any(Appointment.class))).thenReturn(false);
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertFalse(bookingService.bookAppointment(appt));

            verify(appointmentRepository, never()).save(any());
            verifyNoInteractions(notificationService);
            verifyNoInteractions(emailNotificationPort);
        }

        @Test
        @DisplayName("tryBookWithReason(null, …) returns the canonical invalid message")
        void tryBook_nullAppointment_invalidMessage() {
            assertThat(bookingService.tryBookWithReason(null, null)).contains(MSG_INVALID_APPT_OR_SLOT);
        }

        @Test
        @DisplayName("tryBookWithReason rejects null time slot with the same message as null appointment")
        void tryBook_nullTimeSlot_invalidMessage() {
            User p = patient("1");
            Appointment bad = mock(Appointment.class);
            when(bad.getPatient()).thenReturn(p);
            when(bad.getTimeSlot()).thenReturn(null);

            assertThat(bookingService.tryBookWithReason(bad, p)).contains(MSG_INVALID_APPT_OR_SLOT);
        }

        @Test
        @DisplayName("SecurityException from PermissionService maps to 'No permission to book.'")
        void tryBook_permissionDenied_exactMessage() {
            PermissionService perm = mock(PermissionService.class);
            doThrow(new SecurityException("denied")).when(perm).requirePermission(any(User.class), eq(Permission.BOOK_APPOINTMENT));
            BookingService svc = withPermission(perm);

            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).contains(MSG_NO_PERM_BOOK);
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("PolicyEngine denial returns the stable booking-policy message (not the policy's internal text)")
        void tryBook_policyEngineDeny_stableMessage() {
            PolicyEngine engine = mock(PolicyEngine.class);
            when(engine.evaluate(any(BookingPolicies.BookingContext.class))).thenReturn(Policy.PolicyResult.deny("internal reason"));
            BookingService svc = withPolicy(engine);

            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).contains(MSG_POLICY_BOOKING_DENIED);
        }

        @Test
        @DisplayName("Overlapping CONFIRMED appointment in the master schedule blocks the slot")
        void tryBook_overlapWithActiveAppointment_denies() {
            User p = patient("1");
            TimeSlot taken = slotStartingDaysFromNow(3);
            InPersonAppointment existing = new InPersonAppointment("e1", p, taken, "A");
            existing.setStatus("CONFIRMED");
            when(appointmentRepository.findAll()).thenReturn(List.of(existing));

            ScheduleService ss = new ScheduleService(appointmentRepository);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, ss,
                    List.of(passesAllRule), null);

            InPersonAppointment clash = new InPersonAppointment(p, taken, "B");
            assertThat(svc.tryBookWithReason(clash, p)).contains(MSG_SLOT_TAKEN_OR_OVERLAP);
            verify(appointmentRepository, never()).save(clash);
        }

        @Test
        @DisplayName("CANCELLED appointments are ignored for overlap; the same slot can be booked again")
        void tryBook_overlapOnlyCancelled_allowsBook() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(4);
            InPersonAppointment cancelled = new InPersonAppointment("old", p, slot, "Room");
            cancelled.setStatus("CANCELLED");
            when(appointmentRepository.findAll()).thenReturn(List.of(cancelled));

            ScheduleService ss = new ScheduleService(appointmentRepository);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, ss,
                    List.of(passesAllRule), null);

            InPersonAppointment neu = new InPersonAppointment(p, slot, "Room");
            assertThat(svc.tryBookWithReason(neu, p)).isEmpty();
            verify(appointmentRepository).save(neu);
        }

        @Test
        @DisplayName("Self-service patient with an open blocking appointment cannot book again")
        void tryBook_patientBlockingOpenAnother_selfServiceDenied() {
            User p = patient("1");
            TimeSlot first = slotStartingDaysFromNow(5);
            InPersonAppointment blocking = new InPersonAppointment("blk", p, first, "X");
            blocking.setStatus("CONFIRMED");
            when(appointmentRepository.findBlockingBookingsForPatient("1")).thenReturn(List.of(blocking));

            InPersonAppointment next = new InPersonAppointment(p, slotStartingDaysFromNow(6), "Y");
            assertThat(bookingService.tryBookWithReason(next, p))
                    .contains(BookingFailureCodes.OPEN_APPOINTMENT_NOT_COMPLETED);
        }

        @Test
        @DisplayName("Receptionist staff bypasses the one-open-booking check for the patient")
        void tryBook_receptionistBypassesBlocking() {
            User p = patient("1");
            TimeSlot first = slotStartingDaysFromNow(7);
            InPersonAppointment blocking = new InPersonAppointment("blk", p, first, "X");
            blocking.setStatus("CONFIRMED");
            when(appointmentRepository.findBlockingBookingsForPatient("1")).thenReturn(List.of(blocking));

            InPersonAppointment next = new InPersonAppointment(p, slotStartingDaysFromNow(8), "Y");
            ReceptionistUser staff = new ReceptionistUser("r1", "R", "r@x.com", "p");
            assertThat(bookingService.tryBookWithReason(next, staff)).isEmpty();
            verify(appointmentRepository).save(next);
            verify(emailNotificationPort).sendBookingConfirmation(next);
        }

        @Test
        @DisplayName("Administrator also bypasses blocking (same staff rule as receptionist)")
        void tryBook_administratorBypassesBlocking() {
            User p = patient("1");
            TimeSlot first = slotStartingDaysFromNow(9);
            when(appointmentRepository.findBlockingBookingsForPatient("1"))
                    .thenReturn(List.of(new InPersonAppointment("b", p, first, "X")));

            InPersonAppointment next = new InPersonAppointment(p, slotStartingDaysFromNow(10), "Y");
            Administrator admin = new Administrator("a1", "A", "a@x.com", "p");
            assertThat(bookingService.tryBookWithReason(next, admin)).isEmpty();
            verify(appointmentRepository).save(next);
            verify(emailNotificationPort).sendBookingConfirmation(next);
        }

        @Test
        @DisplayName("Persistence failure surfaces a database-oriented message (root cause)")
        void tryBook_saveThrows_includesCauseMessageWhenPresent() {
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");
            RuntimeException cause = new RuntimeException("db down");
            doThrow(new RuntimeException("wrapper", cause)).when(appointmentRepository).save(appt);

            assertThat(bookingService.tryBookWithReason(appt, p)).hasValueSatisfying(msg ->
                    assertThat(msg).contains("Could not save to database").contains("db down"));
        }

        @Test
        @DisplayName("Persistence failure without cause falls back to exception message")
        void tryBook_saveThrows_noCause_usesExceptionMessage() {
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");
            doThrow(new RuntimeException("plain")).when(appointmentRepository).save(appt);

            assertThat(bookingService.tryBookWithReason(appt, p)).hasValueSatisfying(msg ->
                    assertThat(msg).contains("Could not save to database").contains("plain"));
        }

        @Test
        @DisplayName("CREATED event publishes when AppointmentEventPublisher is configured")
        void tryBook_withEventPublisher_publishesCreated() {
            AppointmentEventPublisher publisher = mock(AppointmentEventPublisher.class);
            BookingService svc = withEventPublisher(publisher);
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).isEmpty();

            ArgumentCaptor<AppointmentEvent> cap = ArgumentCaptor.forClass(AppointmentEvent.class);
            verify(publisher).publish(cap.capture());
            assertThat(cap.getValue().getType()).isEqualTo(AppointmentEvent.Type.CREATED);
            verify(notificationService, never()).notifyAllObservers(any(), anyString());
            verify(emailNotificationPort).sendBookingConfirmation(appt);
        }

        @Test
        @DisplayName("Unclassified rule failure includes the rule class name for supportability")
        void tryBook_genericRuleFailure_includesRuleName() {
            when(passesAllRule.isValid(any(Appointment.class))).thenReturn(true);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule, new ArbitraryFailureRule()), null);
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).hasValueSatisfying(msg ->
                    assertThat(msg).contains("ArbitraryFailureRule"));
        }
    }

    @Nested
    @DisplayName("Named rule strategies (message branches)")
    class RuleMessageBranches {

        @Test
        @DisplayName("Cutoff rule name maps to cutoff user message")
        void cutoffMessage() {
            when(passesAllRule.isValid(any(Appointment.class))).thenReturn(true);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule, new CutoffRuleStub()), null);
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).hasValueSatisfying(msg ->
                    assertThat(msg).startsWith(PREFIX_CUTOFF));
        }

        @Test
        @DisplayName("WorkingHours rule name maps to business-hours message")
        void workingHoursMessage() {
            when(passesAllRule.isValid(any(Appointment.class))).thenReturn(true);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule, new WorkingHoursRuleStub()), null);
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).contains(MSG_WORKING_HOURS);
        }

        @Test
        @DisplayName("Duration rule name maps to duration/capacity message")
        void durationMessage() {
            when(passesAllRule.isValid(any(Appointment.class))).thenReturn(true);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule, new DurationRuleStub()), null);
            User p = patient("1");
            InPersonAppointment appt = new InPersonAppointment(p, slotStartingDaysFromNow(2), "R");

            assertThat(svc.tryBookWithReason(appt, p)).contains(MSG_DURATION);
        }
    }

    @Nested
    @DisplayName("Complete appointment (enterprise closure)")
    class Complete {

        @Test
        @DisplayName("Administrator completes CONFIRMED appointment and publishes COMPLETED event")
        void adminCompletes_publishesEvent() {
            AppointmentEventPublisher publisher = mock(AppointmentEventPublisher.class);
            BookingService svc = withEventPublisher(publisher);
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("fixed-id", p, slotStartingDaysFromNow(2), "A");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("fixed-id")).thenReturn(Optional.of(appt));

            assertThat(svc.tryCompleteAppointmentWithReason("fixed-id", new Administrator("a", "A", "a@x.com", "p"))).isEmpty();

            assertThat(appt.getStatus()).isEqualTo("COMPLETED");
            ArgumentCaptor<AppointmentEvent> cap = ArgumentCaptor.forClass(AppointmentEvent.class);
            verify(publisher).publish(cap.capture());
            assertThat(cap.getValue().getType()).isEqualTo(AppointmentEvent.Type.COMPLETED);
            assertThat(cap.getValue().getAppointment().getPatient()).isEqualTo(p);
        }

        @Test
        @DisplayName("Receptionist may complete; without publisher, notifications are sent")
        void receptionistCompletes_notifiesWhenNoPublisher() {
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("r1", p, slotStartingDaysFromNow(3), "B");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("r1")).thenReturn(Optional.of(appt));

            assertThat(bookingService.tryCompleteAppointmentWithReason("r1",
                    new ReceptionistUser("rec", "R", "r@x.com", "p"))).isEmpty();

            verify(notificationService).notifyAllObservers(eq(p), contains("marked completed"));
        }

        @Test
        @DisplayName("Patient cannot complete appointments")
        void patientDenied() {
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("x1", p, slotStartingDaysFromNow(2), "C");
            when(appointmentRepository.findById("x1")).thenReturn(Optional.of(appt));

            assertThat(bookingService.tryCompleteAppointmentWithReason("x1", p)).contains("NO_PERMISSION");
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("EXPIRED status is treated as invalid for completion")
        void expiredInvalidState() {
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("ex", p, slotStartingDaysFromNow(2), "E");
            appt.setStatus("EXPIRED");
            when(appointmentRepository.findById("ex")).thenReturn(Optional.of(appt));

            assertThat(bookingService.tryCompleteAppointmentWithReason("ex", new Administrator("a", "A", "a@x.com", "p")))
                    .contains("INVALID_STATE");
        }

        @Test
        @DisplayName("Optional expiration service runs before load")
        void expirationServiceInvoked() {
            AppointmentExpirationService exp = mock(AppointmentExpirationService.class);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule), new AuditLogService(),
                    null, null, null, exp, null);
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("ex", p, slotStartingDaysFromNow(2), "L");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("ex")).thenReturn(Optional.of(appt));

            assertThat(svc.tryCompleteAppointmentWithReason("ex", new Administrator("a", "A", "a@x.com", "p"))).isEmpty();
            verify(exp).expirePastAppointments();
        }

        @Test
        @DisplayName("NOT_FOUND when appointment id does not exist")
        void notFound() {
            when(appointmentRepository.findById("missing")).thenReturn(Optional.empty());
            assertThat(bookingService.tryCompleteAppointmentWithReason("missing", new Administrator("a", "A", "a@x.com", "p")))
                    .contains("NOT_FOUND");
        }

        @Test
        @DisplayName("ALREADY_COMPLETED is idempotent guard")
        void alreadyCompleted() {
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("done", p, slotStartingDaysFromNow(2), "D");
            appt.setStatus("COMPLETED");
            when(appointmentRepository.findById("done")).thenReturn(Optional.of(appt));
            assertThat(bookingService.tryCompleteAppointmentWithReason("done", new Administrator("a", "A", "a@x.com", "p")))
                    .contains("ALREADY_COMPLETED");
        }

        @Test
        @DisplayName("CANCELLED cannot be completed")
        void cancelledIsInvalidState() {
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("can", p, slotStartingDaysFromNow(2), "E");
            appt.setStatus("CANCELLED");
            when(appointmentRepository.findById("can")).thenReturn(Optional.of(appt));
            assertThat(bookingService.tryCompleteAppointmentWithReason("can", new Administrator("a", "A", "a@x.com", "p")))
                    .contains("INVALID_STATE");
        }

        @Test
        @DisplayName("SAVE_FAILED when repository throws on persist")
        void saveFailed() {
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("sf", p, slotStartingDaysFromNow(2), "F");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("sf")).thenReturn(Optional.of(appt));
            doThrow(new RuntimeException("db")).when(appointmentRepository).save(appt);
            assertThat(bookingService.tryCompleteAppointmentWithReason("sf", new Administrator("a", "A", "a@x.com", "p")))
                    .contains("SAVE_FAILED");
        }

        @Test
        @DisplayName("PermissionService can deny MODIFY_ANY_APPOINTMENT even for staff")
        void permissionServiceDenies() {
            PermissionService perm = mock(PermissionService.class);
            doThrow(new SecurityException()).when(perm).requirePermission(any(User.class), eq(Permission.MODIFY_ANY_APPOINTMENT));
            BookingService svc = withPermission(perm);
            User p = patient("p1");
            InPersonAppointment appt = new InPersonAppointment("perm", p, slotStartingDaysFromNow(2), "G");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("perm")).thenReturn(Optional.of(appt));
            assertThat(svc.tryCompleteAppointmentWithReason("perm", new Administrator("a", "A", "a@x.com", "p")))
                    .contains("NO_PERMISSION");
        }

        @Test
        @DisplayName("Null appointment id or null requester yields INVALID")
        void invalidArguments() {
            Administrator admin = new Administrator("a", "A", "a@x.com", "p");
            assertThat(bookingService.tryCompleteAppointmentWithReason(null, admin)).contains("INVALID");
            assertThat(bookingService.tryCompleteAppointmentWithReason("id", null)).contains("INVALID");
        }
    }

    @Nested
    @DisplayName("Modify appointment")
    class Modify {

        @Test
        @DisplayName("Patient can reschedule a future CONFIRMED appointment")
        void patientModifiesOwn() {
            User p = patient("1");
            TimeSlot oldSlot = slotStartingDaysFromNow(2);
            TimeSlot newSlot = slotStartingDaysFromNow(4);
            InPersonAppointment appt = new InPersonAppointment("mid", p, oldSlot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("mid")).thenReturn(Optional.of(appt));

            assertTrue(bookingService.modifyAppointment("mid", p, newSlot));
            assertThat(appt.getTimeSlot()).isEqualTo(newSlot);
            verify(notificationService).notifyAllObservers(eq(p), contains("MODIFIED"));
            verify(emailNotificationPort).sendAppointmentModified(appt);
        }

        @Test
        @DisplayName("modifyAppointment still persists when reschedule email throws")
        void modify_emailExceptionDoesNotRollback() {
            doThrow(new RuntimeException("smtp modify failed")).when(emailNotificationPort).sendAppointmentModified(any(Appointment.class));
            User p = patient("1");
            TimeSlot oldSlot = slotStartingDaysFromNow(2);
            TimeSlot newSlot = slotStartingDaysFromNow(6);
            InPersonAppointment appt = new InPersonAppointment("mid-email-fail", p, oldSlot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("mid-email-fail")).thenReturn(Optional.of(appt));

            assertTrue(bookingService.modifyAppointment("mid-email-fail", p, newSlot));
            assertThat(appt.getTimeSlot()).isEqualTo(newSlot);
            verify(appointmentRepository).save(appt);
            verify(emailNotificationPort).sendAppointmentModified(appt);
        }

        @Test
        @DisplayName("Administrator can modify another patient's appointment")
        void adminModifiesOtherPatient() {
            User patientUser = patient("1");
            Administrator admin = new Administrator("adm", "A", "a@x.com", "p");
            TimeSlot slot = slotStartingDaysFromNow(2);
            TimeSlot newSlot = slotStartingDaysFromNow(5);
            InPersonAppointment appt = new InPersonAppointment("adm", patientUser, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("adm")).thenReturn(Optional.of(appt));

            assertTrue(bookingService.modifyAppointment("adm", admin, newSlot));
            verify(appointmentRepository).save(appt);
        }

        @Test
        @DisplayName("EXPIRED appointments cannot be modified")
        void expiredCannotModify() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("mid", p, slot, "R");
            appt.setStatus("EXPIRED");
            when(appointmentRepository.findById("mid")).thenReturn(Optional.of(appt));

            assertFalse(bookingService.modifyAppointment("mid", p, slotStartingDaysFromNow(5)));
        }

        @Test
        @DisplayName("Event publisher suppresses duplicate notification on modify")
        void modifyUsesPublisherWhenPresent() {
            AppointmentEventPublisher publisher = mock(AppointmentEventPublisher.class);
            BookingService svc = withEventPublisher(publisher);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            TimeSlot newSlot = slotStartingDaysFromNow(5);
            InPersonAppointment appt = new InPersonAppointment("ev", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("ev")).thenReturn(Optional.of(appt));

            assertTrue(svc.modifyAppointment("ev", p, newSlot));
            verify(publisher).publish(any(AppointmentEvent.class));
            verify(notificationService, never()).notifyAllObservers(any(), anyString());
            verify(emailNotificationPort).sendAppointmentModified(appt);
        }

        @Test
        @DisplayName("False when appointment id is unknown")
        void notFound() {
            when(appointmentRepository.findById("x")).thenReturn(Optional.empty());
            assertFalse(bookingService.modifyAppointment("x", patient("1"), slotStartingDaysFromNow(4)));
        }

        @Test
        @DisplayName("Non-staff cannot modify another patient's appointment")
        void wrongPatientDenied() {
            User owner = patient("1");
            User intruder = patient("2");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("m2", owner, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("m2")).thenReturn(Optional.of(appt));
            assertFalse(bookingService.modifyAppointment("m2", intruder, slotStartingDaysFromNow(4)));
        }

        @Test
        @DisplayName("Past start time blocks modification even for the owner")
        void pastStartBlocksModify() {
            User p = patient("1");
            TimeSlot past = new TimeSlot(LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(1));
            InPersonAppointment appt = new InPersonAppointment("m3", p, past, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("m3")).thenReturn(Optional.of(appt));
            assertFalse(bookingService.modifyAppointment("m3", p, slotStartingDaysFromNow(4)));
        }

        @Test
        @DisplayName("CANCELLED status blocks modification")
        void cancelledBlocksModify() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("m4", p, slot, "R");
            appt.setStatus("CANCELLED");
            when(appointmentRepository.findById("m4")).thenReturn(Optional.of(appt));
            assertFalse(bookingService.modifyAppointment("m4", p, slotStartingDaysFromNow(5)));
        }

        @Test
        @DisplayName("PermissionService.hasPermission false denies modify")
        void permissionDenied() {
            PermissionService perm = mock(PermissionService.class);
            when(perm.hasPermission(any(User.class), any(Permission.class))).thenReturn(false);
            BookingService svc = withPermission(perm);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("m5", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("m5")).thenReturn(Optional.of(appt));
            assertFalse(svc.modifyAppointment("m5", p, slotStartingDaysFromNow(5)));
        }

        @Test
        @DisplayName("PolicyEngine denial blocks modify")
        void policyDeny() {
            PolicyEngine engine = mock(PolicyEngine.class);
            when(engine.evaluate(any())).thenReturn(Policy.PolicyResult.deny("no"));
            BookingService svc = withPolicy(engine);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("m6", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("m6")).thenReturn(Optional.of(appt));
            assertFalse(svc.modifyAppointment("m6", p, slotStartingDaysFromNow(5)));
        }

        @Test
        @DisplayName("Expiration hook runs before modify when service is wired")
        void expirationRunsBeforeModify() {
            AppointmentExpirationService exp = mock(AppointmentExpirationService.class);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule), new AuditLogService(),
                    null, null, null, exp, null);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("mod", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("mod")).thenReturn(Optional.of(appt));
            assertTrue(svc.modifyAppointment("mod", p, slotStartingDaysFromNow(5)));
            verify(exp).expirePastAppointments();
        }
    }

    @Nested
    @DisplayName("Cancel appointment")
    class Cancel {

        @Test
        @DisplayName("Patient can cancel own future appointment")
        void patientCancels() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment(p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById(appt.getId())).thenReturn(Optional.of(appt));

            assertTrue(bookingService.cancelAppointment(appt.getId(), p));
            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
            verify(notificationService).notifyAllObservers(eq(p), contains("CANCELLED"));
            verify(emailNotificationPort).sendAppointmentCancelled(appt);
        }

        @Test
        @DisplayName("cancelAppointment still persists when cancellation email throws")
        void cancel_emailExceptionDoesNotRollback() {
            doThrow(new RuntimeException("smtp cancel failed")).when(emailNotificationPort).sendAppointmentCancelled(any(Appointment.class));
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("can-email-fail", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("can-email-fail")).thenReturn(Optional.of(appt));

            assertTrue(bookingService.cancelAppointment("can-email-fail", p));
            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
            verify(appointmentRepository).save(appt);
            verify(emailNotificationPort).sendAppointmentCancelled(appt);
        }

        @Test
        @DisplayName("Administrator can cancel another patient's appointment")
        void adminCancelsOtherPatient() {
            User patientUser = patient("1");
            Administrator admin = new Administrator("adm", "A", "a@x.com", "p");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("cadm", patientUser, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("cadm")).thenReturn(Optional.of(appt));

            assertTrue(bookingService.cancelAppointment("cadm", admin));
            assertThat(appt.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("EXPIRED appointments cannot be cancelled via this path")
        void expiredCannotCancel() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("cex", p, slot, "R");
            appt.setStatus("EXPIRED");
            when(appointmentRepository.findById("cex")).thenReturn(Optional.of(appt));

            assertFalse(bookingService.cancelAppointment("cex", p));
        }

        @Test
        @DisplayName("False when id unknown")
        void notFound() {
            when(appointmentRepository.findById("missing")).thenReturn(Optional.empty());
            assertFalse(bookingService.cancelAppointment("missing", patient("1")));
        }

        @Test
        @DisplayName("Cannot cancel appointments that already started in the past")
        void pastStartBlocksCancel() {
            User p = patient("1");
            TimeSlot past = new TimeSlot(LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1).plusHours(1));
            InPersonAppointment appt = new InPersonAppointment("c1", p, past, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("c1")).thenReturn(Optional.of(appt));
            assertFalse(bookingService.cancelAppointment("c1", p));
        }

        @Test
        @DisplayName("Already CANCELLED is rejected")
        void alreadyCancelled() {
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("c2", p, slot, "R");
            appt.setStatus("CANCELLED");
            when(appointmentRepository.findById("c2")).thenReturn(Optional.of(appt));
            assertFalse(bookingService.cancelAppointment("c2", p));
        }

        @Test
        @DisplayName("Non-staff cannot cancel someone else's appointment")
        void wrongUser() {
            User owner = patient("1");
            User other = patient("2");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("c3", owner, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("c3")).thenReturn(Optional.of(appt));
            assertFalse(bookingService.cancelAppointment("c3", other));
        }

        @Test
        @DisplayName("PermissionService.hasPermission false denies cancel")
        void permissionDenied() {
            PermissionService perm = mock(PermissionService.class);
            when(perm.hasPermission(any(User.class), any(Permission.class))).thenReturn(false);
            BookingService svc = withPermission(perm);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("c4", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("c4")).thenReturn(Optional.of(appt));
            assertFalse(svc.cancelAppointment("c4", p));
        }

        @Test
        @DisplayName("PolicyEngine denial blocks cancel")
        void policyDeny() {
            PolicyEngine engine = mock(PolicyEngine.class);
            when(engine.evaluate(any())).thenReturn(Policy.PolicyResult.deny("no"));
            BookingService svc = withPolicy(engine);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("c5", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("c5")).thenReturn(Optional.of(appt));
            assertFalse(svc.cancelAppointment("c5", p));
        }

        @Test
        @DisplayName("Expiration hook runs before cancel when service is wired")
        void expirationRunsBeforeCancel() {
            AppointmentExpirationService exp = mock(AppointmentExpirationService.class);
            BookingService svc = new BookingService(
                    appointmentRepository, notificationService, scheduleService,
                    List.of(passesAllRule), new AuditLogService(),
                    null, null, null, exp, null);
            User p = patient("1");
            TimeSlot slot = slotStartingDaysFromNow(2);
            InPersonAppointment appt = new InPersonAppointment("cc", p, slot, "R");
            appt.setStatus("CONFIRMED");
            when(appointmentRepository.findById("cc")).thenReturn(Optional.of(appt));
            assertTrue(svc.cancelAppointment("cc", p));
            verify(exp).expirePastAppointments();
        }
    }

    @Nested
    @DisplayName("Queries and guards")
    class Queries {

        @Test
        @DisplayName("patientHasBlockingOpenAppointment is false when repository returns none")
        void noBlockers() {
            assertFalse(bookingService.patientHasBlockingOpenAppointment("p1"));
        }

        @Test
        @DisplayName("patientHasBlockingOpenAppointment is false when patient id is null")
        void nullPatientId() {
            assertFalse(bookingService.patientHasBlockingOpenAppointment(null));
        }

        @Test
        @DisplayName("patientHasBlockingOpenAppointment is true when repository returns blocking rows")
        void hasBlockers() {
            User p = patient("p1");
            when(appointmentRepository.findBlockingBookingsForPatient("p1"))
                    .thenReturn(List.of(new InPersonAppointment(p, slotStartingDaysFromNow(2), "x")));

            assertTrue(bookingService.patientHasBlockingOpenAppointment("p1"));
        }
    }

    /** Fails validation; class name must not match Cutoff/WorkingHours/Duration shortcuts. */
    private static final class ArbitraryFailureRule implements BookingRuleStrategy {
        @Override
        public boolean isValid(Appointment appointment) {
            return false;
        }
    }

    private static final class CutoffRuleStub implements BookingRuleStrategy {
        @Override
        public boolean isValid(Appointment appointment) {
            return false;
        }
    }

    private static final class WorkingHoursRuleStub implements BookingRuleStrategy {
        @Override
        public boolean isValid(Appointment appointment) {
            return false;
        }
    }

    private static final class DurationRuleStub implements BookingRuleStrategy {
        @Override
        public boolean isValid(Appointment appointment) {
            return false;
        }
    }
}
