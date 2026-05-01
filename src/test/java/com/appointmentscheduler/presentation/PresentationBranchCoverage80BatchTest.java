package com.appointmentscheduler.presentation;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.testsupport.JavaFxTestSupport;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Targeted presentation-layer branch coverage batch (dialogs, aggregates, notification store edges)
 * complementing existing controller/FX sweeps.
 */
class PresentationBranchCoverage80BatchTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestSupport.initPlatform();
    }

    @AfterEach
    void restoreAutoDialogs() {
        System.clearProperty("app.test.autoDialogs");
    }

    @Test
    void calendarView_dailyMode_emptyAppointments_buildsGrid() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        LocalDate anchor = LocalDate.of(2026, 6, 9);
        Platform.runLater(() -> {
            try {
                CalendarViewComponent cv = new CalendarViewComponent(
                        List.of(), anchor, CalendarViewComponent.ViewMode.DAILY);
                assertThat(cv.getChildren()).isNotEmpty();
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void calendarView_monthlyMode_emptyAppointments_buildsGrid() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        LocalDate anchor = LocalDate.of(2026, 6, 15);
        Platform.runLater(() -> {
            try {
                CalendarViewComponent cv = new CalendarViewComponent(
                        List.of(), anchor, CalendarViewComponent.ViewMode.MONTHLY);
                assertThat(cv.getChildren()).isNotEmpty();
            } finally {
                done.countDown();
            }
        });
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void patronBookingSummary_twoSeparatePatients_producesTwoRows() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 12, 0);
        User a = new User("pa", "User A", "a@t.com", "h");
        User b = new User("pb", "User B", "b@t.com", "h");
        InPersonAppointment x = new InPersonAppointment("x1", a, new TimeSlot(now.plusDays(1), now.plusDays(1).plusHours(1)), "L");
        x.setStatus("CONFIRMED");
        InPersonAppointment y = new InPersonAppointment("y1", b, new TimeSlot(now.plusDays(2), now.plusDays(2).plusHours(1)), "L");
        y.setStatus("PENDING");
        List<PatronBookingSummary> rows = PatronBookingSummary.build(List.of(x, y), Map.of(), null, now);
        assertThat(rows).hasSize(2);
    }

    @Test
    void appNotificationStore_getRecent_whenSizeEqualsMax_returnsFullListNotTailSlice() {
        AppNotificationStore store = new AppNotificationStore();
        IntStream.range(0, 4).forEach(i -> store.add("t" + i, "m"));
        assertThat(store.getRecent(4)).hasSize(4);
        assertThat(store.getRecent(4).get(0).getTitle()).isEqualTo("t0");
    }

    @Test
    void appNotificationStore_getUnreadCount_capsAtStoredMaxNotNinetyNineWhenUnderFiftyEntries() {
        AppNotificationStore store = new AppNotificationStore();
        IntStream.range(0, 30).forEach(i -> store.add("n", "m"));
        assertThat(store.getUnreadCount()).isEqualTo(30);
    }
}
