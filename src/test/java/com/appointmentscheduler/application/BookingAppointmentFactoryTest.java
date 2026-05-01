package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingAppointmentFactoryTest {

    @AfterEach
    void restoreAppConfig() {
        AppConfig.reloadClasspathPropertiesForTest();
    }

    @Test
    void requiresArgs() {
        User p = new User("u", "N", "e@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("X", 30, 1);
        assertThatThrownBy(() -> BookingAppointmentFactory.create(null, p, slot)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BookingAppointmentFactory.create(BookingOption.of(t, false), null, slot)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BookingAppointmentFactory.create(BookingOption.of(t, false), p, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void groupWhenMaxParticipantsGreaterThanOne() {
        User p = new User("u", "N", "e@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Workshop", 60, 5);
        BookingOption opt = BookingOption.of(t, false);
        assertThat(BookingAppointmentFactory.create(opt, p, slot)).isInstanceOf(GroupAppointment.class);
    }

    @Test
    void onlineVirtual() {
        User p = new User("u", "N", "e@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Any", 30, 1);
        BookingOption opt = BookingOption.of(t, true);
        assertThat(BookingAppointmentFactory.create(opt, p, slot)).isInstanceOf(VirtualAppointment.class);
    }

    @Test
    void consultationAssessment() {
        User p = new User("u", "N", "e@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        String[] types = AppConfig.getBookingAppointmentTypes();
        int ci = AppConfig.getBookingConsultationTypeIndex();
        String name = types[ci];
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type(name, 45, 1);
        BookingOption opt = BookingOption.of(t, false);
        assertThat(BookingAppointmentFactory.create(opt, p, slot)).isInstanceOf(AssessmentAppointment.class);
    }

    @Test
    void followUpAndUrgentAndInPerson() {
        User p = new User("u", "N", "e@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type follow = new AppointmentTypeConfig.Type("Return visit", 20, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(follow, false), p, slot)).isInstanceOf(FollowUpAppointment.class);

        AppointmentTypeConfig.Type urgent = new AppointmentTypeConfig.Type("Urgent slot", 15, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(urgent, false), p, slot)).isInstanceOf(UrgentAppointment.class);

        AppointmentTypeConfig.Type plain = new AppointmentTypeConfig.Type("Standard visit", 40, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(plain, false), p, slot)).isInstanceOf(InPersonAppointment.class);

        AppointmentTypeConfig.Type express = new AppointmentTypeConfig.Type("Express lane", 15, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(express, false), p, slot)).isInstanceOf(UrgentAppointment.class);

        AppointmentTypeConfig.Type priority = new AppointmentTypeConfig.Type("priority case", 15, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(priority, false), p, slot)).isInstanceOf(UrgentAppointment.class);
    }

    @Test
    void blankServiceNameAfterTrim_fallsThroughToInPerson() {
        User p = new User("u-blank", "N", "e-blank@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("   ", 30, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), p, slot)).isInstanceOf(InPersonAppointment.class);
    }

    @Test
    void consultationIndexOutOfBounds_skipsAssessmentBranch() {
        Properties props = new Properties();
        props.setProperty("booking.consultationTypeIndex", "99");
        AppConfig.applyPropertiesForTest(props);
        User p = new User("u-ci", "N", "e-ci@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        String[] types = AppConfig.getBookingAppointmentTypes();
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type(types[0], 30, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), p, slot)).isInstanceOf(InPersonAppointment.class);
    }

    @Test
    void followUpIndexSetButNameDoesNotMatchTypeAtIndex_goesInPerson() {
        Properties props = new Properties();
        props.setProperty("booking.followUpTypeIndex", "1");
        AppConfig.applyPropertiesForTest(props);
        User p = new User("u-fidx", "N", "e-fidx@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Standard", 30, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), p, slot)).isInstanceOf(InPersonAppointment.class);
    }

    @Test
    void followUpWhenFollowUpIndexNegative_matchesKeywordInServiceName() {
        Properties p = new Properties();
        p.setProperty("booking.followUpTypeIndex", "-1");
        AppConfig.applyPropertiesForTest(p);
        User usr = new User("u-fu", "N", "e-fu@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Custom follow-up care", 25, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), usr, slot)).isInstanceOf(FollowUpAppointment.class);
    }

    @Test
    void consultationIndexOutOfRange_skipsAssessment_toInPersonOrOther() {
        Properties p = new Properties();
        p.setProperty("booking.appointmentTypes", "Alpha,Beta");
        p.setProperty("booking.consultationTypeIndex", "9");
        p.setProperty("booking.followUpTypeIndex", "9");
        AppConfig.applyPropertiesForTest(p);
        User usr = new User("u-ci", "N", "e-ci@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Gamma service", 30, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), usr, slot)).isInstanceOf(InPersonAppointment.class);
    }

    @Test
    void followUpIndexPositiveButNameMismatch_noKeyword_isInPerson() {
        Properties p = new Properties();
        p.setProperty("booking.appointmentTypes", "Standard,New session,Return visit");
        p.setProperty("booking.followUpTypeIndex", "2");
        p.setProperty("booking.consultationTypeIndex", "9");
        AppConfig.applyPropertiesForTest(p);
        User usr = new User("u-fm", "N", "e-fm@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Only a haircut", 25, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), usr, slot)).isInstanceOf(InPersonAppointment.class);
    }

    @Test
    void followUpIndexNegative_nameWithoutKeywords_isInPerson() {
        Properties p = new Properties();
        p.setProperty("booking.followUpTypeIndex", "-1");
        AppConfig.applyPropertiesForTest(p);
        User usr = new User("u-nk", "N", "e-nk@x.com", "x");
        TimeSlot slot = new TimeSlot(LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(4).plusHours(1));
        AppointmentTypeConfig.Type t = new AppointmentTypeConfig.Type("Regular checkup only", 20, 1);
        assertThat(BookingAppointmentFactory.create(BookingOption.of(t, false), usr, slot)).isInstanceOf(InPersonAppointment.class);
    }
}
