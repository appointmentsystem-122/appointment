package com.appointmentscheduler.domain.rules;

import com.appointmentscheduler.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookingStrategiesTest {

    @Test
    void testDurationRuleStrategy() {
        DurationRuleStrategy strategy = new DurationRuleStrategy(60); // 1 hour max

        User patient = new User("1", "P", "e", "p");
        LocalDateTime start = LocalDateTime.now();
        TimeSlot validSlot = new TimeSlot(start, start.plusMinutes(45));
        TimeSlot invalidSlot = new TimeSlot(start, start.plusMinutes(90));

        Appointment validAppt = new InPersonAppointment(patient, validSlot, "Room");
        Appointment invalidAppt = new InPersonAppointment(patient, invalidSlot, "Room");

        assertTrue(strategy.isValid(validAppt));
        assertFalse(strategy.isValid(invalidAppt));
    }

    @Test
    void testCapacityRuleStrategy() {
        CapacityRuleStrategy strategy = new CapacityRuleStrategy();

        User patient = new User("1", "P", "e", "p");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        GroupAppointment validGroupAppt = new GroupAppointment(patient, slot, 5);
        validGroupAppt.setParticipantCount(3);
        assertTrue(strategy.isValid(validGroupAppt));

        GroupAppointment invalidGroupAppt = new GroupAppointment(patient, slot, 5);
        invalidGroupAppt.setParticipantCount(6);
        assertFalse(strategy.isValid(invalidGroupAppt));

        Appointment individual = new IndividualAppointment(patient, slot);
        individual.setParticipantCount(1);
        assertTrue(strategy.isValid(individual));
    }

    @Test
    void testAppointmentTypeRuleStrategy() {
        AppointmentTypeRuleStrategy strategy = new AppointmentTypeRuleStrategy();
        User patient = new User("1", "P", "e", "p");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        VirtualAppointment validVirtual = new VirtualAppointment(patient, slot, "link");
        VirtualAppointment invalidVirtual = new VirtualAppointment(patient, slot, null);

        assertTrue(strategy.isValid(validVirtual));
        assertFalse(strategy.isValid(invalidVirtual));

        InPersonAppointment validInPerson = new InPersonAppointment(patient, slot, "Loc");
        InPersonAppointment invalidInPerson = new InPersonAppointment(patient, slot, "");

        assertTrue(strategy.isValid(validInPerson));
        assertFalse(strategy.isValid(invalidInPerson));
    }
}
