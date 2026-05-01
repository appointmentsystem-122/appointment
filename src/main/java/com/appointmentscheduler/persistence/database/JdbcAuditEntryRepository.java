package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.AuditEntry;
import com.appointmentscheduler.persistence.AuditEntryRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of AuditEntryRepository. Append-only table.
 */
public class JdbcAuditEntryRepository implements AuditEntryRepository {

    private static final String TABLE = "audit_entry";
    private final DataSource dataSource;

    public JdbcAuditEntryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(AuditEntry entry) {
        if (entry == null) return;
        String sql = "INSERT INTO " + TABLE + " (timestamp, user_id, user_name, action, details, entity_type, entity_id, old_value, new_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, entry.getTimestamp());
            ps.setString(2, entry.getUserId());
            ps.setString(3, entry.getUserName());
            ps.setString(4, entry.getAction());
            ps.setString(5, entry.getDetails());
            ps.setString(6, entry.getEntityType());
            ps.setString(7, entry.getEntityId());
            ps.setString(8, entry.getOldValue());
            ps.setString(9, entry.getNewValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to append audit entry", e);
        }
    }

    @Override
    public List<AuditEntry> findRecent(int max) {
        String sql = "SELECT timestamp, user_id, user_name, action, details, entity_type, entity_id, old_value, new_value FROM " + TABLE + " ORDER BY id DESC LIMIT ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, max));
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditEntry> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find recent audit entries", e);
        }
    }

    @Override
    public List<AuditEntry> findAll() {
        String sql = "SELECT timestamp, user_id, user_name, action, details, entity_type, entity_id, old_value, new_value FROM " + TABLE + " ORDER BY id DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<AuditEntry> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list audit entries", e);
        }
    }

    @Override
    public List<AuditEntry> findByUserId(String userId) {
        if (userId == null) return findAll();
        String sql = "SELECT timestamp, user_id, user_name, action, details, entity_type, entity_id, old_value, new_value FROM " + TABLE + " WHERE user_id = ? ORDER BY id DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditEntry> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by user", e);
        }
    }

    @Override
    public List<AuditEntry> findByEntityType(String entityType) {
        if (entityType == null) return findAll();
        String sql = "SELECT timestamp, user_id, user_name, action, details, entity_type, entity_id, old_value, new_value FROM " + TABLE + " WHERE entity_type = ? ORDER BY id DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entityType);
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditEntry> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by entity type", e);
        }
    }

    private static AuditEntry mapRow(ResultSet rs) throws SQLException {
        return new AuditEntry(
                rs.getObject("timestamp", LocalDateTime.class),
                rs.getString("user_id"),
                rs.getString("user_name"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getString("old_value"),
                rs.getString("new_value")
        );
    }
}
