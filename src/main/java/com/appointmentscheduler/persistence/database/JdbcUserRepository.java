package com.appointmentscheduler.persistence.database;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;
import com.appointmentscheduler.persistence.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of UserRepository. Single-table inheritance via user_type.
 */
public class JdbcUserRepository implements UserRepository {

    private static final String TABLE = "app_user";
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String tableFor(Connection c) throws SQLException {
        String product = c.getMetaData() != null ? c.getMetaData().getDatabaseProductName() : "";
        return (product != null && product.toLowerCase().contains("postgres")) ? "appointment." + TABLE : TABLE;
    }

    private static boolean isMySql(Connection c) throws SQLException {
        String product = c.getMetaData() != null ? c.getMetaData().getDatabaseProductName() : "";
        return product != null && (product.toLowerCase().contains("mysql") || product.toLowerCase().contains("mariadb"));
    }

    @Override
    public void save(User user) {
        if (user == null) return;
        String type = userType(user);
        try (Connection c = dataSource.getConnection()) {
            boolean isPostgres = isPostgres(c);
            boolean isMySql = isMySql(c);
            String tableName = isPostgres ? "appointment." + TABLE : TABLE;
            String sql;
            if (isPostgres) {
                sql = "INSERT INTO " + tableName + " (id, name, email, password_hash, user_type, updated_at) " +
                      "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                      "ON CONFLICT (id) DO UPDATE SET " +
                      "name = EXCLUDED.name, " +
                      "email = EXCLUDED.email, " +
                      "password_hash = EXCLUDED.password_hash, " +
                      "user_type = EXCLUDED.user_type, " +
                      "updated_at = CURRENT_TIMESTAMP";
            } else if (isMySql) {
                sql = "INSERT INTO " + tableName + " (id, name, email, password_hash, user_type, updated_at) " +
                      "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "name = VALUES(name), email = VALUES(email), password_hash = VALUES(password_hash), " +
                      "user_type = VALUES(user_type), updated_at = CURRENT_TIMESTAMP";
            } else {
                sql = "MERGE INTO " + tableName + " (id, name, email, password_hash, user_type, updated_at) " +
                      "KEY(id) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            }

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (isPostgres) {
                    ps.setObject(1, UUID.fromString(user.getId()), Types.OTHER);
                } else {
                    ps.setString(1, user.getId());
                }
                ps.setString(2, user.getName());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPassword());
                ps.setString(5, type);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user: " + user.getId(), e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        try (Connection c = dataSource.getConnection()) {
            String tbl = tableFor(c);
            boolean isPostgres = isPostgres(c);
            String sql = "SELECT id, name, email, password_hash, user_type FROM " + tbl + " WHERE id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (isPostgres && isValidUUID(id)) {
                    ps.setObject(1, UUID.fromString(id), Types.OTHER);
                } else {
                    ps.setString(1, id);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user: " + id, e);
        }
    }

    private static boolean isPostgres(Connection c) throws SQLException {
        String product = c.getMetaData() != null ? c.getMetaData().getDatabaseProductName() : "";
        return product != null && product.toLowerCase().contains("postgres");
    }

    private static boolean isValidUUID(String s) {
        if (s == null || s.length() != 36) return false;
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        try (Connection c = dataSource.getConnection()) {
            String tbl = tableFor(c);
            String sql = "SELECT id, name, email, password_hash, user_type FROM " + tbl + " WHERE LOWER(email) = LOWER(?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email", e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        return findAll();
    }

    @Override
    public List<User> findAll() {
        try (Connection c = dataSource.getConnection()) {
            String tbl = tableFor(c);
            String sql = "SELECT id, name, email, password_hash, user_type FROM " + tbl + " ORDER BY name";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list users", e);
        }
    }

    private static String userType(User u) {
        if (u instanceof Administrator) return "ADMINISTRATOR";
        if (u instanceof DoctorUser) return "DOCTOR";
        if (u instanceof ReceptionistUser) return "RECEPTIONIST";
        return "PATIENT";
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password_hash");
        String type = rs.getString("user_type");
        if (type == null) type = "PATIENT";
        return switch (type) {
            case "ADMINISTRATOR" -> new Administrator(id, name, email, password);
            case "DOCTOR" -> new DoctorUser(id, name, email, password);
            case "RECEPTIONIST" -> new ReceptionistUser(id, name, email, password);
            default -> new User(id, name, email, password);
        };
    }
}
