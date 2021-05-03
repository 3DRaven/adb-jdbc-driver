package com.anyqn.adbsql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public class AdbSqlDriver implements Driver {
    private static final Driver INSTANCE = new AdbSqlDriver();
    private static boolean registered;

    static {
        load();
    }

    public static synchronized Driver load() {
        if (!registered) {
            registered = true;
            try {
                DriverManager.registerDriver(INSTANCE);
            } catch (final SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        return INSTANCE;
    }

    private JadbConnection jadb;

    public AdbSqlDriver() {
        try {
            preloadSqliteDriver();
            createAdbConnection();
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load SQLite jdbc driver", e);
        }
    }

    @Override
    public boolean acceptsURL(final String s) throws SQLException {
        return true;
    }

    @Override
    public Connection connect(final String s, final Properties properties) throws SQLException {
        return new AdbSqlConnection(s, jadb);
    }

    private void createAdbConnection() {
        try {
            jadb = new JadbConnection();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to connect to adb", e);
        }
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String s, final Properties properties) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    private void preloadSqliteDriver() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

}
