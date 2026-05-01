package com.appointmentscheduler.persistence.database;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Wraps a {@link DataSource} so {@link Connection#getMetaData()} reports a chosen
 * {@link DatabaseMetaData#getDatabaseProductName()} value. Used to exercise MySQL-specific JDBC
 * branches against the in-memory H2 schema (MODE=MySQL), without a real MySQL server.
 */
public final class TestJdbcDataSources {

    private TestJdbcDataSources() {}

    public static DataSource withProductName(DataSource delegate, String databaseProductName) {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return wrapConnection(delegate.getConnection(), databaseProductName);
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return wrapConnection(delegate.getConnection(username, password), databaseProductName);
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return delegate.getLogWriter();
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                delegate.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                delegate.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return delegate.getLoginTimeout();
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return delegate.getParentLogger();
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return delegate.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return delegate.isWrapperFor(iface);
            }
        };
    }

    private static Connection wrapConnection(Connection conn, String databaseProductName) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) {
                        DatabaseMetaData md = conn.getMetaData();
                        return java.lang.reflect.Proxy.newProxyInstance(
                                DatabaseMetaData.class.getClassLoader(),
                                new Class<?>[]{DatabaseMetaData.class},
                                (p2, m2, a2) -> {
                                    if ("getDatabaseProductName".equals(m2.getName())) {
                                        return databaseProductName;
                                    }
                                    return a2 == null ? m2.invoke(md) : m2.invoke(md, a2);
                                });
                    }
                    return args == null ? method.invoke(conn) : method.invoke(conn, args);
                });
    }
}
