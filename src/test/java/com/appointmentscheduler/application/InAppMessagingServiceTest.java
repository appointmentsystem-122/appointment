package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.InMemoryUserRepository;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InAppMessagingServiceTest {

    @Test
    void canBroadcast_andCanViewStaffInbox_onlyStaffRoles() {
        assertThat(InAppMessagingService.canBroadcast(null)).isFalse();
        assertThat(InAppMessagingService.canViewStaffInbox(null)).isFalse();
        User patient = new User("p", "P", "p@x.com", "x");
        assertThat(InAppMessagingService.canBroadcast(patient)).isFalse();
        assertThat(InAppMessagingService.canViewStaffInbox(patient)).isFalse();
        assertThat(InAppMessagingService.canBroadcast(new Administrator("a", "A", "a@x.com", "x"))).isTrue();
        assertThat(InAppMessagingService.canBroadcast(new ReceptionistUser("r", "R", "r@x.com", "x"))).isTrue();
        assertThat(InAppMessagingService.canViewStaffInbox(new Administrator("a2", "A", "a2@x.com", "x"))).isTrue();
    }

    @Test
    void listPatients_excludesStaffAccounts() {
        UserRepository repo = new InMemoryUserRepository();
        repo.save(new User("p1", "P", "p@x.com", "x"));
        repo.save(new Administrator("a1", "A", "a@x.com", "x"));
        InAppMessagingService svc = new InAppMessagingService(repo, mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        assertThat(svc.listPatients()).extracting(User::getId).containsExactly("p1");
    }

    @Test
    void broadcast_writesInbox_forEachPatient() {
        UserRepository repo = new InMemoryUserRepository();
        User p1 = new User("p1", "A", "a@x.com", "x");
        User p2 = new User("p2", "B", "b@x.com", "x");
        repo.save(p1);
        repo.save(p2);

        AuditLogService audit = mock(AuditLogService.class);
        PatientInboxService inbox = new PatientInboxService();
        StaffInboxService staffInbox = new StaffInboxService();
        InAppMessagingService svc = new InAppMessagingService(repo, audit, inbox, staffInbox);

        Administrator admin = new Administrator("adm", "Admin", "adm@x.com", "x");
        DispatchSummary r = svc.broadcastToPatients(admin, List.of(p1, p2), "Hello", "Body text");

        assertThat(r.getSuccessCount()).isEqualTo(2);
        assertThat(r.getFailureCount()).isEqualTo(0);
        assertThat(inbox.listRecent("p1", 5)).hasSize(1);
        assertThat(inbox.listRecent("p2", 5)).hasSize(1);
    }

    @Test
    void broadcast_rejectedForNonStaff() {
        UserRepository repo = new InMemoryUserRepository();
        User p = new User("p1", "A", "a@x.com", "x");
        repo.save(p);
        InAppMessagingService svc = new InAppMessagingService(repo, mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        DispatchSummary r = svc.broadcastToPatients(p, List.of(p), "S", "B");
        assertThat(r.isForbidden()).isTrue();
    }

    @Test
    void contactFromPatient_appendsStaffInbox() {
        UserRepository repo = new InMemoryUserRepository();
        User patient = new User("p1", "Pat", "p@x.com", "x");
        repo.save(patient);
        StaffInboxService staffInbox = new StaffInboxService();
        InAppMessagingService svc = new InAppMessagingService(repo, mock(AuditLogService.class), new PatientInboxService(), staffInbox);
        DispatchSummary r = svc.sendContactRequestFromPatient(patient, "Hello", "Body text");
        assertThat(r.getSuccessCount()).isEqualTo(1);
        assertThat(svc.getStaffContactInbox(10)).hasSize(1);
        assertThat(svc.getStaffContactInbox(10).get(0).getSubject()).isEqualTo("Hello");
        assertThat(svc.getStaffContactInbox(10).get(0).getCustomerEmail()).isEqualTo("p@x.com");
    }

    @Test
    void broadcast_emptySubjectOrBody_returnsEmptySummary() {
        UserRepository repo = new InMemoryUserRepository();
        User p = new User("p1", "P", "p@x.com", "x");
        repo.save(p);
        InAppMessagingService svc = new InAppMessagingService(repo, mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        Administrator admin = new Administrator("adm", "Admin", "a@x.com", "x");
        assertThat(svc.broadcastToPatients(admin, List.of(p), "  ", "body").getMessage()).contains("required");
        assertThat(svc.broadcastToPatients(admin, List.of(p), "sub", "  ").getMessage()).contains("required");
    }

    @Test
    void broadcast_noRecipients_returnsEmptySummary() {
        InAppMessagingService svc = new InAppMessagingService(new InMemoryUserRepository(), mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        Administrator admin = new Administrator("adm", "Admin", "a@x.com", "x");
        assertThat(svc.broadcastToPatients(admin, null, "S", "B").getMessage()).contains("No recipients");
        assertThat(svc.broadcastToPatients(admin, List.of(), "S", "B").getMessage()).contains("No recipients");
    }

    @Test
    void broadcast_skipsNullAndNonPatient_andDeduplicatesById() {
        UserRepository repo = new InMemoryUserRepository();
        User p1 = new User("p1", "P", "p@x.com", "x");
        repo.save(p1);
        InAppMessagingService svc = new InAppMessagingService(repo, mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        Administrator admin = new Administrator("adm", "Admin Name", "a@x.com", "x");
        ArrayList<User> recipients = new ArrayList<>();
        recipients.add(p1);
        recipients.add(null);
        recipients.add(p1);
        recipients.add(new Administrator("x", "X", "x@x.com", "p"));
        DispatchSummary r = svc.broadcastToPatients(admin, recipients, "S", "B");
        assertThat(r.getSuccessCount()).isEqualTo(1);
        assertThat(r.getSkipped()).isEqualTo(2);
    }

    @Test
    void broadcast_usesOrganizationWhenActorNameNull() {
        UserRepository repo = new InMemoryUserRepository();
        User p = new User("p1", "P", "p@x.com", "x");
        repo.save(p);
        PatientInboxService inbox = new PatientInboxService();
        InAppMessagingService svc = new InAppMessagingService(repo, mock(AuditLogService.class), inbox, new StaffInboxService());
        Administrator admin = mock(Administrator.class);
        when(admin.getName()).thenReturn(null);
        when(admin.getId()).thenReturn("adm");
        svc.broadcastToPatients(admin, List.of(p), "S", "B");
        assertThat(inbox.listRecent("p1", 1).get(0).getSenderLabel()).isEqualTo("Organization");
    }

    @Test
    void sendContact_rejectsNonCustomer() {
        InAppMessagingService svc = new InAppMessagingService(new InMemoryUserRepository(), mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        assertThat(svc.sendContactRequestFromPatient(null, "S", "B").getMessage()).contains("customer");
        assertThat(svc.sendContactRequestFromPatient(new Administrator("a", "A", "a@x.com", "x"), "S", "B").getMessage()).contains("customer");
    }

    @Test
    void sendContact_requiresSubjectAndBody() {
        User patient = new User("p1", "P", "p@x.com", "x");
        InAppMessagingService svc = new InAppMessagingService(new InMemoryUserRepository(), mock(AuditLogService.class), new PatientInboxService(), new StaffInboxService());
        assertThat(svc.sendContactRequestFromPatient(patient, "", "B").getMessage()).contains("required");
        assertThat(svc.sendContactRequestFromPatient(patient, "S", " ").getMessage()).contains("required");
    }
}
