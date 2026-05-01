package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * In-app messaging: staff broadcast to customers (per-customer inbox), customer contact to staff
 * (staff inbox + audit), no external email/SMS.
 */
public class InAppMessagingService {

    private static final Logger log = LoggerFactory.getLogger(InAppMessagingService.class);

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PatientInboxService patientInbox;
    private final StaffInboxService staffInbox;

    public InAppMessagingService(UserRepository userRepository,
                                 AuditLogService auditLogService,
                                 PatientInboxService patientInbox,
                                 StaffInboxService staffInbox) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.auditLogService = Objects.requireNonNull(auditLogService);
        this.patientInbox = Objects.requireNonNull(patientInbox);
        this.staffInbox = Objects.requireNonNull(staffInbox);
    }

    public static boolean canBroadcast(User actor) {
        return actor instanceof Administrator || actor instanceof ReceptionistUser;
    }

    /** Staff / reception can view the inbound customer contact inbox. */
    public static boolean canViewStaffInbox(User actor) {
        return actor instanceof Administrator || actor instanceof ReceptionistUser;
    }

    public List<StaffContactMessage> getStaffContactInbox(int max) {
        return staffInbox.listRecent(max);
    }

    public List<User> listPatients() {
        List<User> out = new ArrayList<>();
        for (User u : userRepository.getAllUsers()) {
            if (UserDirectory.isPatient(u)) {
                out.add(u);
            }
        }
        return List.copyOf(out);
    }

    public List<PatientInboxEntry> getPatientInbox(String userId) {
        return patientInbox.listRecent(userId, 50);
    }

    /**
     * Appends an in-app inbox entry for each distinct customer recipient.
     */
    public DispatchSummary broadcastToPatients(User actor, List<User> recipients, String subject, String body) {
        if (!canBroadcast(actor)) {
            return DispatchSummary.forbidden();
        }
        String sub = subject != null ? subject.trim() : "";
        String txt = body != null ? body.trim() : "";
        if (sub.isEmpty() || txt.isEmpty()) {
            return DispatchSummary.empty("Subject and message are required.");
        }
        if (recipients == null || recipients.isEmpty()) {
            return DispatchSummary.empty("No recipients.");
        }
        Set<String> seen = new LinkedHashSet<>();
        int ok = 0;
        int skip = 0;
        String senderLabel = actor.getName() != null ? actor.getName() : "Organization";
        for (User r : recipients) {
            if (r == null || !UserDirectory.isPatient(r)) {
                skip++;
                continue;
            }
            String id = r.getId();
            if (!seen.add(id)) {
                continue;
            }
            patientInbox.append(id, new PatientInboxEntry(sub, txt, LocalDateTime.now(), senderLabel));
            ok++;
            log.debug("In-app message stored userId={} correlationId={}", id, UUID.randomUUID());
        }
        String msg = String.format("Delivered to in-app inbox: %d, skipped: %d.", ok, skip);
        auditLogService.log(actor, "MESSAGING_BROADCAST", msg + " subject=" + sub);
        return DispatchSummary.of(ok, 0, skip, msg);
    }

    /**
     * Stores the message in the staff inbox, appends audit detail, and returns success to the customer.
     */
    public DispatchSummary sendContactRequestFromPatient(User patient, String subject, String body) {
        if (patient == null || !UserDirectory.isPatient(patient)) {
            return DispatchSummary.empty("Only customer accounts can use this form.");
        }
        String sub = subject != null ? subject.trim() : "";
        String txt = body != null ? body.trim() : "";
        if (sub.isEmpty() || txt.isEmpty()) {
            return DispatchSummary.empty("Subject and message are required.");
        }
        StaffContactMessage inbound = new StaffContactMessage(
                sub, txt, LocalDateTime.now(),
                patient.getId(), patient.getName(), patient.getEmail());
        staffInbox.append(inbound);
        auditLogService.log(patient, "MESSAGING_PATIENT_CONTACT",
                "subject=" + sub + " staffInboxId=" + inbound.getId());
        log.debug("Customer contact stored in staff inbox id={} customerId={}", inbound.getId(), patient.getId());
        String okMsg = "Your message was delivered to the service team inbox.";
        return DispatchSummary.of(1, 0, 0, okMsg);
    }
}
