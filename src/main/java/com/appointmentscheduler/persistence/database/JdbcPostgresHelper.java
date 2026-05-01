package com.appointmentscheduler.persistence.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Shared helpers for JDBC repositories (PostgreSQL, MySQL, H2).
 */
public final class JdbcPostgresHelper {

    private JdbcPostgresHelper() {}

    public static boolean isPostgres(Connection c) throws SQLException {
        String product = c.getMetaData() != null ? c.getMetaData().getDatabaseProductName() : "";
        return product != null && product.toLowerCase().contains("postgres");
    }

    public static boolean isMySql(Connection c) throws SQLException {
        String product = c.getMetaData() != null ? c.getMetaData().getDatabaseProductName() : "";
        return product != null && (product.toLowerCase().contains("mysql") || product.toLowerCase().contains("mariadb"));
    }

    /** Full table name: "appointment.foo" on PostgreSQL, "foo" on H2/MySQL. */
    public static String table(Connection c, String tableName) throws SQLException {
        return isPostgres(c) ? "appointment." + tableName : tableName;
    }
}
