package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.AppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.mockito.Answers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Master schedule loading, slot generation, and closed-day integration.
 */
@DisplayName("ScheduleService")
@ResourceLock("AppConfigProps")
class ScheduleServiceTest {

    private AppointmentRepository repo;
    private ScheduleService scheduleService;

    private void setupScheduleService() {
        Properties p = new Properties();
        p.setProperty("business.hourStart", "8");
        p.setProperty("business.hourEnd", "18");
        AppConfig.applyPropertiesForTest(p);
        repo = mock(AppointmentRepository.class);
        when(repo.findAll()).thenReturn(Collections.emptyList());
        scheduleService = new ScheduleService(repo);
    }

    @BeforeEach
    void setUp() {
        setupScheduleService();
    }

    @AfterEach
    void tearDown() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Nested
    @DisplayName("Slot discovery")
    class Slots {

        @BeforeEach
        void refreshForSlotTests() {
            setupScheduleService();
        }

        @Test
        @DisplayName("Future day within business hours yields at least one slot when empty")
        void availableSlotsFutureDay() {
            LocalDate future = LocalDate.now().plusDays(7);
            assertThat(scheduleService.getAvailableSlots(future)).isNotEmpty();
        }

        @Test
        @DisplayName("Custom duration (30 min) is honoured in the grid")
        void availableSlots30Minutes() {
            LocalDate future = LocalDate.now().plusDays(10);
            List<TimeSlot> slots = scheduleService.getAvailableSlots(future, 30);
            assertThat(slots).as("need non-empty slots (business.hourStart < hourEnd in test application.properties)").isNotEmpty();
            TimeSlot first = slots.get(0);
            assertThat(Duration.between(first.getStartTime(), first.getEndTime()).toMinutes()).isEqualTo(30);
        }

        @Test
        @DisplayName("Admin-closed day returns no slots")
        void closedDay_returnsEmpty() {
            Preferences prefs = Preferences.userNodeForPackage(ClosedDayService.class);
            prefs.remove("admin.closedDays");

            ClosedDayService closed = new ClosedDayService();
            LocalDate d = LocalDate.now().plusDays(5);
            closed.addClosedDay(d);
            scheduleService.setClosedDayService(closed);
            assertThat(scheduleService.getAvailableSlots(d)).isEmpty();

            // Defensive: ensure the test setup matches the expectation.
            assertThat(closed.isDayClosed(d)).isTrue();

            prefs.remove("admin.closedDays");
        }

        @Test
        @DisplayName("Cancelled overlapping appointment does not consume the slot in the grid")
        void cancelledOverlap_doesNotBlockSlot() {
            User p = new User("u-can", "U", "u-can@t.com", "x");
            LocalDate future = LocalDate.now().plusDays(11);
            LocalDateTime start = future.atTime(9, 0);
            TimeSlot occupied = new TimeSlot(start, start.plusHours(1));
            InPersonAppointment cancelled = new InPersonAppointment("cx", p, occupied, "Loc");
            cancelled.setStatus("CANCELLED");
            when(repo.findAll()).thenReturn(List.of(cancelled));
            ScheduleService svc = new ScheduleService(repo);
            List<TimeSlot> slots = svc.getAvailableSlots(future, 60);
            assertThat(slots.stream().anyMatch(s -> s.getStartTime().equals(start))).isTrue();
        }

        @Test
        @DisplayName("Next available slot exists when repository is empty")
        void nextSlotWhenEmpty() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThat(scheduleService.getAvailableSlots(tomorrow, 60))
                    .as("tomorrow must have grid slots when repo is empty and hours are 8–18")
                    .isNotEmpty();
            assertThat(scheduleService.getNextAvailableSlot(60)).isPresent();
        }
    }

    @Nested
    @DisplayName("Bookability")
    class Bookability {

        @BeforeEach
        void refreshForBookability() {
            setupScheduleService();
        }

        @Test
        @DisplayName("Future date with free capacity is bookable")
        void futureDateBookable() {
            assertThat(scheduleService.isDateBookable(LocalDate.now().plusDays(14))).isTrue();
        }

        @Test
        @DisplayName("Past calendar days are never bookable")
        void pastDateNotBookable() {
            // Use a "deep past" date to avoid flakiness around midnight where LocalDate.now()
            // could change between test setup and method execution.
            assertThat(scheduleService.isDateBookable(LocalDate.now().minusDays(10))).isFalse();
        }

        @Test
        @DisplayName("Null date is rejected")
        void nullDateNotBookable() {
            assertThat(scheduleService.isDateBookable(null)).isFalse();
        }

        @Test
        @DisplayName("Today after business hours: slots exist but none start at or after now → not bookable")
        void today_allSlotsStartBeforeNow_notBookable() {
            LocalDate fixedToday = LocalDate.of(2026, 6, 15);
            LocalDateTime afterHours = fixedToday.atTime(20, 0);
            // CALLS_REAL_METHODS: stub only now(); a bare mockStatic(LocalDate.class) breaks LocalDate.of / equals.
            try (var mockDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
                 var mockDateTime = mockStatic(LocalDateTime.class, Answers.CALLS_REAL_METHODS)) {
                mockDate.when(LocalDate::now).thenReturn(fixedToday);
                mockDateTime.when(LocalDateTime::now).thenReturn(afterHours);
                assertThat(scheduleService.isDateBookable(fixedToday, 60)).isFalse();
            }
        }

        @Test
        @DisplayName("getNextAvailableSlot skips today when now is after the last grid slot and returns tomorrow morning")
        void nextSlot_afterBusinessHours_returnsFirstSlotNextDay() {
            LocalDate fixedToday = LocalDate.of(2026, 6, 15);
            LocalDateTime afterHours = fixedToday.atTime(20, 0);
            try (var mockDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
                 var mockDateTime = mockStatic(LocalDateTime.class, Answers.CALLS_REAL_METHODS)) {
                mockDate.when(LocalDate::now).thenReturn(fixedToday);
                mockDateTime.when(LocalDateTime::now).thenReturn(afterHours);
                assertThat(scheduleService.getNextAvailableSlot(60))
                        .hasValueSatisfying(slot ->
                                assertThat(slot.getStartTime().toLocalDate()).isEqualTo(fixedToday.plusDays(1)));
            }
        }

        @Test
        @DisplayName("Today with now inside the grid: at least one slot starts at or after now → bookable")
        void today_nowMidMorning_stillBookableWhenSlotsRemain() {
            LocalDate fixedToday = LocalDate.of(2026, 8, 20);
            LocalDateTime midMorning = fixedToday.atTime(10, 15);
            try (var mockDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
                 var mockDateTime = mockStatic(LocalDateTime.class, Answers.CALLS_REAL_METHODS)) {
                mockDate.when(LocalDate::now).thenReturn(fixedToday);
                mockDateTime.when(LocalDateTime::now).thenReturn(midMorning);
                assertThat(scheduleService.isDateBookable(fixedToday, 60)).isTrue();
            }
        }

        @Test
        @DisplayName("getNextAvailableSlot returns same-day slot when now aligns with an available grid start")
        void nextSlot_sameDay_whenNowEqualsSlotStart_returnsThatSlot() {
            LocalDate fixedToday = LocalDate.of(2026, 8, 21);
            LocalDateTime atNineThirty = fixedToday.atTime(9, 30);
            try (var mockDate = mockStatic(LocalDate.class, Answers.CALLS_REAL_METHODS);
                 var mockDateTime = mockStatic(LocalDateTime.class, Answers.CALLS_REAL_METHODS)) {
                mockDate.when(LocalDate::now).thenReturn(fixedToday);
                mockDateTime.when(LocalDateTime::now).thenReturn(atNineThirty);
                assertThat(scheduleService.getNextAvailableSlot(60))
                        .hasValueSatisfying(slot -> {
                            assertThat(slot.getStartTime().toLocalDate()).isEqualTo(fixedToday);
                            assertThat(slot.getStartTime().toLocalTime().getHour()).isEqualTo(9);
                            assertThat(slot.getStartTime().toLocalTime().getMinute()).isEqualTo(30);
                        });
            }
        }
    }

    @Nested
    @DisplayName("loadSchedule")
    class Load {

        @BeforeEach
        void refreshForLoad() {
            setupScheduleService();
        }

        @Test
        @DisplayName("Null entries from repository are ignored (defensive)")
        void ignoresNullAppointments() {
            List<com.appointmentscheduler.domain.Appointment> list = new ArrayList<>();
            list.add(null);
            when(repo.findAll()).thenReturn(list);
            ScheduleService svc = new ScheduleService(repo);
            assertThat(svc.getMasterSchedule().getAllAppointments()).isEmpty();
        }

        @Test
        @DisplayName("Deleted appointments are not loaded into the master schedule")
        void ignoresDeleted() {
            User p = new User("u1", "U", "u@t.com", "x");
            LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
            InPersonAppointment a = new InPersonAppointment("a1", p, new TimeSlot(start, start.plusHours(1)), "L");
            a.markDeleted("admin");
            when(repo.findAll()).thenReturn(List.of(a));
            ScheduleService svc = new ScheduleService(repo);
            assertThat(svc.getMasterSchedule().getAllAppointments()).isEmpty();
        }
    }
}
