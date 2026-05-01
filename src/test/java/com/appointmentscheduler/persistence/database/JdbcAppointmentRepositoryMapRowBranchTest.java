package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link JdbcAppointmentRepository#mapRow} switch branches and JDBC edge paths
 * (null ids, optional columns, recurrence fallback, group capacity) via mocked {@link ResultSet}s.
 */
class JdbcAppointmentRepositoryMapRowBranchTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 6, 1, 11, 0);
    private static final User PATIENT = new User("pid-mock", "N", "e@e.com", "h");

    @Test
    void findById_null_returnsEmpty() {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        assertThat(new JdbcAppointmentRepository(ds, users).findById(null)).isEmpty();
    }

    @Test
    void findBlockingBookingsForPatient_null_returnsEmpty() {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        assertThat(new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient(null)).isEmpty();
    }

    @Test
    void deleteById_null_noOp() throws SQLException {
        DataSource ds = mock(DataSource.class);
        UserRepository users = mock(UserRepository.class);
        new JdbcAppointmentRepository(ds, users).deleteById(null);
        verify(ds, never()).getConnection();
    }

    @Test
    void findById_mapsInPerson() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "IN_PERSON");
        when(rs.getString("location")).thenReturn("Hall A");
        assertThat(loadOne(rs)).isInstanceOf(InPersonAppointment.class);
    }

    @Test
    void findById_mapsVirtual() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "VIRTUAL");
        when(rs.getString("meeting_link")).thenReturn("https://meet/x");
        assertThat(loadOne(rs)).isInstanceOf(VirtualAppointment.class);
    }

    @Test
    void findById_mapsFollowUp() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "FOLLOW_UP");
        when(rs.getString("prior_appointment_id")).thenReturn("prior-1");
        assertThat(loadOne(rs)).isInstanceOf(FollowUpAppointment.class);
    }

    @Test
    void findById_mapsRecurring_withFullRecurrencePattern() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "RECURRING");
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");
        when(rs.getString("rec_frequency")).thenReturn("WEEKLY");
        when(rs.getTimestamp("rec_series_start")).thenReturn(Timestamp.valueOf(START.minusDays(1)));
        when(rs.getTimestamp("rec_series_end")).thenReturn(Timestamp.valueOf(END.plusYears(1)));
        when(rs.getInt("rec_interval")).thenReturn(2);
        assertThat(loadOne(rs)).isInstanceOf(RecurringAppointment.class);
    }

    @Test
    void findById_mapsRecurring_fallbackPattern_whenIncomplete() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "RECURRING");
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");
        when(rs.getString("rec_frequency")).thenReturn(null);
        when(rs.getTimestamp("rec_series_start")).thenReturn(null);
        when(rs.getTimestamp("rec_series_end")).thenReturn(null);
        when(rs.getInt("rec_interval")).thenReturn(0);
        assertThat(loadOne(rs)).isInstanceOf(RecurringAppointment.class);
    }

    @Test
    void findById_mapsRecurring_fallback_whenSeriesStartMissing() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "RECURRING");
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");
        when(rs.getString("rec_frequency")).thenReturn("WEEKLY");
        when(rs.getTimestamp("rec_series_start")).thenReturn(null);
        when(rs.getTimestamp("rec_series_end")).thenReturn(Timestamp.valueOf(END.plusYears(1)));
        when(rs.getInt("rec_interval")).thenReturn(2);
        assertThat(loadOne(rs)).isInstanceOf(RecurringAppointment.class);
    }

    @Test
    void findById_mapsRecurring_fallback_whenSeriesEndMissing() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "RECURRING");
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");
        when(rs.getString("rec_frequency")).thenReturn("WEEKLY");
        when(rs.getTimestamp("rec_series_start")).thenReturn(Timestamp.valueOf(START.minusDays(1)));
        when(rs.getTimestamp("rec_series_end")).thenReturn(null);
        when(rs.getInt("rec_interval")).thenReturn(2);
        assertThat(loadOne(rs)).isInstanceOf(RecurringAppointment.class);
    }

    @Test
    void findById_mapsRecurring_negativeInterval_usesFallbackPattern() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "RECURRING");
        when(rs.getString("series_id")).thenReturn("s1");
        when(rs.getString("occurrence_id")).thenReturn("o1");
        when(rs.getString("rec_frequency")).thenReturn("WEEKLY");
        when(rs.getTimestamp("rec_series_start")).thenReturn(Timestamp.valueOf(START.minusDays(1)));
        when(rs.getTimestamp("rec_series_end")).thenReturn(Timestamp.valueOf(END.plusYears(1)));
        when(rs.getInt("rec_interval")).thenReturn(-1);
        assertThat(loadOne(rs)).isInstanceOf(RecurringAppointment.class);
    }

    @Test
    void findById_mapsGroup_maxCapacityPositive() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "GROUP");
        when(rs.getInt("max_capacity")).thenReturn(12);
        Appointment a = loadOne(rs);
        assertThat(a).isInstanceOf(GroupAppointment.class);
        assertThat(((GroupAppointment) a).getMaxCapacity()).isEqualTo(12);
    }

    @Test
    void findById_mapsGroup_maxCapacityZero_defaultsToTen() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "GROUP");
        when(rs.getInt("max_capacity")).thenReturn(0);
        Appointment a = loadOne(rs);
        assertThat(a).isInstanceOf(GroupAppointment.class);
        assertThat(((GroupAppointment) a).getMaxCapacity()).isEqualTo(10);
    }

    @Test
    void findById_mapsAssessment() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "ASSESSMENT");
        assertThat(loadOne(rs)).isInstanceOf(AssessmentAppointment.class);
    }

    @Test
    void findById_mapsUrgent() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "URGENT");
        assertThat(loadOne(rs)).isInstanceOf(UrgentAppointment.class);
    }

    @Test
    void findById_nullAppointmentType_defaultsToIndividual() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, null);
        assertThat(loadOne(rs)).isInstanceOf(IndividualAppointment.class);
    }

    @Test
    void findById_unknownType_mapsIndividual() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "UNKNOWN_XYZ");
        assertThat(loadOne(rs)).isInstanceOf(IndividualAppointment.class);
    }

    @Test
    void findById_participantCountZero_becomesOne() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "IN_PERSON");
        when(rs.getString("location")).thenReturn("L");
        when(rs.getInt("participant_count")).thenReturn(0);
        assertThat(loadOne(rs).getParticipantCount()).isEqualTo(1);
    }

    @Test
    void findById_mapRowThrowsSqlException_wrapsRuntimeException() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("id")).thenThrow(new SQLException("read id"));

        assertThatThrownBy(() -> new JdbcAppointmentRepository(ds, users).findById("x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("map appointment row");
    }

    @Test
    void findById_optionalStringColumnThrowsSqlException_skipsSetter() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "IN_PERSON");
        when(rs.getString("location")).thenReturn("L");
        when(rs.getString("customer_notes")).thenThrow(new SQLException("bad column"));
        Appointment a = loadOne(rs);
        assertThat(a.getCustomerNotes()).isNull();
    }

    @Test
    void findById_optionalStringsSet_whenPresent() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        stubCommonRow(rs, "IN_PERSON");
        when(rs.getString("location")).thenReturn("L");
        when(rs.getString("customer_notes")).thenReturn("note");
        when(rs.getString("contact_phone")).thenReturn("+1");
        when(rs.getString("reminder_channel")).thenReturn("SMS");
        when(rs.getString("accessibility_needs")).thenReturn("wheelchair");
        when(rs.getString("preferred_language")).thenReturn("ar");
        when(rs.wasNull()).thenReturn(false);
        Appointment a = loadOne(rs);
        assertThat(a.getCustomerNotes()).isEqualTo("note");
        assertThat(a.getContactPhone()).isEqualTo("+1");
        assertThat(a.getReminderChannel()).isEqualTo("SMS");
        assertThat(a.getAccessibilityNeeds()).isEqualTo("wheelchair");
        assertThat(a.getPreferredLanguage()).isEqualTo("ar");
    }

    @Test
    void findBlockingBookingsForPatient_mapsRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("patient_id = ?") && sql.contains("PENDING"))))
                .thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        stubCommonRow(rs, "VIRTUAL");
        when(rs.getString("meeting_link")).thenReturn("https://x");
        when(users.findById("pid-mock")).thenReturn(Optional.of(PATIENT));

        List<Appointment> list = new JdbcAppointmentRepository(ds, users).findBlockingBookingsForPatient("pid-mock");
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isInstanceOf(VirtualAppointment.class);
    }

    @Test
    void findAll_mapsRows() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("ORDER BY start_time")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        stubCommonRow(rs, "IN_PERSON");
        when(rs.getString("location")).thenReturn("Loc");
        when(users.findById("pid-mock")).thenReturn(Optional.of(PATIENT));

        List<Appointment> list = new JdbcAppointmentRepository(ds, users).findAll();
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isInstanceOf(InPersonAppointment.class);
    }

    private static Appointment loadOne(ResultSet rs) throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection c = h2Connection();
        PreparedStatement ps = mock(PreparedStatement.class);
        UserRepository users = mock(UserRepository.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.prepareStatement(argThat(sql -> sql != null && sql.contains("WHERE id =")))).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(users.findById("pid-mock")).thenReturn(Optional.of(PATIENT));
        return new JdbcAppointmentRepository(ds, users).findById("aid-1").orElseThrow();
    }

    private static Connection h2Connection() throws SQLException {
        Connection c = mock(Connection.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(c.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        return c;
    }

    /**
     * Stubs columns read by {@link JdbcAppointmentRepository} mapRow for {@code appointment_type} = {@code type}
     * (pass {@code null} to leave {@code appointment_type} unset — override in test).
     */
    private static void stubCommonRow(ResultSet rs, String type) throws SQLException {
        when(rs.getString("id")).thenReturn("aid-1");
        when(rs.getString("patient_id")).thenReturn("pid-mock");
        when(rs.getObject("start_time", LocalDateTime.class)).thenReturn(START);
        when(rs.getObject("end_time", LocalDateTime.class)).thenReturn(END);
        if (type != null) {
            when(rs.getString("appointment_type")).thenReturn(type);
        }
        when(rs.getString("status")).thenReturn("CONFIRMED");
        when(rs.getInt("participant_count")).thenReturn(2);
        when(rs.getBoolean("deleted")).thenReturn(false);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null);
        when(rs.getString("deleted_by")).thenReturn(null);
        when(rs.getString("doctor_id")).thenReturn("d1");
        when(rs.getString("room_id")).thenReturn("r1");
        when(rs.getString("clinic_id")).thenReturn("c1");
        when(rs.getBoolean("urgent")).thenReturn(false);
        when(rs.getString("customer_notes")).thenReturn(null);
        when(rs.getString("contact_phone")).thenReturn(null);
        when(rs.getString("reminder_channel")).thenReturn(null);
        when(rs.getString("accessibility_needs")).thenReturn(null);
        when(rs.getString("preferred_language")).thenReturn(null);
        when(rs.wasNull()).thenReturn(true);
    }
}
