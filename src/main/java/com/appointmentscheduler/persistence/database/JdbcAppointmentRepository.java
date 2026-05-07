package com.appointmentscheduler.persistence.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.appointmentscheduler.domain.Appointment;
import com.appointmentscheduler.domain.AssessmentAppointment;
import com.appointmentscheduler.domain.FollowUpAppointment;
import com.appointmentscheduler.domain.GroupAppointment;
import com.appointmentscheduler.domain.InPersonAppointment;
import com.appointmentscheduler.domain.IndividualAppointment;
import com.appointmentscheduler.domain.RecurrencePattern;
import com.appointmentscheduler.domain.RecurringAppointment;
import com.appointmentscheduler.domain.TimeSlot;
import com.appointmentscheduler.domain.UrgentAppointment;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.domain.VirtualAppointment;
import com.appointmentscheduler.persistence.AppointmentRepository;
import com.appointmentscheduler.persistence.UserRepository;

/**
 * JDBC implementation of AppointmentRepository. Single-table inheritance;
 * requires UserRepository to resolve patient when loading.
 */
public class JdbcAppointmentRepository implements AppointmentRepository {

    private static final String TABLE = "appointment";

    /** PostgreSQL upsert; H2-style MERGE ... KEY(id) is not valid on PostgreSQL. */
    private static String postgresUpsertSql(String tbl) {
        return "INSERT INTO " + tbl + " (id, patient_id, doctor_id, room_id, clinic_id, start_time, end_time, "
                + "status, participant_count, deleted, deleted_at, deleted_by, urgent, appointment_type, "
                + "location, meeting_link, prior_appointment_id, series_id, occurrence_id, rec_frequency, rec_series_start, rec_series_end, rec_interval, max_capacity, "
                + "customer_notes, contact_phone, reminder_channel, accessibility_needs, preferred_language, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (id) DO UPDATE SET "
                + "patient_id = EXCLUDED.patient_id, doctor_id = EXCLUDED.doctor_id, room_id = EXCLUDED.room_id, clinic_id = EXCLUDED.clinic_id, "
                + "start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time, status = EXCLUDED.status, participant_count = EXCLUDED.participant_count, "
                + "deleted = EXCLUDED.deleted, deleted_at = EXCLUDED.deleted_at, deleted_by = EXCLUDED.deleted_by, urgent = EXCLUDED.urgent, appointment_type = EXCLUDED.appointment_type, "
                + "location = EXCLUDED.location, meeting_link = EXCLUDED.meeting_link, prior_appointment_id = EXCLUDED.prior_appointment_id, series_id = EXCLUDED.series_id, occurrence_id = EXCLUDED.occurrence_id, "
                + "rec_frequency = EXCLUDED.rec_frequency, rec_series_start = EXCLUDED.rec_series_start, rec_series_end = EXCLUDED.rec_series_end, rec_interval = EXCLUDED.rec_interval, max_capacity = EXCLUDED.max_capacity, "
                + "customer_notes = EXCLUDED.customer_notes, contact_phone = EXCLUDED.contact_phone, reminder_channel = EXCLUDED.reminder_channel, "
                + "accessibility_needs = EXCLUDED.accessibility_needs, preferred_language = EXCLUDED.preferred_language, updated_at = EXCLUDED.updated_at";
    }
    private final DataSource dataSource;
    private final UserRepository userRepository;

    public JdbcAppointmentRepository(DataSource dataSource, UserRepository userRepository) {
        this.dataSource = dataSource;
        this.userRepository = userRepository;
    }

    private static String appointmentTable(Connection c) throws SQLException {
        String tableName = JdbcPostgresHelper.table(c, TABLE);
        if (!isSafeSqlIdentifierPath(tableName)) {
            throw new SQLException("Unsafe appointment table name: " + tableName);
        }
        return tableName;
    }

    private static boolean isSafeSqlIdentifierPath(String value) {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    }

    @Override
    public void save(Appointment appointment) {
        if (appointment == null) return;
        try (Connection c = dataSource.getConnection()) {
            String tbl = appointmentTable(c);
            String sql;
            if (JdbcPostgresHelper.isMySql(c)) {
                sql = "INSERT INTO " + tbl + " (id, patient_id, doctor_id, room_id, clinic_id, start_time, end_time, "
                        + "status, participant_count, deleted, deleted_at, deleted_by, urgent, appointment_type, "
                        + "location, meeting_link, prior_appointment_id, series_id, occurrence_id, rec_frequency, rec_series_start, rec_series_end, rec_interval, max_capacity, "
                        + "customer_notes, contact_phone, reminder_channel, accessibility_needs, preferred_language, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) "
                        + "ON DUPLICATE KEY UPDATE patient_id=VALUES(patient_id), doctor_id=VALUES(doctor_id), room_id=VALUES(room_id), clinic_id=VALUES(clinic_id), "
                        + "start_time=VALUES(start_time), end_time=VALUES(end_time), status=VALUES(status), participant_count=VALUES(participant_count), "
                        + "deleted=VALUES(deleted), deleted_at=VALUES(deleted_at), deleted_by=VALUES(deleted_by), urgent=VALUES(urgent), appointment_type=VALUES(appointment_type), "
                        + "location=VALUES(location), meeting_link=VALUES(meeting_link), prior_appointment_id=VALUES(prior_appointment_id), series_id=VALUES(series_id), occurrence_id=VALUES(occurrence_id), "
                        + "rec_frequency=VALUES(rec_frequency), rec_series_start=VALUES(rec_series_start), rec_series_end=VALUES(rec_series_end), rec_interval=VALUES(rec_interval), max_capacity=VALUES(max_capacity), "
                        + "customer_notes=VALUES(customer_notes), contact_phone=VALUES(contact_phone), reminder_channel=VALUES(reminder_channel), accessibility_needs=VALUES(accessibility_needs), preferred_language=VALUES(preferred_language), updated_at=CURRENT_TIMESTAMP";
            } else if (JdbcPostgresHelper.isPostgres(c)) {
                sql = postgresUpsertSql(tbl);
            } else {
                sql = "MERGE INTO " + tbl + " (id, patient_id, doctor_id, room_id, clinic_id, start_time, end_time, "
                        + "status, participant_count, deleted, deleted_at, deleted_by, urgent, appointment_type, "
                        + "location, meeting_link, prior_appointment_id, series_id, occurrence_id, rec_frequency, rec_series_start, rec_series_end, rec_interval, max_capacity, "
                        + "customer_notes, contact_phone, reminder_channel, accessibility_needs, preferred_language, updated_at) "
                        + "KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, appointment.getId());
                ps.setString(i++, appointment.getPatient().getId());
                ps.setString(i++, appointment.getDoctorId());
                ps.setString(i++, appointment.getRoomId());
                ps.setString(i++, appointment.getClinicId());
                ps.setObject(i++, appointment.getTimeSlot().getStartTime());
                ps.setObject(i++, appointment.getTimeSlot().getEndTime());
                ps.setString(i++, appointment.getStatus());
                ps.setInt(i++, appointment.getParticipantCount());
                ps.setBoolean(i++, appointment.isDeleted());
                ps.setObject(i++, appointment.getDeletedAt());
                ps.setString(i++, appointment.getDeletedBy());
                ps.setBoolean(i++, appointment.isUrgent());
                String type = appointmentType(appointment);
                ps.setString(i++, type);
                setTypeSpecificParams(ps, i, appointment, type);
                int next = bindRequestFieldParams(ps, 25, appointment);
                if (!JdbcPostgresHelper.isMySql(c)) {
                    ps.setTimestamp(next, new Timestamp(System.currentTimeMillis()));
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            SQLException next = e.getNextException();
            if (next != null && next.getMessage() != null) {
                msg = msg + " — " + next.getMessage();
            }
            throw new RuntimeException("Failed to save appointment: " + appointment.getId() + ": " + msg, e);
        }
    }

    @Override
    public Optional<Appointment> findById(String id) {
        if (id == null) return Optional.empty();
        try (Connection c = dataSource.getConnection()) {
            String tbl = appointmentTable(c);
            String sql = "SELECT * FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find appointment: " + id, e);
        }
    }

    @Override
    public List<Appointment> findAll() {
        try (Connection c = dataSource.getConnection()) {
            String tbl = appointmentTable(c);
            String sql = "SELECT * FROM " + tbl + " ORDER BY start_time";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                List<Appointment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list appointments", e);
        }
    }

    @Override
    public List<Appointment> findBlockingBookingsForPatient(String patientId) {
        if (patientId == null) return List.of();
        try (Connection c = dataSource.getConnection()) {
            String tbl = appointmentTable(c);
            String sql = "SELECT * FROM " + tbl + " WHERE patient_id = ? AND deleted = ? AND status IN ('PENDING','CONFIRMED') ORDER BY start_time";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, patientId);
                ps.setBoolean(2, false);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Appointment> list = new ArrayList<>();
                    while (rs.next()) list.add(mapRow(rs));
                    return list;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list blocking appointments for patient: " + patientId, e);
        }
    }

    @Override
    public void deleteById(String id) {
        if (id == null) return;
        try (Connection c = dataSource.getConnection()) {
            String tbl = appointmentTable(c);
            String sql = "DELETE FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete appointment: " + id, e);
        }
    }

    private static String appointmentType(Appointment a) {
        if (a instanceof InPersonAppointment) return "IN_PERSON";
        if (a instanceof VirtualAppointment) return "VIRTUAL";
        if (a instanceof FollowUpAppointment) return "FOLLOW_UP";
        if (a instanceof RecurringAppointment) return "RECURRING";
        if (a instanceof GroupAppointment) return "GROUP";
        if (a instanceof IndividualAppointment) return "INDIVIDUAL";
        if (a instanceof AssessmentAppointment) return "ASSESSMENT";
        if (a instanceof UrgentAppointment) return "URGENT";
        return "INDIVIDUAL";
    }

    private static int bindRequestFieldParams(PreparedStatement ps, int startIndex, Appointment a) throws SQLException {
        int i = startIndex;
        ps.setString(i++, a.getCustomerNotes());
        ps.setString(i++, a.getContactPhone());
        ps.setString(i++, a.getReminderChannel());
        ps.setString(i++, a.getAccessibilityNeeds());
        ps.setString(i++, a.getPreferredLanguage());
        return i;
    }

    private static void setTypeSpecificParams(PreparedStatement ps, int startIndex, Appointment a, String type) {
        try {
            int i = startIndex;
            String loc = null, link = null, prior = null, seriesId = null, occId = null;
            String recFreq = null;
            Timestamp recStart = null, recEnd = null;
            Integer recInterval = null, maxCap = null;
            switch (type) {
                case "IN_PERSON" -> loc = ((InPersonAppointment) a).getLocation();
                case "VIRTUAL" -> link = ((VirtualAppointment) a).getMeetingLink();
                case "FOLLOW_UP" -> prior = ((FollowUpAppointment) a).getPriorAppointmentId();
                case "RECURRING" -> {
                    RecurringAppointment r = (RecurringAppointment) a;
                    seriesId = r.getSeriesId();
                    occId = r.getOccurrenceId();
                    RecurrencePattern p = r.getRecurrencePattern();
                    if (p != null) {
                        recFreq = p.getFrequency().name();
                        recStart = Timestamp.valueOf(p.getSeriesStart());
                        recEnd = Timestamp.valueOf(p.getSeriesEnd());
                        recInterval = p.getInterval();
                    }
                }
                case "GROUP" -> maxCap = ((GroupAppointment) a).getMaxCapacity();
                default -> { }
            }
            ps.setString(i++, loc);
            ps.setString(i++, link);
            ps.setString(i++, prior);
            ps.setString(i++, seriesId);
            ps.setString(i++, occId);
            ps.setString(i++, recFreq);
            ps.setTimestamp(i++, recStart);
            ps.setTimestamp(i++, recEnd);
            ps.setObject(i++, recInterval);
            ps.setObject(i, maxCap);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set type-specific params", e);
        }
    }

    private Appointment mapRow(ResultSet rs) {
        try {
            String id = rs.getString("id");
            String patientId = rs.getString("patient_id");
            User patient = userRepository.findById(patientId).orElseThrow(() -> new IllegalStateException("Patient not found: " + patientId));
            LocalDateTime start = rs.getObject("start_time", LocalDateTime.class);
            LocalDateTime end = rs.getObject("end_time", LocalDateTime.class);
            TimeSlot slot = new TimeSlot(start, end);
            String type = rs.getString("appointment_type");
            if (type == null) type = "INDIVIDUAL";

            Appointment a = switch (type) {
                case "IN_PERSON" -> new InPersonAppointment(id, patient, slot, rs.getString("location"));
                case "VIRTUAL" -> new VirtualAppointment(id, patient, slot, rs.getString("meeting_link"));
                case "FOLLOW_UP" -> new FollowUpAppointment(id, patient, slot, rs.getString("prior_appointment_id"));
                case "RECURRING" -> {
                    RecurrencePattern rp = null;
                    String freq = rs.getString("rec_frequency");
                    Timestamp rsStart = rs.getTimestamp("rec_series_start");
                    Timestamp rsEnd = rs.getTimestamp("rec_series_end");
                    int interval = rs.getInt("rec_interval");
                    if (freq != null && rsStart != null && rsEnd != null && interval >= 1) {
                        rp = new RecurrencePattern(RecurrencePattern.Frequency.valueOf(freq), rsStart.toLocalDateTime(), rsEnd.toLocalDateTime(), interval);
                    }
                    if (rp == null) rp = new RecurrencePattern(RecurrencePattern.Frequency.WEEKLY, start, end.plusYears(1), 1);
                    yield new RecurringAppointment(id, patient, slot, rs.getString("series_id"), rp, rs.getString("occurrence_id"));
                }
                case "GROUP" -> new GroupAppointment(id, patient, slot, rs.getInt("max_capacity") > 0 ? rs.getInt("max_capacity") : 10);
                case "ASSESSMENT" -> new AssessmentAppointment(id, patient, slot);
                case "URGENT" -> new UrgentAppointment(id, patient, slot);
                default -> new IndividualAppointment(id, patient, slot);
            };

            a.setStatus(rs.getString("status"));
            a.setParticipantCount(Math.max(1, rs.getInt("participant_count")));
            a.setDeletedState(rs.getBoolean("deleted"), rs.getObject("deleted_at", LocalDateTime.class), rs.getString("deleted_by"));
            a.setDoctorId(rs.getString("doctor_id"));
            a.setRoomId(rs.getString("room_id"));
            a.setClinicId(rs.getString("clinic_id"));
            a.setUrgent(rs.getBoolean("urgent"));
            String cn = getOptionalString(rs, "customer_notes");
            if (cn != null) a.setCustomerNotes(cn);
            String cp = getOptionalString(rs, "contact_phone");
            if (cp != null) a.setContactPhone(cp);
            String rc = getOptionalString(rs, "reminder_channel");
            if (rc != null) a.setReminderChannel(rc);
            String an = getOptionalString(rs, "accessibility_needs");
            if (an != null) a.setAccessibilityNeeds(an);
            String pl = getOptionalString(rs, "preferred_language");
            if (pl != null) a.setPreferredLanguage(pl);
            return a;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to map appointment row", e);
        }
    }

    private static String getOptionalString(ResultSet rs, String column) {
        try {
            String s = rs.getString(column);
            return rs.wasNull() ? null : s;
        } catch (SQLException e) {
            return null;
        }
    }
}
