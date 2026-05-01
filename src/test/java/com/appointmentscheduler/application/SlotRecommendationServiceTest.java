package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.Schedule;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.InMemoryAppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ResourceLock("AppConfigProps")
class SlotRecommendationServiceTest {

    @BeforeEach
    void pinBusinessHours() {
        Properties p = new Properties();
        p.setProperty("business.hourStart", "8");
        p.setProperty("business.hourEnd", "18");
        AppConfig.applyPropertiesForTest(p);
    }

    @AfterEach
    void restoreAppConfig() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void recommendsSubsetWhenManySlots() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        ScheduleService schedule = new ScheduleService(repo);
        SlotRecommendationService svc = new SlotRecommendationService(schedule);
        LocalDate future = LocalDate.now().plusDays(10);
        List<TimeSlot> rec = svc.getRecommendedSlots(future, 5);
        assertThat(rec.size()).isLessThanOrEqualTo(5);
    }

    @Test
    void emptyDay_returnsEmpty() {
        InMemoryAppointmentRepository repo = new InMemoryAppointmentRepository();
        ScheduleService schedule = new ScheduleService(repo);
        ClosedDayService closed = new ClosedDayService();
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(ClosedDayService.class);
        p.remove("admin.closedDays");
        schedule.setClosedDayService(closed);
        LocalDate d = LocalDate.now().plusDays(20);
        closed.addClosedDay(d);
        SlotRecommendationService svc = new SlotRecommendationService(schedule);
        assertThat(svc.getRecommendedSlots(d, 5)).isEmpty();
        p.remove("admin.closedDays");
    }

    @Test
    void recommendsAllWhenAvailableLessOrEqualMax() {
        ScheduleService schedule = mock(ScheduleService.class);
        LocalDate d = LocalDate.now().plusDays(3);
        List<TimeSlot> slots = List.of(
                new TimeSlot(d.atTime(9, 0), d.atTime(10, 0)),
                new TimeSlot(d.atTime(10, 0), d.atTime(11, 0))
        );
        when(schedule.getAvailableSlots(d)).thenReturn(slots);
        SlotRecommendationService svc = new SlotRecommendationService(schedule);
        assertThat(svc.getRecommendedSlots(d, 5)).hasSize(2);
    }

    @Test
    void countNearbyAppointments_ignoresCancelledBranch() throws Exception {
        ScheduleService scheduleService = mock(ScheduleService.class);
        Schedule schedule = mock(Schedule.class);
        when(scheduleService.getMasterSchedule()).thenReturn(schedule);
        SlotRecommendationService svc = new SlotRecommendationService(scheduleService);

        LocalDateTime s = LocalDate.now().plusDays(2).atTime(9, 0);
        TimeSlot target = new TimeSlot(s, s.plusHours(1));
        User u = new User("u", "N", "e@x.com", "x");
        InPersonAppointment cancelled = new InPersonAppointment(u, new TimeSlot(s.minusMinutes(30), s.plusMinutes(30)), "L");
        cancelled.setStatus("CANCELLED");
        InPersonAppointment active = new InPersonAppointment(u, new TimeSlot(s.plusMinutes(15), s.plusMinutes(45)), "L");
        active.setStatus("CONFIRMED");
        when(schedule.getOverlappingAppointments(org.mockito.ArgumentMatchers.any(TimeSlot.class)))
                .thenReturn(List.of(cancelled, active));

        Method m = SlotRecommendationService.class.getDeclaredMethod("countNearbyAppointments", TimeSlot.class, Schedule.class);
        m.setAccessible(true);
        long n = (long) m.invoke(svc, target, schedule);
        assertThat(n).isEqualTo(1);
    }
}
