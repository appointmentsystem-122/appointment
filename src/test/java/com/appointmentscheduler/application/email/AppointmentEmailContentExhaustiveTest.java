package com.appointmentscheduler.application.email;

import com.appointmentscheduler.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises all branches in {@link AppointmentEmailContent} (types, branding, reminder details).
 */
class AppointmentEmailContentExhaustiveTest {

    private static User user() {
        return new User("u1", "Pat", "p@test.com", "x");
    }

    private static TimeSlot slot() {
        LocalDateTime s = LocalDateTime.of(2026, 7, 1, 10, 0);
        return new TimeSlot(s, s.plusHours(1));
    }

    @Test
    void describeType_coversConcreteKinds() {
        User u = user();
        TimeSlot t = slot();
        assertThat(AppointmentEmailContent.describeAppointmentType(new VirtualAppointment(u, t, "https://meet"))).isEqualTo("Virtual / online");
        assertThat(AppointmentEmailContent.describeAppointmentType(new InPersonAppointment(u, t, "loc"))).isEqualTo("In person");
        assertThat(AppointmentEmailContent.describeAppointmentType(new UrgentAppointment(u, t))).isEqualTo("Urgent");
        assertThat(AppointmentEmailContent.describeAppointmentType(new GroupAppointment(u, t, 5))).isEqualTo("Group");
        assertThat(AppointmentEmailContent.describeAppointmentType(new FollowUpAppointment(u, t, "prior"))).isEqualTo("Follow-up");
        assertThat(AppointmentEmailContent.describeAppointmentType(new AssessmentAppointment(u, t))).isEqualTo("Assessment / consultation");
        RecurrencePattern rp = new RecurrencePattern(
                RecurrencePattern.Frequency.WEEKLY,
                t.getStartTime(),
                t.getStartTime().plusWeeks(8),
                1
        );
        assertThat(AppointmentEmailContent.describeAppointmentType(
                new RecurringAppointment(u, t, "ser", rp, "occ1"))).isEqualTo("Recurring");
        assertThat(AppointmentEmailContent.describeAppointmentType(new IndividualAppointment(u, t))).isEqualTo("Individual");
    }

    @Test
    void describeType_fallbackBySimpleName() {
        User u = user();
        TimeSlot t = slot();
        // Subclasses of Appointment that do not match earlier instanceof checks:
        assertThat(AppointmentEmailContent.describeAppointmentType(new OddAssessmentNamed(u, t))).isEqualTo("Consultation");
        assertThat(AppointmentEmailContent.describeAppointmentType(new OddFollowNamed(u, t))).isEqualTo("Follow-up");
        assertThat(AppointmentEmailContent.describeAppointmentType(new OddPlainNamed(u, t))).isEqualTo("Standard");
    }

    @Test
    void describeType_rejectsNull() {
        assertThatThrownBy(() -> AppointmentEmailContent.describeAppointmentType(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void formatDateAndTime_requireSlot() {
        assertThatThrownBy(() -> AppointmentEmailContent.formatAppointmentDate(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AppointmentEmailContent.formatAppointmentTimeRange(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildBodies_nullPatientUsesCustomer_and_brandFallbacks() {
        User u = user();
        TimeSlot t = slot();
        InPersonAppointment appt = new NullPatientAppointment(u, t, "L");

        String b1 = AppointmentEmailContent.buildBookingConfirmationBody(appt, null);
        String b2 = AppointmentEmailContent.buildBookingConfirmationBody(appt, "  ");
        String b3 = AppointmentEmailContent.buildBookingConfirmationBody(appt, " Brand Co ");

        assertThat(b1).contains("Dear Customer").contains("Appointment Booking System");
        assertThat(b2).contains("Dear Customer").contains("Appointment Booking System");
        assertThat(b3).contains("Dear Customer").contains("Brand Co");

        String m = AppointmentEmailContent.buildModifiedBody(appt, null);
        String c = AppointmentEmailContent.buildCancelledBody(appt, null);
        assertThat(m).contains("Dear Customer");
        assertThat(c).contains("Dear Customer");
    }

    @Test
    void buildReminder_detailsBranch() {
        User u = user();
        TimeSlot t = slot();
        InPersonAppointment appt = new InPersonAppointment(u, t, "L");

        String noNote = AppointmentEmailContent.buildReminderBody(appt, null, "Co");
        String blankNote = AppointmentEmailContent.buildReminderBody(appt, "   ", "Co");
        String note = AppointmentEmailContent.buildReminderBody(appt, "Bring ID", "Co");

        assertThat(noNote).doesNotContain("Note:");
        assertThat(blankNote).doesNotContain("Note:");
        assertThat(note).contains("Note:").contains("Bring ID");
    }

    /** Overrides getPatient() to exercise {@code patient == null} branches in builders. */
    private static final class NullPatientAppointment extends InPersonAppointment {
        NullPatientAppointment(User patient, TimeSlot timeSlot, String location) {
            super(patient, timeSlot, location);
        }

        @Override
        public User getPatient() {
            return null;
        }
    }

    private static final class OddAssessmentNamed extends Appointment {
        OddAssessmentNamed(User p, TimeSlot t) {
            super(p, t);
        }
    }

    private static final class OddFollowNamed extends Appointment {
        OddFollowNamed(User p, TimeSlot t) {
            super(p, t);
        }
    }

    private static final class OddPlainNamed extends Appointment {
        OddPlainNamed(User p, TimeSlot t) {
            super(p, t);
        }
    }
}
