package com.appointmentscheduler.persistence.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.persistence.ClinicRepository;

public class JdbcClinicRepository implements ClinicRepository {

    private static final String TABLE = "clinic";
    private final DataSource dataSource;

    public JdbcClinicRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static String clinicTable(Connection c) throws SQLException {
        String tableName = JdbcPostgresHelper.table(c, TABLE);
        if (!isSafeSqlIdentifierPath(tableName)) {
            throw new SQLException("Unsafe clinic table name: " + tableName);
        }
        return tableName;
    }

    private static boolean isSafeSqlIdentifierPath(String value) {
        return value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    }

    @SuppressWarnings("java:S2077")
    private static PreparedStatement prepareSafeStatement(Connection c, String sql) throws SQLException {
        return c.prepareStatement(sql);
    }

    @Override
    public void save(Clinic clinic) {
        if (clinic == null) return;
        try (Connection c = dataSource.getConnection()) {
            String tbl = clinicTable(c);
            String sql = JdbcPostgresHelper.isMySql(c)
                    ? "INSERT INTO " + tbl + " (id, name, address, time_zone, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), address = VALUES(address), time_zone = VALUES(time_zone), updated_at = CURRENT_TIMESTAMP"
                    : "MERGE INTO " + tbl + " (id, name, address, time_zone, updated_at) KEY(id) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ps = prepareSafeStatement(c, sql)) {
                ps.setString(1, clinic.getId());
                ps.setString(2, clinic.getName());
                ps.setString(3, clinic.getAddress());
                ps.setString(4, clinic.getTimeZone());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save clinic: " + clinic.getId(), e);
        }
    }

    @Override
    public Optional<Clinic> findById(String id) {
        if (id == null) return Optional.empty();
        try (Connection c = dataSource.getConnection()) {
            String tbl = clinicTable(c);
            String sql = "SELECT id, name, address, time_zone FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = prepareSafeStatement(c, sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find clinic: " + id, e);
        }
    }

    @Override
    public List<Clinic> findAll() {
        try (Connection c = dataSource.getConnection()) {
            String tbl = clinicTable(c);
            String sql = "SELECT id, name, address, time_zone FROM " + tbl + " ORDER BY name";
            try (PreparedStatement ps = prepareSafeStatement(c, sql); ResultSet rs = ps.executeQuery()) {
                List<Clinic> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list clinics", e);
        }
    }

    private static Clinic mapRow(ResultSet rs) throws SQLException {
        return new Clinic(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("time_zone") != null ? rs.getString("time_zone") : "UTC"
        );
    }
}