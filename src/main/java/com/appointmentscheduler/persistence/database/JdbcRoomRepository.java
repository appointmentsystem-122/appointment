package com.appointmentscheduler.persistence.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.persistence.RoomRepository;

/**
 * JDBC implementation of RoomRepository.
 */
public class JdbcRoomRepository implements RoomRepository {

    private static final String TABLE = "room";
    private final DataSource dataSource;

    public JdbcRoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static String roomTable(Connection c) throws SQLException {
        String tableName = JdbcPostgresHelper.table(c, TABLE);
        if (!isSafeSqlIdentifierPath(tableName)) {
            throw new SQLException("Unsafe room table name: " + tableName);
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
    public void save(Room room) {
        if (room == null) return;
        try (Connection c = dataSource.getConnection()) {
            String tbl = roomTable(c);
            String sql = JdbcPostgresHelper.isMySql(c)
                    ? "INSERT INTO " + tbl + " (id, name, clinic_id, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), clinic_id = VALUES(clinic_id), updated_at = CURRENT_TIMESTAMP"
                    : "MERGE INTO " + tbl + " (id, name, clinic_id, updated_at) KEY(id) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ps = prepareSafeStatement(c, sql)) {
                ps.setString(1, room.getId());
                ps.setString(2, room.getName());
                ps.setString(3, room.getClinicId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room: " + room.getId(), e);
        }
    }

    @Override
    public Optional<Room> findById(String id) {
        if (id == null) return Optional.empty();
        try (Connection c = dataSource.getConnection()) {
            String tbl = roomTable(c);
            String sql = "SELECT id, name, clinic_id FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = prepareSafeStatement(c, sql)) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room: " + id, e);
        }
    }

    @Override
    public List<Room> findAll() {
        try (Connection c = dataSource.getConnection()) {
            String tbl = roomTable(c);
            String sql = "SELECT id, name, clinic_id FROM " + tbl + " ORDER BY name";
            try (PreparedStatement ps = prepareSafeStatement(c, sql); ResultSet rs = ps.executeQuery()) {
                List<Room> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list rooms", e);
        }
    }

    private static Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("clinic_id")
        );
    }
}