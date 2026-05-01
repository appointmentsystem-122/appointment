package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.DoctorRepository;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Targets branch gaps in booking rule strategies (conflict checks, cut-off, working hours, follow-ups).
 */
class DomainRulesBranchCoverageTest {

    @AfterEach
    void restoreAppConfig() throws Exception {
        Method reload = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("reloadClasspathPropertiesForTest");
        reload.setAccessible(true);
        reload.invoke(null);
    }

    private static void applyProps(Properties p) throws Exception {
        Method apply = Class.forName("com.appointmentscheduler.application.AppConfig")
                .getDeclaredMethod("applyPropertiesForTest", Properties.class);
        apply.setAccessible(true);
        apply.invoke(null, p);
    }

    @Test
    void doctorConflict_skipsNullDoctorOrNullSlot() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        DoctorConflictRuleStrategy s = new DoctorConflictRuleStrategy(repo);
        User p = new User("1", "n", "e", "h");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        InPersonAppointment ap = new InPersonAppointment(p, slot, "R");
        assertThat(s.isValid(null)).isTrue();
        assertThat(s.isValid(ap)).isTrue();
        ap.setDoctorId("d1");
        assertThat(s.isValid(ap)).isTrue();
    }

    @Test
    void doctorConflict_overlapDifferentIdsFails_sameIdIgnored() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("1", "n", "e", "h");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        InPersonAppointment existing = new InPersonAppointment("ea", p, slot, "R");
        existing.setDoctorId("d1");
        existing.setStatus("CONFIRMED");
        repo.save(existing);

        InPersonAppointment overlap = new InPersonAppointment("eb", p, slot, "R");
        overlap.setDoctorId("d1");
        DoctorConflictRuleStrategy s = new DoctorConflictRuleStrategy(repo);
        assertThat(s.isValid(overlap)).isFalse();

        InPersonAppointment sameIdNewSlot = new InPersonAppointment("ea", p, slot, "R");
        sameIdNewSlot.setDoctorId("d1");
        assertThat(s.isValid(sameIdNewSlot)).isTrue();
    }

    @Test
    void doctorConflict_skipsCancelledAndExpired() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("1", "n", "e", "h");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        InPersonAppointment cancelled = new InPersonAppointment("c1", p, slot, "R");
        cancelled.setDoctorId("d1");
        cancelled.setStatus("CANCELLED");
        repo.save(cancelled);

        InPersonAppointment candidate = new InPersonAppointment("c2", p, slot, "R");
        candidate.setDoctorId("d1");
        assertThat(new DoctorConflictRuleStrategy(repo).isValid(candidate)).isTrue();
    }

    @Test
    void roomConflict_overlapDifferentIdsFails() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("1", "n", "e", "h");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        InPersonAppointment existing = new InPersonAppointment("r1", p, slot, "R");
        existing.setRoomId("room-a");
        existing.setStatus("CONFIRMED");
        repo.save(existing);

        InPersonAppointment overlap = new InPersonAppointment("r2", p, slot, "R");
        overlap.setRoomId("room-a");
        assertThat(new RoomConflictRuleStrategy(repo).isValid(overlap)).isFalse();
    }

    @Test
    void maxPerDoctor_blocksWhenAtCapacity() {
        InMemoryAppointmentRepository apptRepo = new InMemoryAppointmentRepository();
        DoctorRepository docRepo = mock(DoctorRepository.class);
        Doctor doc = new Doctor("doc-x", "Dr", "d@d.com", "g", 1, "clinic");
        when(docRepo.findById("doc-x")).thenReturn(Optional.of(doc));

        User p = new User("1", "n", "e", "h");
        LocalDateTime day = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < 1; i++) {
            LocalDateTime s = day.plusHours(i);
            InPersonAppointment a = new InPersonAppointment("m" + i, p, new TimeSlot(s, s.plusMinutes(30)), "R");
            a.setDoctorId("doc-x");
            a.setStatus("CONFIRMED");
            apptRepo.save(a);
        }
        MaxAppointmentsPerDoctorRuleStrategy rule = new MaxAppointmentsPerDoctorRuleStrategy(apptRepo, docRepo);
        InPersonAppointment next = new InPersonAppointment("m-new", p, new TimeSlot(day.plusHours(2), day.plusHours(3)), "R");
        next.setDoctorId("doc-x");
        assertThat(rule.isValid(next)).isFalse();

        when(docRepo.findById("doc-y")).thenReturn(Optional.empty());
        InPersonAppointment unknownDoc = new InPersonAppointment("m2", p, new TimeSlot(day, day.plusHours(1)), "R");
        unknownDoc.setDoctorId("doc-y");
        assertThat(rule.isValid(unknownDoc)).isTrue();
    }

    @Test
    void workingHours_endHourBoundary() throws Exception {
        Properties props = new Properties();
        props.setProperty("business.hourStart", "8");
        props.setProperty("business.hourEnd", "18");
        applyProps(props);
        WorkingHoursRuleStrategy s = new WorkingHoursRuleStrategy();
        LocalDateTime base = LocalDateTime.now().plusWeeks(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOk = base.withHour(8);
        TimeSlot inside = new TimeSlot(startOk, startOk.withHour(17).withMinute(59));
        assertThat(s.isValid(new InPersonAppointment(new User("1", "n", "e", "h"), inside, "R"))).isTrue();

        TimeSlot lateEnd = new TimeSlot(base.withHour(10), base.withHour(19));
        assertThat(s.isValid(new InPersonAppointment(new User("1", "n", "e", "h"), lateEnd, "R"))).isFalse();
        assertThat(s.isValid(null)).isFalse();
    }

    @Test
    void bookingCutoff_respectsProperty() throws Exception {
        Properties props = new Properties();
        props.setProperty("booking.cutoffHoursBefore", "0");
        applyProps(props);
        BookingCutoffRuleStrategy s = new BookingCutoffRuleStrategy();
        LocalDateTime start = LocalDateTime.now().plusMinutes(30);
        TimeSlot slot = new TimeSlot(start, start.plusHours(1));
        assertThat(s.isValid(new InPersonAppointment(new User("1", "n", "e", "h"), slot, "R"))).isTrue();
    }

    @Test
    void followUpDependency_priorMustExistAndMatchPatient() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("1", "n", "e", "h");
        User other = new User("2", "o", "o@o.com", "h");
        LocalDateTime t0 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot priorSlot = new TimeSlot(t0, t0.plusHours(1));
        InPersonAppointment prior = new InPersonAppointment("prior-1", p, priorSlot, "R");
        prior.setStatus("COMPLETED");
        repo.save(prior);

        LocalDateTime t1 = t0.plusHours(3);
        FollowUpAppointment fu = new FollowUpAppointment("fu-1", p, new TimeSlot(t1, t1.plusHours(1)), "prior-1");
        assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(fu)).isTrue();

        FollowUpAppointment wrongPrior = new FollowUpAppointment("fu-2", p, new TimeSlot(t1, t1.plusHours(1)), "nope");
        assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(wrongPrior)).isFalse();

        InPersonAppointment priorOther = new InPersonAppointment("prior-2", other, priorSlot, "R");
        priorOther.setStatus("COMPLETED");
        repo.save(priorOther);
        FollowUpAppointment wrongPatient = new FollowUpAppointment("fu-3", p, new TimeSlot(t1, t1.plusHours(1)), "prior-2");
        assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(wrongPatient)).isFalse();

        FollowUpAppointment noPrior = new FollowUpAppointment("fu-4", p, new TimeSlot(t1, t1.plusHours(1)), "");
        assertThat(noPrior.hasPriorAppointment()).isFalse();
        assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(noPrior)).isTrue();
    }

    @Test
    void followUpDependency_priorNotCompletedFails() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("1", "n", "e", "h");
        LocalDateTime t0 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot priorSlot = new TimeSlot(t0, t0.plusHours(1));
        InPersonAppointment prior = new InPersonAppointment("prior-nc", p, priorSlot, "R");
        prior.setStatus("PENDING");
        repo.save(prior);
        LocalDateTime t1 = t0.plusHours(4);
        FollowUpAppointment fu = new FollowUpAppointment("fu-nc", p, new TimeSlot(t1, t1.plusHours(1)), "prior-nc");
        assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(fu)).isFalse();
    }
}
