package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Room;
import com.appointmentscheduler.persistence.RoomRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of RoomRepository.
 */
public class JdbcRoomRepository implements RoomRepository {

    private static final String TABLE = "room";
    private final DataSource dataSource;

    public JdbcRoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Room room) {
        if (room == null) return;
        try (Connection c = dataSource.getConnection()) {
            String tbl = JdbcPostgresHelper.table(c, TABLE);
            String sql = JdbcPostgresHelper.isMySql(c)
                    ? "INSERT INTO " + tbl + " (id, name, clinic_id, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP) "
                    + "ON DUPLICATE KEY UPDATE name = VALUES(name), clinic_id = VALUES(clinic_id), updated_at = CURRENT_TIMESTAMP"
                    : "MERGE INTO " + tbl + " (id, name, clinic_id, updated_at) KEY(id) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
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
            String tbl = JdbcPostgresHelper.table(c, TABLE);
            String sql = "SELECT id, name, clinic_id FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
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
            String tbl = JdbcPostgresHelper.table(c, TABLE);
            String sql = "SELECT id, name, clinic_id FROM " + tbl + " ORDER BY name";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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
