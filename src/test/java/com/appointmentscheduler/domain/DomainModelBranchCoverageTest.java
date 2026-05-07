package com.appointmentscheduler.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises constructor validation, equals/hashCode, and edge branches on core domain types
 * to raise JaCoCo branch coverage without UI.
 */
class DomainModelBranchCoverageTest {

    // --- AppointmentType ---

    @Test
    void appointmentType_idFromNameWhenIdBlank() {
        AppointmentType t = new AppointmentType("", "My Type Name", 60, 5);
        assertThat(t.getId()).isEqualTo("My_Type_Name");
        assertThat(t.getName()).isEqualTo("My Type Name");
    }

    @Test
    void appointmentType_idDefaultWhenIdAndNameMissing() {
        AppointmentType t = new AppointmentType(null, null, 60, 5);
        assertThat(t.getId()).isEqualTo("type");
    }

    @ParameterizedTest
    @CsvSource({
            "10, 15",   // clamp min duration
            "500, 480", // clamp max duration
            "60, 60"
    })
    void appointmentType_durationClamped(int input, int expected) {
        assertThat(new AppointmentType("x", "n", input, 5).getDurationMinutes()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1",
            "200, 100",
            "5, 5"
    })
    void appointmentType_maxParticipantsClamped(int input, int expected) {
        assertThat(new AppointmentType("x", "n", 30, input).getMaxParticipants()).isEqualTo(expected);
    }

    @Test
    void appointmentType_equalsAndHashCode_byId() {
        AppointmentType a = new AppointmentType("id1", "A", 30, 2);
        AppointmentType b = new AppointmentType("id1", "B", 45, 3);
        AppointmentType c = new AppointmentType("id2", "A", 30, 2);
        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo(null).isNotEqualTo("x");
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void appointmentType_twoArgConstructor_delegates() {
        AppointmentType t = new AppointmentType("LabelOnly", 45, 3);
        assertThat(t.getName()).isEqualTo("LabelOnly");
    }

    // --- Room ---

    @ParameterizedTest
    @ValueSource(strings = {""})
    void room_rejectsEmptyId(String badId) {
        assertThatThrownBy(() -> new Room(badId, "n")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void room_rejectsNullId() {
        assertThatThrownBy(() -> new Room(null, "n")).isInstanceOf(IllegalArgumentException.class);
    }

    /** Production code only checks isEmpty(), not trim — whitespace-only id is currently allowed. */
    @Test
    void room_whitespaceOnlyId_isAccepted() {
        Room r = new Room("  ", "n");
        assertThat(r.getId()).isEqualTo("  ");
    }

    @Test
    void room_nullNameBecomesEmpty() {
        assertThat(new Room("r1", null).getName()).isEmpty();
    }

    @Test
    void room_equalsById() {
        Room a = new Room("r1", "A", "c1");
        Room b = new Room("r1", "B", null);
        assertThat(a).isEqualTo(b).isNotEqualTo(new Room("r2", "A"));
    }

    // --- Doctor ---

    @Test
    void doctor_rejectsBlankId() {
        assertThatThrownBy(() -> new Doctor("", "n", "e", "s", 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doctor_optionalFieldsDefaultAndMaxPerDay() {
        Doctor d = new Doctor("d1", null, null, null, 0);
        assertThat(d.getName()).isEmpty();
        assertThat(d.getEmail()).isEmpty();
        assertThat(d.getSpecialty()).isEmpty();
        assertThat(d.getMaxAppointmentsPerDay()).isEqualTo(1);
    }

    @Test
    void doctor_equalsById() {
        Doctor a = new Doctor("d1", "A", "a@x.com", "s", 3, "c");
        Doctor b = new Doctor("d1", "B", "b@x.com", "t", 5, null);
        assertThat(a).isEqualTo(b);
    }

    // --- Clinic ---

    @Test
    void clinic_rejectsBlankId() {
        assertThatThrownBy(() -> new Clinic("", "n", "a", "UTC")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clinic_defaultsTimeZoneAndFields() {
        Clinic c = new Clinic("c1", null, null, null);
        assertThat(c.getName()).isEmpty();
        assertThat(c.getAddress()).isEmpty();
        assertThat(c.getTimeZone()).isEqualTo("UTC");
    }

    @Test
    void clinic_equalsById() {
        Clinic a = new Clinic("c1", "N", "addr", "Asia/Riyadh");
        Clinic b = new Clinic("c1", "X", "y", "UTC");
        assertThat(a).isEqualTo(b);
    }

    // --- User ---

    @Test
    void user_rejectsEmptyId() {
        assertThatThrownBy(() -> new User("", "N", "e@x.com", "p")).hasMessageContaining("ID");
    }

    @Test
    void user_rejectsEmptyName() {
        assertThatThrownBy(() -> new User("id", "", "e@x.com", "p")).hasMessageContaining("Name");
    }

    @Test
    void user_rejectsEmptyEmail() {
        assertThatThrownBy(() -> new User("id", "N", "", "p")).hasMessageContaining("Email");
    }

    @Test
    void user_rejectsEmptyPassword() {
        assertThatThrownBy(() -> new User("id", "N", "e@x.com", "")).hasMessageContaining("Password");
    }

    @Test
    void user_equalsById() {
        User a = new User("u1", "A", "a@x.com", "p");
        User b = new User("u1", "B", "b@x.com", "q");
        assertThat(a).isEqualTo(b);
    }

    // --- Appointment / VirtualAppointment ---

    @Test
    void appointment_reconstitutionConstructor_rejectsBadArgs() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        assertThatThrownBy(() -> new VirtualAppointment("", p, slot, "link")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirtualAppointment(null, p, slot, "link")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appointment_constructorsRejectNullPatientAndSlot() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> new VirtualAppointment(null, slot, "link"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient");
        assertThatThrownBy(() -> new VirtualAppointment(p, null, "link"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TimeSlot");
        assertThatThrownBy(() -> new VirtualAppointment("id", null, slot, "link"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient");
        assertThatThrownBy(() -> new VirtualAppointment("id", p, null, "link"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TimeSlot");
    }

    @Test
    void concreteAppointmentFieldsRoundTrip() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));

        GroupAppointment group = new GroupAppointment("group-id", p, slot, 4);
        group.setMaxCapacity(8);
        assertThat(group.getMaxCapacity()).isEqualTo(8);

        VirtualAppointment virtual = new VirtualAppointment("virtual-id", p, slot, "https://old.example.test");
        virtual.setMeetingLink("https://new.example.test");
        assertThat(virtual.getMeetingLink()).isEqualTo("https://new.example.test");

        InPersonAppointment inPerson = new InPersonAppointment("in-person-id", p, slot, null);
        assertThat(inPerson.getLocation()).isEmpty();
        inPerson.setLocation("Room 12");
        assertThat(inPerson.getLocation()).isEqualTo("Room 12");
    }

    @Test
    void appointment_setParticipantCount_rejectsLow() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        VirtualAppointment v = new VirtualAppointment(p, slot, "http://x");
        assertThatThrownBy(() -> v.setParticipantCount(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appointment_equals_sameIdDifferentSubtypeStillByClass() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        VirtualAppointment v = new VirtualAppointment("shared-id", p, slot, "http://x");
        InPersonAppointment i = new InPersonAppointment("shared-id", p, slot, "loc");
        assertThat(v).isNotEqualTo(i);
    }

    @Test
    void virtualAppointment_sameId_equal() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        VirtualAppointment a = new VirtualAppointment("vid", p, slot, "http://a");
        VirtualAppointment b = new VirtualAppointment("vid", p, slot, "http://b");
        assertThat(a).isEqualTo(b);
    }

    // --- RecurringAppointment ---

    @Test
    void recurringAppointment_rejectsNullSeriesId() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrencePattern.Frequency.WEEKLY,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1
        );
        assertThatThrownBy(() -> new RecurringAppointment(p, slot, null, pattern, "o1"))
                .hasMessageContaining("Series");
    }

    @Test
    void recurringAppointment_rejectsNullPattern() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        assertThatThrownBy(() -> new RecurringAppointment(p, slot, "s1", null, "o1"))
                .hasMessageContaining("Recurrence");
    }

    @Test
    void recurringAppointment_rejectsNullOccurrenceId() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrencePattern.Frequency.WEEKLY,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1
        );
        assertThatThrownBy(() -> new RecurringAppointment(p, slot, "s1", pattern, null))
                .hasMessageContaining("Occurrence");
    }

    @Test
    void recurringAppointment_withIdConstructor_validates() {
        User p = new User("u", "N", "e@x.com", "pw");
        TimeSlot slot = new TimeSlot(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrencePattern.Frequency.WEEKLY,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                1
        );
        assertThatThrownBy(() -> new RecurringAppointment("id", p, slot, "", pattern, "o"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recurringAppointment_equalsUsesId() {
        User p = new User("u", "N", "e@x.com", "pw");
        LocalDateTime t0 = LocalDateTime.now().plusDays(1).withNano(0);
        TimeSlot slot = new TimeSlot(t0, t0.plusHours(1));
        RecurrencePattern pattern = new RecurrencePattern(
                RecurrencePattern.Frequency.WEEKLY,
                t0,
                t0.plusDays(60),
                1
        );
        RecurringAppointment a = new RecurringAppointment("rid", p, slot, "s1", pattern, "o1");
        RecurringAppointment b = new RecurringAppointment("rid", p, slot, "s1", pattern, "o1");
        assertThat(a).isEqualTo(b);
    }
}
