package com.generic.appointments.service;

import com.generic.appointments.model.TimeSlot;
import com.generic.appointments.repository.impl.InMemoryTimeSlotRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericScheduleServiceTest {

    @Test
    void createAndFindAvailable() {
        InMemoryTimeSlotRepository repo = new InMemoryTimeSlotRepository();
        ScheduleService svc = new ScheduleService(repo);
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 1, 18, 0);
        TimeSlot created = svc.createTimeSlot(from.plusHours(1), from.plusHours(2));
        assertThat(created.getId()).isNotNull();
        List<TimeSlot> avail = svc.findAvailableBetween(from, to);
        assertThat(avail).isNotEmpty();
    }
}
