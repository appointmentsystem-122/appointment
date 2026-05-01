package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import com.appointmentscheduler.persistence.InMemoryUserRepository;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalSearchServiceTest {

    @Test
    void blankTerm_returnsEmpty() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search(null, 10)).isEmpty();
        assertThat(g.search("   ", 10)).isEmpty();
    }

    @Test
    void findsAppointmentAndUser() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u1", "Alice Search", "alice@x.com", "x");
        ur.save(u);
        LocalDateTime s = LocalDateTime.now().plusDays(2);
        InPersonAppointment ap = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "R");
        ar.save(ap);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        List<GlobalSearchService.SearchResult> r1 = g.search("alice", 10);
        assertThat(r1.stream().map(x -> x.type)).contains("appointment", "user");
        String prefix = ap.getId().substring(0, Math.min(8, ap.getId().length()));
        List<GlobalSearchService.SearchResult> r2 = g.search(prefix, 10);
        assertThat(r2.stream().anyMatch(x -> "appointment".equals(x.type))).isTrue();
    }

    @Test
    void deletedAppointment_excludedFromResults() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u-del", "Bob Deleted", "bob@del.com", "x");
        ur.save(u);
        LocalDateTime s = LocalDateTime.now().plusDays(3);
        InPersonAppointment ap = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "R");
        ap.setDeletedState(true, LocalDateTime.now(), "test");
        ar.save(ap);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        // User repo still matches name/email; deleted appointments must not appear as "appointment" hits.
        assertThat(g.search("Bob Deleted", 20).stream().filter(r -> "appointment".equals(r.type))).isEmpty();
        assertThat(g.search("bob@del.com", 20).stream().filter(r -> "appointment".equals(r.type))).isEmpty();
        String idPrefix = ap.getId().substring(0, Math.min(8, ap.getId().length()));
        assertThat(g.search(idPrefix, 20).stream().filter(r -> "appointment".equals(r.type))).isEmpty();
    }

    @Test
    void maxResults_zero_returnsEmpty() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u0", "Zero Max", "zero@x.com", "x");
        ur.save(u);
        ar.save(new InPersonAppointment(u, new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1)), "R"));
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search("Zero", 0)).isEmpty();
    }

    @Test
    void findsUserByEmailOnly() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u-email", "UniqueNameXy", "onlyemail@domain.test", "x");
        ur.save(u);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        List<GlobalSearchService.SearchResult> r = g.search("onlyemail@domain", 10);
        assertThat(r.stream().filter(x -> "user".equals(x.type))).isNotEmpty();
    }

    @Test
    void matchesAppointmentByTimeSlotString() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u-ts", "Slot User", "slot@x.com", "x");
        ur.save(u);
        LocalDateTime start = LocalDateTime.of(2030, 6, 15, 14, 30);
        String slotFragment = "2030";
        InPersonAppointment ap = new InPersonAppointment(u, new TimeSlot(start, start.plusHours(1)), "R");
        ar.save(ap);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        List<GlobalSearchService.SearchResult> r = g.search(slotFragment, 20);
        assertThat(r.stream().anyMatch(x -> "appointment".equals(x.type) && ap.getId().equals(x.id))).isTrue();
    }

    @Test
    void matchesAppointment_byIdOrSlotWhenPatientFieldsNotSearchable() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        LocalDateTime s = LocalDateTime.of(2031, 3, 3, 9, 15);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        Appointment a = mock(Appointment.class);
        when(a.isDeleted()).thenReturn(false);
        when(a.getPatient()).thenReturn(null);
        when(a.getId()).thenReturn("unique-appt-id-abc");
        when(a.getTimeSlot()).thenReturn(slot);
        ar.save(a);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search("unique-appt", 10).stream().anyMatch(r -> "appointment".equals(r.type))).isTrue();
        assertThat(g.search("2031", 10).stream().anyMatch(r -> "appointment".equals(r.type))).isTrue();
    }

    @Test
    void userMatch_emailWhenNameNullInRepositoryModel() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        UserRepository ur = mock(UserRepository.class);
        User u = mock(User.class);
        when(u.getId()).thenReturn("mock-u1");
        when(u.getName()).thenReturn(null);
        when(u.getEmail()).thenReturn("onlyedge@unique.org");
        when(ur.findAll()).thenReturn(List.of(u));
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search("onlyedge", 8).stream().anyMatch(r -> "user".equals(r.type))).isTrue();
    }

    @Test
    void matchesAppointment_patientWithNullNameButEmailMatch() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User p = mock(User.class);
        when(p.getName()).thenReturn(null);
        when(p.getEmail()).thenReturn("hidden@match.test");
        LocalDateTime s = LocalDateTime.of(2032, 4, 4, 11, 0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        Appointment ap = mock(Appointment.class);
        when(ap.isDeleted()).thenReturn(false);
        when(ap.getPatient()).thenReturn(p);
        when(ap.getId()).thenReturn("aid-emailonly");
        when(ap.getTimeSlot()).thenReturn(slot);
        when(ap.getStatus()).thenReturn("PENDING");
        ar.save(ap);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        List<GlobalSearchService.SearchResult> r = g.search("hidden@match", 10);
        assertThat(r.stream().filter(x -> "appointment".equals(x.type) && "aid-emailonly".equals(x.id))).isNotEmpty();
        assertThat(r.stream().filter(x -> "appointment".equals(x.type)).findFirst().orElseThrow().title)
                .contains("2032");
    }

    @Test
    void appointmentSubtitle_statusNull_branch() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("st", "Status Null", "sn@x.com", "x");
        ur.save(u);
        LocalDateTime s = LocalDateTime.now().plusDays(4);
        InPersonAppointment ap = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "R");
        ap.setStatus(null);
        ar.save(ap);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        GlobalSearchService.SearchResult hit = g.search("Status Null", 10).stream()
                .filter(x -> "appointment".equals(x.type))
                .findFirst()
                .orElseThrow();
        assertThat(hit.subtitle).isEqualTo("Status Null – ");
    }

    @Test
    void findAll_withNullAppointmentEntry_isFiltered() {
        AppointmentRepository ar = mock(AppointmentRepository.class);
        List<Appointment> withNull = new ArrayList<>();
        withNull.add(null);
        when(ar.findAll()).thenReturn(withNull);
        InMemoryUserRepository ur = new InMemoryUserRepository();
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search("anything", 10).stream().noneMatch(r -> "appointment".equals(r.type))).isTrue();
    }

    @Test
    void findAllContainingNull_skipsNullAppointments() {
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u-null", "Keep", "keep@x.com", "x");
        ur.save(u);
        LocalDateTime s = LocalDateTime.of(2032, 1, 10, 11, 0);
        InPersonAppointment ap = new InPersonAppointment(u, new TimeSlot(s, s.plusHours(1)), "R");
        AppointmentRepository ar = mock(AppointmentRepository.class);
        List<Appointment> mixed = new ArrayList<>();
        mixed.add(null);
        mixed.add(ap);
        when(ar.findAll()).thenReturn(mixed);
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search("Keep", 10).stream().filter(r -> "appointment".equals(r.type))).isNotEmpty();
    }

    @Test
    void limitsCombinedResultsToMaxResults() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryUserRepository ur = new InMemoryUserRepository();
        User u = new User("u1", "Limit User", "limit@x.com", "x");
        ur.save(u);
        LocalDateTime s = LocalDateTime.now().plusDays(2);
        for (int i = 0; i < 5; i++) {
            ar.save(new InPersonAppointment(u, new TimeSlot(s.plusHours(i), s.plusHours(i + 1)), "R"));
        }
        GlobalSearchService g = new GlobalSearchService(ar, ur);
        assertThat(g.search("Limit", 4)).hasSizeLessThanOrEqualTo(4);
    }
}
