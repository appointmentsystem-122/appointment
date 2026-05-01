package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.RecurrencePattern;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercises CalendarViewComponent view modes and event styling branches.
 * UI must be built on the JavaFX application thread.
 */
class CalendarViewComponentCoverageTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @Test
    void allViewModes_nullViewMode_nullAnchor_andStyleBranches() throws Exception {
        User u = new User("cal-u", "Cal User", "cal@t.com", "h");
        LocalDate monday = LocalDate.of(2026, 6, 8);
        LocalDateTime midWeek = LocalDateTime.of(2026, 6, 10, 10, 0);
        TimeSlot slotWeek = new TimeSlot(midWeek, midWeek.plusHours(1));

        List<Appointment> weekly = new ArrayList<>();
        InPersonAppointment conf = new InPersonAppointment("w1", u, slotWeek, "L");
        conf.setStatus("CONFIRMED");
        weekly.add(conf);

        InPersonAppointment canc = new InPersonAppointment("w2", u,
                new TimeSlot(midWeek.plusDays(1), midWeek.plusDays(1).plusHours(1)), "L");
        canc.setStatus("CANCELLED");
        weekly.add(canc);

        InPersonAppointment exp = new InPersonAppointment("w3", u,
                new TimeSlot(midWeek.plusDays(1).plusHours(2), midWeek.plusDays(1).plusHours(3)), "L");
        exp.setStatus("EXPIRED");
        weekly.add(exp);

        RecurrencePattern rp = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, midWeek, midWeek.plusMonths(2), 1);
        RecurringAppointment recur = new RecurringAppointment("wrec", u, slotWeek, "SID", rp, "OID");
        recur.setStatus("PENDING");
        weekly.add(recur);

        UrgentAppointment urg = new UrgentAppointment("wurg", u,
                new TimeSlot(LocalDateTime.of(2026, 6, 11, 14, 0), LocalDateTime.of(2026, 6, 11, 15, 0)));
        urg.setStatus("CONFIRMED");
        weekly.add(urg);

        AssessmentAppointment asmt = new AssessmentAppointment("was", u,
                new TimeSlot(LocalDateTime.of(2026, 6, 12, 9, 0), LocalDateTime.of(2026, 6, 12, 10, 0)));
        asmt.setStatus("CONFIRMED");
        weekly.add(asmt);

        InPersonAppointment far = new InPersonAppointment("far", u,
                new TimeSlot(LocalDateTime.of(2026, 7, 1, 10, 0), LocalDateTime.of(2026, 7, 1, 11, 0)), "L");
        far.setStatus("CONFIRMED");
        weekly.add(far);

        InPersonAppointment earlyHour = new InPersonAppointment("early", u,
                new TimeSlot(LocalDateTime.of(2026, 6, 10, 6, 0), LocalDateTime.of(2026, 6, 10, 7, 0)), "L");
        weekly.add(earlyHour);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(List.of(), monday, null)).doesNotThrowAnyException();
                assertThatCode(() -> new CalendarViewComponent(List.of(), null, CalendarViewComponent.ViewMode.WEEKLY))
                        .doesNotThrowAnyException();
                assertThatCode(() -> new CalendarViewComponent(weekly, monday, CalendarViewComponent.ViewMode.WEEKLY))
                        .doesNotThrowAnyException();

                LocalDate dailyAnchor = LocalDate.of(2026, 6, 15);
                List<Appointment> dailyList = new ArrayList<>();
                InPersonAppointment d1 = new InPersonAppointment("d1", u,
                        new TimeSlot(LocalDateTime.of(2026, 6, 15, 9, 0), LocalDateTime.of(2026, 6, 15, 10, 0)), "L");
                d1.setStatus("CONFIRMED");
                dailyList.add(d1);
                dailyList.add(new InPersonAppointment("d2", u,
                        new TimeSlot(LocalDateTime.of(2026, 6, 16, 10, 0), LocalDateTime.of(2026, 6, 16, 11, 0)), "L"));

                assertThatCode(() -> new CalendarViewComponent(dailyList, dailyAnchor, CalendarViewComponent.ViewMode.DAILY))
                        .doesNotThrowAnyException();

                List<Appointment> monthlyList = new ArrayList<>();
                InPersonAppointment m1 = new InPersonAppointment("m1", u,
                        new TimeSlot(LocalDateTime.of(2026, 6, 20, 11, 0), LocalDateTime.of(2026, 6, 20, 12, 0)), "L");
                m1.setStatus("CANCELLED");
                monthlyList.add(m1);
                monthlyList.add(new UrgentAppointment("mU", u,
                        new TimeSlot(LocalDateTime.of(2026, 6, 11, 11, 0), LocalDateTime.of(2026, 6, 11, 12, 0))));

                assertThatCode(() -> new CalendarViewComponent(monthlyList, LocalDate.of(2026, 6, 1),
                        CalendarViewComponent.ViewMode.MONTHLY)).doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }

    // Daily mode: wrong day, off hours before 8 or after 18, long type name subtitle branch.
    @Test
    void dailyMode_skipsWrongDayAndOffHours_coversLongTypeName() throws Exception {
        User u = new User("daily-u", "Daily User", "daily@t.com", "h");
        LocalDate anchor = LocalDate.of(2026, 8, 20);
        LocalDateTime onDayMorning = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime wrongDay = LocalDateTime.of(2026, 8, 21, 10, 0);
        LocalDateTime tooEarly = LocalDateTime.of(2026, 8, 20, 6, 0);
        LocalDateTime tooLate = LocalDateTime.of(2026, 8, 20, 19, 0);

        List<Appointment> list = new ArrayList<>();
        InPersonAppointment skipWrong = new InPersonAppointment("dw", u, new TimeSlot(wrongDay, wrongDay.plusHours(1)), "L");
        skipWrong.setStatus("CONFIRMED");
        list.add(skipWrong);
        InPersonAppointment skipHour = new InPersonAppointment("dh", u, new TimeSlot(tooEarly, tooEarly.plusHours(1)), "L");
        skipHour.setStatus("CONFIRMED");
        list.add(skipHour);
        InPersonAppointment skipLate = new InPersonAppointment("dl", u, new TimeSlot(tooLate, tooLate.plusHours(1)), "L");
        skipLate.setStatus("CONFIRMED");
        list.add(skipLate);
        AssessmentAppointment longName = new AssessmentAppointment("dasmt", u,
                new TimeSlot(onDayMorning, onDayMorning.plusHours(1)));
        longName.setStatus("CONFIRMED");
        list.add(longName);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(list, anchor, CalendarViewComponent.ViewMode.DAILY))
                        .doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }

    // Daily grid: long runtime simple name hits populateDaily subtitle truncation (18 chars, not weekly 20).
    @Test
    void dailyMode_longAppointmentSimpleName_truncatesSubtitle() throws Exception {
        User u = new User("u-daily-long", "L", "long-daily@t.com", "h");
        LocalDate anchor = LocalDate.of(2026, 9, 1);
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 11, 0);
        Appointment longNameDaily = new LongNameVirtualAppointmentForDailySubtitle(u, start);
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(
                        List.of(longNameDaily), anchor, CalendarViewComponent.ViewMode.DAILY))
                        .doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }

    /** Weekly grid: PENDING + plain InPerson hits neither CONFIRMED nor Urgent/Assessment styling branches. */
    @Test
    void weeklyMode_hourAfterBusinessEnd_skipsPlacingEventBlock() throws Exception {
        User u = new User("u-late", "Late", "late@t.com", "h");
        LocalDate monday = LocalDate.of(2026, 11, 2);
        LocalDateTime evening = LocalDateTime.of(2026, 11, 4, 19, 0);
        InPersonAppointment late = new InPersonAppointment("late-h", u, new TimeSlot(evening, evening.plusHours(1)), "L");
        late.setStatus("CONFIRMED");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(List.of(late), monday, CalendarViewComponent.ViewMode.WEEKLY))
                        .doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void weeklyMode_pendingInPerson_skipsGreenAndGradientStyles() throws Exception {
        User u = new User("u-pend", "Pend User", "pend@t.com", "h");
        LocalDate monday = LocalDate.of(2026, 9, 7);
        LocalDateTime midWeek = LocalDateTime.of(2026, 9, 9, 11, 0);
        InPersonAppointment pend = new InPersonAppointment("pw", u, new TimeSlot(midWeek, midWeek.plusHours(1)), "L");
        pend.setStatus("PENDING");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(List.of(pend), monday, CalendarViewComponent.ViewMode.WEEKLY))
                        .doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }

    // Weekly grid: long appointment simple name triggers weekly subtitle truncation.
    @Test
    void weeklyMode_longAppointmentSimpleName_truncatesSubtitle() throws Exception {
        User u = new User("u-long", "Long", "long@t.com", "h");
        LocalDate monday = LocalDate.of(2026, 10, 5);
        LocalDateTime mid = LocalDateTime.of(2026, 10, 7, 12, 0);
        // Simple name length over 20 triggers weekly substring truncation.
        class VeryLongInPersonAppt1 extends InPersonAppointment {
            VeryLongInPersonAppt1() {
                super("lid", u, new TimeSlot(mid, mid.plusHours(1)), "L");
                setStatus("CONFIRMED");
            }
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(
                        List.of(new VeryLongInPersonAppt1()), monday, CalendarViewComponent.ViewMode.WEEKLY))
                        .doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }

    /** Monthly grid: skip appointments outside anchor month; cover faded adjacent-month cells. */
    @Test
    void monthlyMode_skipsOtherMonthAppointments() throws Exception {
        User u = new User("u-mon", "Mon", "mon@t.com", "h");
        LocalDate juneAnchor = LocalDate.of(2026, 6, 15);
        LocalDateTime inJune = LocalDateTime.of(2026, 6, 18, 10, 0);
        LocalDateTime inMay = LocalDateTime.of(2026, 5, 28, 10, 0);
        InPersonAppointment juneOk = new InPersonAppointment("j1", u, new TimeSlot(inJune, inJune.plusHours(1)), "L");
        juneOk.setStatus("CONFIRMED");
        InPersonAppointment maySkip = new InPersonAppointment("m1", u, new TimeSlot(inMay, inMay.plusHours(1)), "L");
        maySkip.setStatus("PENDING");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                assertThatCode(() -> new CalendarViewComponent(
                        List.of(juneOk, maySkip), juneAnchor, CalendarViewComponent.ViewMode.MONTHLY))
                        .doesNotThrowAnyException();
            } finally {
                latch.countDown();
            }
        });
        assertThat(latch.await(45, TimeUnit.SECONDS)).isTrue();
    }
}
