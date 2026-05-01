package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Branch coverage for {@link PatronBookingSummary#build} (admin patron list aggregation).
 */
class PatronBookingSummaryTest {

    private static User patient(String id) {
        return new User(id, "Name " + id, id + "@test.com", "h");
    }

    private static InPersonAppointment appt(String id, User p, LocalDateTime start, String status, String clinicId) {
        InPersonAppointment a = new InPersonAppointment(id, p, new TimeSlot(start, start.plusHours(1)), "L");
        a.setStatus(status);
        a.setClinicId(clinicId);
        return a;
    }

    @Test
    void emptyInput_returnsEmpty() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        assertThat(PatronBookingSummary.build(List.of(), Map.of(), null, now)).isEmpty();
    }

    @Test
    void deletedAndNullAppointments_filtered() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p1");
        InPersonAppointment ok = appt("a1", p, now.plusDays(1), "CONFIRMED", null);
        InPersonAppointment del = appt("a2", p, now.plusDays(2), "CONFIRMED", null);
        del.setDeletedState(true, now, "x");
        // List.of forbids null elements; use a mutable list to include null for the stream filter.
        assertThat(PatronBookingSummary.build(Arrays.asList(ok, null, del), Map.of(), null, now)).hasSize(1);
    }

    @Test
    void clinicFilter_excludesOtherBranch() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p2");
        InPersonAppointment inC = appt("c1", p, now.plusDays(1), "CONFIRMED", "clinic-A");
        InPersonAppointment outC = appt("c2", p, now.plusDays(2), "CONFIRMED", "clinic-B");
        List<PatronBookingSummary> rows = PatronBookingSummary.build(
                List.of(inC, outC), Map.of(), "clinic-A", now);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getTotalBookings()).isEqualTo(1);
    }

    @Test
    void clinicFilter_legacyNullClinicOnAppointment_stillVisible() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p3");
        InPersonAppointment legacy = appt("leg", p, now.plusDays(1), "PENDING", null);
        List<PatronBookingSummary> rows = PatronBookingSummary.build(List.of(legacy), Map.of(), "any-clinic", now);
        assertThat(rows).hasSize(1);
    }

    @Test
    void clinicFilter_nullClinicIdParam_includesAll() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p4");
        InPersonAppointment a = appt("x", p, now.plusDays(1), "CONFIRMED", "clinic-X");
        InPersonAppointment b = appt("y", p, now.plusDays(2), "CONFIRMED", "clinic-Y");
        assertThat(PatronBookingSummary.build(List.of(a, b), Map.of(), null, now)).hasSize(1);
        assertThat(PatronBookingSummary.build(List.of(a, b), Map.of(), "", now)).hasSize(1);
    }

    @Test
    void inactiveStatuses_reduceActiveCount() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p5");
        InPersonAppointment a1 = appt("i1", p, now.plusDays(1), "CANCELLED", null);
        InPersonAppointment a2 = appt("i2", p, now.plusDays(2), "EXPIRED", null);
        InPersonAppointment a2b = appt("i2b", p, now.plusDays(2).plusHours(1), "expired", null);
        InPersonAppointment a3 = appt("i3", p, now.plusDays(3), "CONFIRMED", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(a1, a2, a2b, a3), Map.of(), null, now).get(0);
        assertThat(row.getTotalBookings()).isEqualTo(4);
        assertThat(row.getActiveBookings()).isEqualTo(1);
    }

    @Test
    void nextUpcoming_ignoresPastSlots() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p6");
        InPersonAppointment past = appt("past", p, now.minusDays(1), "CONFIRMED", null);
        InPersonAppointment future = appt("fut", p, now.plusDays(5), "CONFIRMED", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(past, future), Map.of(), null, now).get(0);
        assertThat(row.getNextUpcomingAt()).isEqualTo(future.getTimeSlot().getStartTime());
    }

    @Test
    void nextUpcoming_excludesFutureExpiredSlots() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-exp-next");
        InPersonAppointment expiredFuture = appt("ef", p, now.plusDays(2), "EXPIRED", null);
        InPersonAppointment confirmedLater = appt("ok", p, now.plusDays(5), "CONFIRMED", null);
        PatronBookingSummary row = PatronBookingSummary.build(
                List.of(expiredFuture, confirmedLater), Map.of(), null, now).get(0);
        assertThat(row.getNextUpcomingAt()).isEqualTo(confirmedLater.getTimeSlot().getStartTime());
    }

    @Test
    void usersById_overridesPatientReference() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p7");
        User display = new User("p7", "Display Name", "p7@test.com", "h");
        InPersonAppointment a = appt("u1", p, now.plusDays(1), "CONFIRMED", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(a), Map.of("p7", display), null, now).get(0);
        assertThat(row.getUser().getName()).isEqualTo("Display Name");
    }

    @Test
    void usersByIdNull_usesPatientFromAppointment() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-null-map");
        InPersonAppointment a = appt("only", p, now.plusDays(1), "CONFIRMED", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(a), null, null, now).get(0);
        assertThat(row.getUser()).isSameAs(p);
    }

    @Test
    void usersByIdMissingPatientKey_usesPatientFromAppointment() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-map-miss");
        InPersonAppointment a = appt("m1", p, now.plusDays(1), "CONFIRMED", null);
        User unrelated = new User("other-id", "Other", "o@o.com", "h");
        PatronBookingSummary row = PatronBookingSummary.build(
                List.of(a), Map.of("unrelated-key", unrelated), null, now).get(0);
        assertThat(row.getUser().getId()).isEqualTo("p-map-miss");
    }

    @Test
    void statsLines_includeArabicAndEnglishPlaceholders() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p8");
        // Past-only active slot => lastBooking set, no upcoming (next line uses "لا يوجد" / "None").
        InPersonAppointment pastOnly = appt("s1", p, now.minusDays(1), "CONFIRMED", null);
        PatronBookingSummary row = PatronBookingSummary.build(
                List.of(pastOnly),
                Map.of(),
                null,
                now).get(0);
        assertThat(row.arabicStatsLine()).contains("حجوزات");
        assertThat(row.arabicStatsLine()).contains("لا يوجد");
        assertThat(row.englishStatsLine()).contains("Bookings:");
        assertThat(row.englishStatsLine()).contains("None");
    }

    @Test
    void statsLines_whenLastAndNextPresent_useFormattedTimestampsNotPlaceholders() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
        User p = patient("p-both-ts");
        InPersonAppointment past = appt("pb-past", p, now.minusDays(3), "CONFIRMED", null);
        InPersonAppointment upcoming = appt("pb-next", p, now.plusDays(2), "PENDING", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(past, upcoming), Map.of(), null, now).get(0);
        assertThat(row.getLastBookingAt()).isNotNull();
        assertThat(row.getNextUpcomingAt()).isNotNull();
        assertThat(row.englishStatsLine()).doesNotContain("None");
        assertThat(row.arabicStatsLine()).doesNotContain("لا يوجد");
        assertThat(row.englishStatsLine()).contains("2026-06");
        assertThat(row.arabicStatsLine()).contains("2026-06");
    }

    @Test
    void lastBooking_isMaxStartAmongAll() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p9");
        InPersonAppointment early = appt("e1", p, now.plusDays(1), "CONFIRMED", null);
        InPersonAppointment late = appt("e2", p, now.plusDays(10), "CANCELLED", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(early, late), Map.of(), null, now).get(0);
        assertThat(row.getLastBookingAt()).isEqualTo(late.getTimeSlot().getStartTime());
    }

    @Test
    void allCancelled_noNextUpcoming() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-all-can");
        InPersonAppointment a1 = appt("xc1", p, now.plusDays(1), "CANCELLED", null);
        InPersonAppointment a2 = appt("xc2", p, now.plusDays(2), "CANCELLED", null);
        PatronBookingSummary row = PatronBookingSummary.build(List.of(a1, a2), Map.of(), null, now).get(0);
        assertThat(row.getNextUpcomingAt()).isNull();
    }

    @Test
    void clinicFilter_emptyClinicIdOnAppointment_stillMatchesAnyBranchFilter() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-empty-aid");
        InPersonAppointment a = appt("eid", p, now.plusDays(1), "CONFIRMED", "");
        assertThat(PatronBookingSummary.build(List.of(a), Map.of(), "clinic-Z", now)).hasSize(1);
    }

    @Test
    void build_filtersAppointmentWithNullPatient() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        Appointment a = mock(Appointment.class);
        when(a.isDeleted()).thenReturn(false);
        when(a.getPatient()).thenReturn(null);
        when(a.getClinicId()).thenReturn(null);
        assertThat(PatronBookingSummary.build(List.of(a), Map.of(), null, now)).isEmpty();
    }

    @Test
    void build_filtersAppointmentWithNullTimeSlot() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-null-slot");
        Appointment a = mock(Appointment.class);
        when(a.isDeleted()).thenReturn(false);
        when(a.getPatient()).thenReturn(p);
        when(a.getTimeSlot()).thenReturn(null);
        when(a.getClinicId()).thenReturn(null);
        assertThat(PatronBookingSummary.build(List.of(a), Map.of(), null, now)).isEmpty();
    }

    @Test
    void build_filtersAppointmentWithNullSlotStartTime() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        User p = patient("p-null-start");
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getStartTime()).thenReturn(null);
        Appointment a = mock(Appointment.class);
        when(a.isDeleted()).thenReturn(false);
        when(a.getPatient()).thenReturn(p);
        when(a.getTimeSlot()).thenReturn(slot);
        when(a.getClinicId()).thenReturn(null);
        assertThat(PatronBookingSummary.build(List.of(a), Map.of(), null, now)).isEmpty();
    }

    @Test
    void sort_tieBreaksByPatientNameWhenLastBookingEqual() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        LocalDateTime sameStart = now.plusDays(3);
        User ann = new User("id-ann", "Ann", "ann@test.com", "h");
        User bob = new User("id-bob", "Bob", "bob@test.com", "h");
        InPersonAppointment aAnn = appt("ta", ann, sameStart, "CONFIRMED", null);
        InPersonAppointment aBob = appt("tb", bob, sameStart, "CONFIRMED", null);
        List<PatronBookingSummary> rows = PatronBookingSummary.build(List.of(aBob, aAnn), Map.of(), null, now);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getUser().getName()).isEqualTo("Ann");
        assertThat(rows.get(1).getUser().getName()).isEqualTo("Bob");
    }
}
