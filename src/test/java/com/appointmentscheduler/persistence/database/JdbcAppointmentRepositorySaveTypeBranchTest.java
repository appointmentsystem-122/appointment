package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.*;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers {@link JdbcAppointmentRepository#save} for each concrete {@link Appointment} subtype on the H2
 * {@code MERGE INTO ... KEY(id)} path, exercising {@code appointmentType} and {@code setTypeSpecificParams}.
 */
class JdbcAppointmentRepositorySaveTypeBranchTest {

    private static final LocalDateTime S = LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final TimeSlot SLOT = new TimeSlot(S, S.plusHours(1));
    private static final User P = new User("pid-save", "Pat", "p@e.com", "x");

    @Test
    void save_inPerson_merges() throws Exception {
        saveAndVerify(new InPersonAppointment("a1", P, SLOT, "Room 1"));
    }

    @Test
    void save_virtual_merges() throws Exception {
        saveAndVerify(new VirtualAppointment("a2", P, SLOT, "https://zoom/x"));
    }

    @Test
    void save_followUp_merges() throws Exception {
        saveAndVerify(new FollowUpAppointment("a3", P, SLOT, "prior-99"));
    }

    @Test
    void save_recurring_merges() throws Exception {
        RecurrencePattern rp = new RecurrencePattern(
                RecurrencePattern.Frequency.MONTHLY,
                S.minusWeeks(1),
                S.plusYears(1),
                2);
        saveAndVerify(new RecurringAppointment("a4", P, SLOT, "series-1", rp, "occ-1"));
    }

    @Test
    void save_recurringMock_nullRecurrencePattern_skipsPatternBindBlock() throws Exception {
        RecurringAppointment r = mock(RecurringAppointment.class);
        when(r.getId()).thenReturn("a4b");
        when(r.getPatient()).thenReturn(P);
        when(r.getDoctorId()).thenReturn("d1");
        when(r.getRoomId()).thenReturn("r1");
        when(r.getClinicId()).thenReturn("c1");
        when(r.getTimeSlot()).thenReturn(SLOT);
        when(r.getStatus()).thenReturn("CONFIRMED");
        when(r.getParticipantCount()).thenReturn(1);
        when(r.isDeleted()).thenReturn(false);
        when(r.getDeletedAt()).thenReturn(null);
        when(r.getDeletedBy()).thenReturn(null);
        when(r.isUrgent()).thenReturn(false);
        when(r.getCustomerNotes()).thenReturn(null);
        when(r.getContactPhone()).thenReturn(null);
        when(r.getReminderChannel()).thenReturn(null);
        when(r.getAccessibilityNeeds()).thenReturn(null);
        when(r.getPreferredLanguage()).thenReturn(null);
        when(r.getSeriesId()).thenReturn("series-1");
        when(r.getOccurrenceId()).thenReturn("occ-1");
        when(r.getRecurrencePattern()).thenReturn(null);
        saveAndVerify(r);
    }

    @Test
    void save_group_merges() throws Exception {
        saveAndVerify(new GroupAppointment("a5", P, SLOT, 8));
    }

    @Test
    void save_assessment_merges() throws Exception {
        saveAndVerify(new AssessmentAppointment("a6", P, SLOT));
    }

    @Test
    void save_urgent_merges() throws Exception {
        saveAndVerify(new UrgentAppointment("a7", P, SLOT));
    }

    @Test
    void save_individual_merges() throws Exception {
        saveAndVerify(new IndividualAppointment("a8", P, SLOT));
    }

    private static void saveAndVerify(Appointment a) throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("MERGE INTO")))).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        new JdbcAppointmentRepository(ds, users).save(a);
        verify(ps).executeUpdate();
    }
}
