package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Doctor;
import com.appointmentscheduler.persistence.DoctorRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of DoctorRepository.
 */
public class JdbcDoctorRepository implements DoctorRepository {

    private static final String TABLE = "doctor";
    private final DataSource dataSource;

    public JdbcDoctorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Doctor doctor) {
        if (doctor == null) return;
        try (Connection c = dataSource.getConnection()) {
            String tbl = JdbcPostgresHelper.table(c, TABLE);
            String sql = JdbcPostgresHelper.isMySql(c)
                    ? "INSERT INTO " + tbl + " (id, name, email, specialty, max_appointments_per_day, clinic_id, updated_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), email = VALUES(email), specialty = VALUES(specialty), max_appointments_per_day = VALUES(max_appointments_per_day), clinic_id = VALUES(clinic_id), updated_at = CURRENT_TIMESTAMP"
                    : "MERGE INTO " + tbl + " (id, name, email, specialty, max_appointments_per_day, clinic_id, updated_at) KEY(id) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, doctor.getId());
                ps.setString(2, doctor.getName());
                ps.setString(3, doctor.getEmail());
                ps.setString(4, doctor.getSpecialty());
                ps.setInt(5, doctor.getMaxAppointmentsPerDay());
                ps.setString(6, doctor.getClinicId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save doctor: " + doctor.getId(), e);
        }
    }

    @Override
    public Optional<Doctor> findById(String id) {
        if (id == null) return Optional.empty();
        try (Connection c = dataSource.getConnection()) {
            String tbl = JdbcPostgresHelper.table(c, TABLE);
            String sql = "SELECT id, name, email, specialty, max_appointments_per_day, clinic_id FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find doctor: " + id, e);
        }
    }

    @Override
    public List<Doctor> findAll() {
        try (Connection c = dataSource.getConnection()) {
            String tbl = JdbcPostgresHelper.table(c, TABLE);
            String sql = "SELECT id, name, email, specialty, max_appointments_per_day, clinic_id FROM " + tbl + " ORDER BY name";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                List<Doctor> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list doctors", e);
        }
    }

    private static Doctor mapRow(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("specialty"),
                rs.getInt("max_appointments_per_day"),
                rs.getString("clinic_id")
        );
    }
}
