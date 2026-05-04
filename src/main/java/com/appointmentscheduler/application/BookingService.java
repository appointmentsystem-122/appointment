package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.authorization.Permission;
import com.appointmentscheduler.domain.events.AppointmentEvent;
import com.appointmentscheduler.domain.events.AppointmentEventPublisher;
import com.appointmentscheduler.domain.policy.BookingPolicies;
import com.appointmentscheduler.domain.rules.BookingRuleStrategy;
import com.appointmentscheduler.application.email.EmailNotificationPort;
import com.appointmentscheduler.persistence.AppointmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core service for handling appointments, applying business rules, and managing state.
 * Integrates: PermissionService, PolicyEngine, event-driven notifications, and optional
 * {@link com.appointmentscheduler.application.email.EmailNotificationPort} for SMTP confirmations after successful book / modify / cancel.
 */
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final ScheduleService scheduleService;
    private final List<BookingRuleStrategy> rules;
    private final AuditLogService auditLogService;
    private final PermissionService permissionService;
    private final PolicyEngine policyEngine;
    private final AppointmentEventPublisher eventPublisher;
    private final AppointmentExpirationService expirationService;
    private final EmailNotificationPort emailNotificationPort;

    public BookingService(AppointmentRepository appointmentRepository,
                          NotificationService notificationService,
                          ScheduleService scheduleService,
                          List<BookingRuleStrategy> rules,
                          AuditLogService auditLogService) {
        this(appointmentRepository, notificationService, scheduleService, rules, auditLogService,
                null, null, null, null, null);
    }

    public BookingService(AppointmentRepository appointmentRepository,
                          NotificationService notificationService,
                          ScheduleService scheduleService,
                          List<BookingRuleStrategy> rules,
                          AuditLogService auditLogService,
                          PermissionService permissionService,
                          PolicyEngine policyEngine,
                          AppointmentEventPublisher eventPublisher,
                          AppointmentExpirationService expirationService,
                          EmailNotificationPort emailNotificationPort) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
        this.scheduleService = scheduleService;
        this.rules = rules;
        this.auditLogService = auditLogService != null ? auditLogService : new AuditLogService();
        this.permissionService = permissionService;
        this.policyEngine = policyEngine;
        this.eventPublisher = eventPublisher;
        this.expirationService = expirationService;
        this.emailNotificationPort = emailNotificationPort;
    }

    /**
     * Attempts to book an appointment.
     * Enforces permissions, policies, and business rules.
     */
    public boolean bookAppointment(Appointment appointment) {
        return bookAppointment(appointment, null);
    }

    /**
     * Attempts to book; returns empty on success, or the failure reason string.
     */
    public Optional<String> tryBookWithReason(Appointment appointment, User requester) {
        User actor = requester != null ? requester : (appointment != null ? appointment.getPatient() : null);
        if (appointment == null || appointment.getTimeSlot() == null) return Optional.of("Invalid appointment or time slot.");
        if (permissionService != null && actor != null) {
            try {
                permissionService.requirePermission(actor, Permission.BOOK_APPOINTMENT);
            } catch (SecurityException e) {
                return Optional.of("No permission to book.");
            }
        }
        // One open booking per customer (self-service): staff may override for walk-in / assisted booking.
        if (!isStaffBookingActor(actor)) {
            List<Appointment> blocking = appointmentRepository.findBlockingBookingsForPatient(appointment.getPatient().getId());
            if (!blocking.isEmpty()) {
                return Optional.of(BookingFailureCodes.OPEN_APPOINTMENT_NOT_COMPLETED);
            }
        }
        BookingPolicies.BookingContext ctx = new BookingPolicies.BookingContext(appointment, actor);
        if (policyEngine != null) {
            var result = policyEngine.evaluate(ctx);
            if (!result.isPassed()) return Optional.of("Booking policy did not allow this reservation.");
        }
        for (BookingRuleStrategy rule : rules) {
            if (!rule.isValid(appointment)) {
                String ruleName = rule.getClass().getSimpleName();
                if (ruleName.contains("Cutoff")) return Optional.of("This slot is too soon. Choose a time at least " + com.appointmentscheduler.application.AppConfig.getInt("booking.cutoffHoursBefore", 0) + " hour(s) from now.");
                if (ruleName.contains("WorkingHours")) return Optional.of("This time is outside working hours (check business hours).");
                if (ruleName.contains("Duration")) return Optional.of("Duration or capacity rule failed.");
                return Optional.of("A booking rule did not allow this slot: " + ruleName);
            }
        }
        List<Appointment> overlapping = scheduleService.getMasterSchedule().getOverlappingAppointments(appointment.getTimeSlot());
        if (overlapping.stream().anyMatch(a -> !"CANCELLED".equals(a.getStatus()))) {
            return Optional.of("This slot is already taken or overlaps with another appointment.");
        }
        try {
            ensureClinicIdOnAppointment(appointment);
            appointment.setStatus("CONFIRMED");
            appointmentRepository.save(appointment);
            scheduleService.loadSchedule();
        } catch (Exception e) {
            return Optional.of("Could not save to database: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
        sendBookingConfirmationEmail(appointment);
        if (eventPublisher != null) {
            eventPublisher.publish(new AppointmentEvent(AppointmentEvent.Type.CREATED, appointment, actor, appointment.getTimeSlot().toString()));
        } else {
            notificationService.notifyAllObservers(appointment.getPatient(), "Your appointment has been CONFIRMED for " + appointment.getTimeSlot());
        }
        auditLogService.log(appointment.getPatient(), "BOOK", "Appointment " + appointment.getId() + " at " + appointment.getTimeSlot(),
                "Appointment", appointment.getId(), null, appointment.getTimeSlot().toString());
        return Optional.empty();
    }

    /** Assigns a branch so admin filters and reports stay consistent when the UI never set clinic. */
    private static void ensureClinicIdOnAppointment(Appointment appointment) {
        if (appointment == null) return;
        String existing = appointment.getClinicId();
        if (existing != null && !existing.isEmpty()) return;
        String cid = null;
        if (ApplicationContext.getCurrentClinicService() != null) {
            cid = ApplicationContext.getCurrentClinicService().getCurrentClinicId();
        }
        if (cid == null || cid.isEmpty()) {
            if (ApplicationContext.getClinicRepository() != null) {
                var clinics = ApplicationContext.getClinicRepository().findAll();
                if (clinics != null && !clinics.isEmpty()) {
                    cid = clinics.get(0).getId();
                }
            }
        }
        if (cid != null && !cid.isEmpty()) {
            appointment.setClinicId(cid);
        }
    }

    public boolean bookAppointment(Appointment appointment, User requester) {
        return tryBookWithReason(appointment, requester).isEmpty();
    }

    /** True if the patient has a PENDING/CONFIRMED non-deleted appointment (blocks another self-service book). */
    public boolean patientHasBlockingOpenAppointment(String patientId) {
        if (patientId == null) return false;
        return !appointmentRepository.findBlockingBookingsForPatient(patientId).isEmpty();
    }

    private static boolean isStaffBookingActor(User actor) {
        return actor instanceof Administrator || actor instanceof ReceptionistUser;
    }

    /**
     * Sends a confirmation email to the patient after persistence. Failures are logged only;
     * the booking outcome is not rolled back.
     */
    private void sendBookingConfirmationEmail(Appointment appointment) {
        if (emailNotificationPort == null || appointment == null) {
            return;
        }
        try {
            emailNotificationPort.sendBookingConfirmation(appointment);
        } catch (Exception e) {
            log.warn("Booking confirmation email failed for appointment {}; booking remains saved.", appointment.getId(), e);
        }
    }

    private void sendAppointmentModifiedEmail(Appointment appointment) {
        if (emailNotificationPort == null || appointment == null) {
            return;
        }
        try {
            emailNotificationPort.sendAppointmentModified(appointment);
        } catch (Exception e) {
            log.warn("Reschedule notification email failed for appointment {}; changes remain saved.", appointment.getId(), e);
        }
    }

    private void sendAppointmentCancelledEmail(Appointment appointment) {
        if (emailNotificationPort == null || appointment == null) {
            return;
        }
        try {
            emailNotificationPort.sendAppointmentCancelled(appointment);
        } catch (Exception e) {
            log.warn("Cancellation email failed for appointment {}; cancellation remains saved.", appointment.getId(), e);
        }
    }

    /**
     * Marks an appointment completed (enterprise closure). Allows the customer to book again.
     */
    public boolean completeAppointment(String appointmentId, User requester) {
        return tryCompleteAppointmentWithReason(appointmentId, requester).isEmpty();
    }

    /**
     * @return empty on success, or a short reason token for the UI
     */
    public Optional<String> tryCompleteAppointmentWithReason(String appointmentId, User requester) {
        if (appointmentId == null || requester == null) return Optional.of("INVALID");
        if (!(requester instanceof Administrator || requester instanceof ReceptionistUser)) {
            return Optional.of("NO_PERMISSION");
        }
        if (permissionService != null) {
            try {
                permissionService.requirePermission(requester, Permission.MODIFY_ANY_APPOINTMENT);
            } catch (SecurityException e) {
                return Optional.of("NO_PERMISSION");
            }
        }
        if (expirationService != null) expirationService.expirePastAppointments();
        Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
        if (opt.isEmpty()) return Optional.of("NOT_FOUND");
        Appointment appointment = opt.get();
        String st = appointment.getStatus();
        if ("COMPLETED".equals(st)) return Optional.of("ALREADY_COMPLETED");
        if ("CANCELLED".equals(st) || "EXPIRED".equals(st)) return Optional.of("INVALID_STATE");
        appointment.setStatus("COMPLETED");
        try {
            appointmentRepository.save(appointment);
            scheduleService.loadSchedule();
        } catch (Exception e) {
            return Optional.of("SAVE_FAILED");
        }
        if (eventPublisher != null) {
            eventPublisher.publish(new AppointmentEvent(AppointmentEvent.Type.COMPLETED, appointment, requester, appointment.getTimeSlot().toString()));
        } else {
            notificationService.notifyAllObservers(appointment.getPatient(),
                    "Your appointment on " + appointment.getTimeSlot() + " has been marked completed.");
        }
        auditLogService.log(requester, "COMPLETE", "Appointment " + appointmentId + " marked COMPLETED",
                "Appointment", appointmentId, st, "COMPLETED");
        return Optional.empty();
    }

    /**
     * Modifies an appointment's timeslot. Enforces permissions and policies.
     */
    public boolean modifyAppointment(String appointmentId, User requester, TimeSlot newSlot) {
        if (expirationService != null) expirationService.expirePastAppointments();
        Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
        if (opt.isEmpty()) return false;
        Appointment appointment = opt.get();

        boolean canModifyAny = requester instanceof Administrator || requester instanceof ReceptionistUser;
        if (permissionService != null) {
            Permission perm = canModifyAny ? Permission.MODIFY_ANY_APPOINTMENT : Permission.MODIFY_OWN_APPOINTMENT;
            if (!permissionService.hasPermission(requester, perm)) return false;
        }
        var mctx = new BookingPolicies.ModifyCancelContext(appointmentId, appointment, requester);
        if (policyEngine != null) {
            var result = policyEngine.evaluate(mctx);
            if (!result.isPassed()) return false;
        }
        if (!appointment.getPatient().equals(requester) && !canModifyAny) return false;
        if (appointment.getTimeSlot().getStartTime().isBefore(LocalDateTime.now())) return false;
        if ("CANCELLED".equals(appointment.getStatus()) || "EXPIRED".equals(appointment.getStatus())) return false;

        String oldSlotStr = appointment.getTimeSlot().toString();
        appointment.setTimeSlot(newSlot);
        appointmentRepository.save(appointment);
        scheduleService.loadSchedule();

        sendAppointmentModifiedEmail(appointment);
        if (eventPublisher != null) {
            eventPublisher.publish(new AppointmentEvent(AppointmentEvent.Type.MODIFIED, appointment, requester, newSlot.toString()));
        } else {
            notificationService.notifyAllObservers(appointment.getPatient(), "Your appointment has been MODIFIED to " + newSlot);
        }
        auditLogService.log(requester, "MODIFY", "Appointment " + appointmentId + " to " + newSlot,
                "Appointment", appointmentId, oldSlotStr, newSlot.toString());
        return true;
    }

    /**
     * Cancels an appointment. Uses soft semantics (status=CANCELLED, no hard delete).
     */
    public boolean cancelAppointment(String appointmentId, User requester) {
        if (expirationService != null) expirationService.expirePastAppointments();
        Optional<Appointment> opt = appointmentRepository.findById(appointmentId);
        if (opt.isEmpty()) return false;
        Appointment appointment = opt.get();

        boolean canCancelAny = requester instanceof Administrator || requester instanceof ReceptionistUser;
        if (permissionService != null) {
            Permission perm = canCancelAny ? Permission.CANCEL_ANY_APPOINTMENT : Permission.CANCEL_OWN_APPOINTMENT;
            if (!permissionService.hasPermission(requester, perm)) return false;
        }
        var mctx = new BookingPolicies.ModifyCancelContext(appointmentId, appointment, requester);
        if (policyEngine != null) {
            var result = policyEngine.evaluate(mctx);
            if (!result.isPassed()) return false;
        }
        if (!appointment.getPatient().equals(requester) && !canCancelAny) return false;
        if (appointment.getTimeSlot().getStartTime().isBefore(LocalDateTime.now())) return false;
        if ("CANCELLED".equals(appointment.getStatus()) || "EXPIRED".equals(appointment.getStatus())) return false;

        String oldStatus = appointment.getStatus();
        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);
        scheduleService.loadSchedule();

        sendAppointmentCancelledEmail(appointment);
        if (eventPublisher != null) {
            eventPublisher.publish(new AppointmentEvent(AppointmentEvent.Type.CANCELLED, appointment, requester, ""));
        } else {
            notificationService.notifyAllObservers(appointment.getPatient(), "Your appointment has been CANCELLED.");
        }
        auditLogService.log(requester, "CANCEL", "Appointment " + appointmentId,
                "Appointment", appointmentId, oldStatus, "CANCELLED");
        return true;
    }
}
