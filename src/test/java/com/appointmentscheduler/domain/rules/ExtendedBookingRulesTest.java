package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.*;
import com.appointmentscheduler.application.AppConfig;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import com.appointmentscheduler.persistence.InMemoryDoctorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Branch-heavy domain rules: conflicts, capacity, follow-ups, cutoff, and working hours.
 */
@DisplayName("Extended booking rule strategies")
@ResourceLock("AppConfigProps")
class ExtendedBookingRulesTest {

    @Test
    void doctorConflict_detectsOverlap() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
        existing.setDoctorId("d1");
        repo.save(existing);
        DoctorConflictRuleStrategy rule = new DoctorConflictRuleStrategy(repo);
        InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
        candidate.setDoctorId("d1");
        assertThat(rule.isValid(candidate)).isFalse();
        candidate.setDoctorId("d2");
        assertThat(rule.isValid(candidate)).isTrue();
    }

    @Test
    void roomConflict_detectsOverlap() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
        existing.setRoomId("room1");
        repo.save(existing);
        RoomConflictRuleStrategy rule = new RoomConflictRuleStrategy(repo);
        InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
        candidate.setRoomId("room1");
        assertThat(rule.isValid(candidate)).isFalse();
    }

    @Test
    void roomConflict_differentRoom_sameSlot_isValid() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(15).withMinute(0).withSecond(0).withNano(0);
        TimeSlot slot = new TimeSlot(s, s.plusHours(1));
        InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
        existing.setRoomId("room1");
        repo.save(existing);
        RoomConflictRuleStrategy rule = new RoomConflictRuleStrategy(repo);
        InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
        candidate.setRoomId("room2");
        assertThat(rule.isValid(candidate)).isTrue();
    }

    @Test
    void workingHours_withinBusinessDay() {
        WorkingHoursRuleStrategy rule = new WorkingHoursRuleStrategy();
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TimeSlot ok = new TimeSlot(s, s.plusHours(1));
        assertThat(rule.isValid(new InPersonAppointment(p, ok, "L"))).isTrue();
    }

    @Test
    void maxPerDoctor_respectsLimit() {
        InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
        InMemoryDoctorRepository dr = new InMemoryDoctorRepository();
        Doctor d = new Doctor("d1", "Doc", "d@x.com", "S", 1);
        dr.save(d);
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime day = LocalDateTime.now().plusDays(4).withHour(9).withMinute(0).withSecond(0).withNano(0);
        TimeSlot t1 = new TimeSlot(day, day.plusHours(1));
        InPersonAppointment a1 = new InPersonAppointment(p, t1, "L");
        a1.setDoctorId("d1");
        ar.save(a1);
        MaxAppointmentsPerDoctorRuleStrategy rule = new MaxAppointmentsPerDoctorRuleStrategy(ar, dr);
        TimeSlot t2 = new TimeSlot(day.plusHours(2), day.plusHours(3));
        InPersonAppointment a2 = new InPersonAppointment(p, t2, "L");
        a2.setDoctorId("d1");
        assertThat(rule.isValid(a2)).isFalse();
    }

    @Test
    void followUp_requiresPriorCompleted() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime priorEnd = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        InPersonAppointment prior = new InPersonAppointment("prior1", p, new TimeSlot(priorEnd.minusHours(1), priorEnd), "L");
        prior.setStatus("COMPLETED");
        repo.save(prior);
        LocalDateTime fuStart = priorEnd.plusHours(1);
        FollowUpAppointment fu = new FollowUpAppointment(p, new TimeSlot(fuStart, fuStart.plusHours(1)), "prior1");
        FollowUpDependencyRuleStrategy rule = new FollowUpDependencyRuleStrategy(repo);
        assertThat(rule.isValid(fu)).isTrue();
    }

    @Test
    void bookingCutoff_futureSlotValid() {
        BookingCutoffRuleStrategy rule = new BookingCutoffRuleStrategy();
        User p = new User("p", "P", "p@x.com", "x");
        LocalDateTime s = LocalDateTime.now().plusDays(30);
        assertThat(rule.isValid(new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L"))).isTrue();
    }

    @Nested
    @DisplayName("BookingCutoffRuleStrategy branches")
    class CutoffBranches {

        @Test
        void nullAppointmentOrSlot_invalid() {
            BookingCutoffRuleStrategy rule = new BookingCutoffRuleStrategy();
            assertThat(rule.isValid(null)).isFalse();
        }

        @Test
        void slotTooSoonWithinCutoffWindow_invalid() {
            BookingCutoffRuleStrategy rule = new BookingCutoffRuleStrategy();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime start = LocalDateTime.now().plusMinutes(30);
            int cutoffHoursBefore = AppConfig.getInt("booking.cutoffHoursBefore", 2);
            // When cutoffHoursBefore is 0 (or negative), there is effectively no cutoff -> the slot should be valid.
            boolean expectedValid = cutoffHoursBefore <= 0;
            assertThat(rule.isValid(new InPersonAppointment(p, new TimeSlot(start, start.plusHours(1)), "L"))).isEqualTo(expectedValid);
        }
    }

    @Nested
    @DisplayName("WorkingHoursRuleStrategy branches")
    class WorkingHoursBranches {

        @Test
        void nullAppointment_invalid() {
            assertThat(new WorkingHoursRuleStrategy().isValid(null)).isFalse();
        }

        @Test
        void beforeBusinessOpen_invalid() {
            WorkingHoursRuleStrategy rule = new WorkingHoursRuleStrategy();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(5).withHour(6).withMinute(0).withSecond(0).withNano(0);
            assertThat(rule.isValid(new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L"))).isFalse();
        }

        @Test
        void afterBusinessClose_invalid() {
            WorkingHoursRuleStrategy rule = new WorkingHoursRuleStrategy();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(5).withHour(18).withMinute(0).withSecond(0).withNano(0);
            assertThat(rule.isValid(new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L"))).isFalse();
        }
    }

    @Nested
    @DisplayName("FollowUpDependencyRuleStrategy branches")
    class FollowUpBranches {

        @Test
        void nonFollowUp_alwaysValid() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            FollowUpDependencyRuleStrategy rule = new FollowUpDependencyRuleStrategy(repo);
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(1);
            assertThat(rule.isValid(new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "L"))).isTrue();
        }

        @Test
        void noPriorId_skipsDependencyCheck() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            FollowUpDependencyRuleStrategy rule = new FollowUpDependencyRuleStrategy(repo);
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2);
            FollowUpAppointment fu = new FollowUpAppointment(p, new TimeSlot(s, s.plusHours(1)), null);
            assertThat(rule.isValid(fu)).isTrue();
        }

        @Test
        void priorMissing_invalid() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            FollowUpDependencyRuleStrategy rule = new FollowUpDependencyRuleStrategy(repo);
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2);
            FollowUpAppointment fu = new FollowUpAppointment(p, new TimeSlot(s, s.plusHours(1)), "missing-id");
            assertThat(rule.isValid(fu)).isFalse();
        }

        @Test
        void priorWrongPatient_invalid() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p1 = new User("p1", "A", "a@x.com", "x");
            User p2 = new User("p2", "B", "b@x.com", "x");
            LocalDateTime t = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment prior = new InPersonAppointment("prior", p1, new TimeSlot(t, t.plusHours(1)), "L");
            prior.setStatus("COMPLETED");
            repo.save(prior);
            LocalDateTime fuStart = t.plusHours(2);
            FollowUpAppointment fu = new FollowUpAppointment(p2, new TimeSlot(fuStart, fuStart.plusHours(1)), "prior");
            assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(fu)).isFalse();
        }

        @Test
        void priorNotActive_invalid() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime t = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment prior = new InPersonAppointment("prior", p, new TimeSlot(t, t.plusHours(1)), "L");
            prior.setStatus("PENDING");
            repo.save(prior);
            LocalDateTime fuStart = t.plusHours(2);
            FollowUpAppointment fu = new FollowUpAppointment(p, new TimeSlot(fuStart, fuStart.plusHours(1)), "prior");
            assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(fu)).isFalse();
        }

        @Test
        void priorEndsAfterFollowUpStart_invalid() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime t = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment prior = new InPersonAppointment("prior", p, new TimeSlot(t, t.plusHours(2)), "L");
            prior.setStatus("COMPLETED");
            repo.save(prior);
            LocalDateTime fuStart = t.plusHours(1);
            FollowUpAppointment fu = new FollowUpAppointment(p, new TimeSlot(fuStart, fuStart.plusHours(1)), "prior");
            assertThat(new FollowUpDependencyRuleStrategy(repo).isValid(fu)).isFalse();
        }
    }

    @Nested
    @DisplayName("Doctor / room conflict edge cases")
    class ConflictEdges {

        @Test
        void doctorConflict_sameAppointmentIdAllowedForUpdate() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(s, s.plusHours(1));
            InPersonAppointment existing = new InPersonAppointment("same", p, slot, "R");
            existing.setDoctorId("d1");
            repo.save(existing);
            DoctorConflictRuleStrategy rule = new DoctorConflictRuleStrategy(repo);
            InPersonAppointment candidate = new InPersonAppointment("same", p, slot, "R");
            candidate.setDoctorId("d1");
            assertThat(rule.isValid(candidate)).isTrue();
        }

        @Test
        void roomConflict_sameAppointmentIdAllowedForUpdate() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(15).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(s, s.plusHours(1));
            InPersonAppointment existing = new InPersonAppointment("rid", p, slot, "R");
            existing.setRoomId("room1");
            repo.save(existing);
            RoomConflictRuleStrategy rule = new RoomConflictRuleStrategy(repo);
            InPersonAppointment candidate = new InPersonAppointment("rid", p, slot, "R");
            candidate.setRoomId("room1");
            assertThat(rule.isValid(candidate)).isTrue();
        }

        @Test
        void doctorConflict_nullDoctorId_skipsCheck() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(s, s.plusHours(1)), "R");
            assertThat(new DoctorConflictRuleStrategy(repo).isValid(a)).isTrue();
        }

        @Test
        void roomConflict_nullAppointment_skips() {
            assertThat(new RoomConflictRuleStrategy(new InMemoryAppointmentRepository()).isValid(null)).isTrue();
        }

        @Test
        void doctorConflict_nullAppointment_skips() {
            assertThat(new DoctorConflictRuleStrategy(new InMemoryAppointmentRepository()).isValid(null)).isTrue();
        }

        @Test
        void roomConflict_blankRoomId_skipsOverlapCheck() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(s, s.plusHours(1));
            InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
            existing.setRoomId("roomX");
            repo.save(existing);
            RoomConflictRuleStrategy rule = new RoomConflictRuleStrategy(repo);
            InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
            assertThat(rule.isValid(candidate)).isTrue();
        }

        @Test
        void roomConflict_expiredExisting_ignored() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(17).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(s, s.plusHours(1));
            InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
            existing.setRoomId("rE");
            existing.setStatus("EXPIRED");
            repo.save(existing);
            InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
            candidate.setRoomId("rE");
            assertThat(new RoomConflictRuleStrategy(repo).isValid(candidate)).isTrue();
        }

        @Test
        void roomConflict_deletedExisting_ignored() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(s, s.plusHours(1));
            InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
            existing.setRoomId("rD");
            existing.markDeleted("admin");
            repo.save(existing);
            InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
            candidate.setRoomId("rD");
            assertThat(new RoomConflictRuleStrategy(repo).isValid(candidate)).isTrue();
        }

        @Test
        void doctorConflict_cancelledExisting_ignored() {
            InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime s = LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0).withNano(0);
            TimeSlot slot = new TimeSlot(s, s.plusHours(1));
            InPersonAppointment existing = new InPersonAppointment(p, slot, "R");
            existing.setDoctorId("docC");
            existing.setStatus("CANCELLED");
            repo.save(existing);
            InPersonAppointment candidate = new InPersonAppointment(p, slot, "R2");
            candidate.setDoctorId("docC");
            assertThat(new DoctorConflictRuleStrategy(repo).isValid(candidate)).isTrue();
        }
    }

    @Nested
    @DisplayName("MaxAppointmentsPerDoctorRuleStrategy branches")
    class MaxPerDoctorBranches {

        @Test
        void unknownDoctorId_skipsLimit() {
            InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
            InMemoryDoctorRepository dr = new InMemoryDoctorRepository();
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime day = LocalDateTime.now().plusDays(6).withHour(9).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment a = new InPersonAppointment(p, new TimeSlot(day, day.plusHours(1)), "L");
            a.setDoctorId("not-in-repo");
            MaxAppointmentsPerDoctorRuleStrategy rule = new MaxAppointmentsPerDoctorRuleStrategy(ar, dr);
            assertThat(rule.isValid(a)).isTrue();
        }

        @Test
        void cancelledSameDayDoesNotConsumeCapacity() {
            InMemoryAppointmentRepository ar = new InMemoryAppointmentRepository();
            InMemoryDoctorRepository dr = new InMemoryDoctorRepository();
            Doctor d = new Doctor("d1", "Doc", "d@x.com", "S", 1);
            dr.save(d);
            User p = new User("p", "P", "p@x.com", "x");
            LocalDateTime day = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment cancelled = new InPersonAppointment(p, new TimeSlot(day, day.plusHours(1)), "L");
            cancelled.setDoctorId("d1");
            cancelled.setStatus("CANCELLED");
            ar.save(cancelled);
            InPersonAppointment neu = new InPersonAppointment(p, new TimeSlot(day.plusHours(2), day.plusHours(3)), "L");
            neu.setDoctorId("d1");
            assertThat(new MaxAppointmentsPerDoctorRuleStrategy(ar, dr).isValid(neu)).isTrue();
        }
    }
}
