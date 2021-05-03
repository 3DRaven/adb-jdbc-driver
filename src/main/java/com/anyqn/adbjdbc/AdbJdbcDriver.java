/*******************************************************************************
 * Copyright 2021 Renat Eskenin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.anyqn.adbjdbc;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.anyqn.adbjdbc.data.AdbDataConnection;
import com.anyqn.adbjdbc.meta.AdbMetadataConnection;

import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public class AdbJdbcDriver implements Driver {
    private static final int DB_NAME_GROUP = 2;
    private static final int PACKAGE_NAME_GROUP = 1;
    private static final String ADB = "adb";
    private static final Driver INSTANCE = new AdbJdbcDriver();
    private static boolean registered;

    static {
        load();
    }

    public static synchronized Driver load() {
        log.debug("Called");
        if (!registered) {
            registered = true;
            try {
                DriverManager.registerDriver(INSTANCE);
            } catch (final SQLException e) {
                throw new IllegalStateException(String.format("Unable to load driver with error %s", e));
            }
        }
        log.debug("Returned");
        return INSTANCE;
    }

    private final Pattern pathPatternWithDb = Pattern.compile("^/(.*?)(/.*)?$");

    private JadbConnection jadb;

    public AdbJdbcDriver() {
        log.debug("Called");
        try {
            preloadSqliteDriver();
            createAdbConnection();
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load SQLite jdbc driver", e);
        }
        log.debug("Returned");
    }

    @Override
    public boolean acceptsURL(final String jdbcUrl) throws SQLException {
        log.debug("Called");
        final URI uri = URI.create(jdbcUrl.substring(5));
        return Objects.equals(uri.getScheme(), ADB);
    }

    @Override
    public Connection connect(final String jdbcUrl, final Properties properties) throws SQLException {
        log.debug("Called with jdbcUrl {} and properties {}", jdbcUrl, properties);
        final String cleanURI = jdbcUrl.substring(5);
        final URI uri = URI.create(cleanURI);
        final String deviceName = StringUtils.defaultIfBlank(uri.getHost(), uri.getAuthority());
        final String path = uri.getPath();
        final String query = uri.getQuery();
        log.info("Connect to device: [{}] db [{}] params [{}]", deviceName, path, query);
        if ("adb".equals(uri.getScheme())) {

            final Map<String, String> params = getParams(query);

            final Matcher matcherWithDb = pathPatternWithDb.matcher(path);

            String packageName = null;
            String dbName = null;

            if (matcherWithDb.find()) {
                packageName = matcherWithDb.group(PACKAGE_NAME_GROUP);
                dbName = matcherWithDb.group(DB_NAME_GROUP);
                log.debug("Try to start with package name {} and db name {}", packageName, dbName);

                if (null == dbName) {
                    return new AdbMetadataConnection(deviceName, packageName, params, jadb);
                } else {
                    return new AdbDataConnection(deviceName, packageName, dbName, params, jadb);
                }
            } else {
                throw new IllegalArgumentException(String.format("Unable to get package from %s", path));
            }
        } else {
            throw new IllegalArgumentException("Unknown connection url type");
        }
    }

    private void createAdbConnection() {
        log.debug("Called");
        try {
            jadb = new JadbConnection();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to connect to adb", e);
        }
    }

    @Override
    public int getMajorVersion() {
        log.debug("Called");
        return 0;
    }

    @Override
    public int getMinorVersion() {
        log.debug("Called");
        return 0;
    }

    private Map<String, String> getParams(final String query) {
        log.debug("Called");
        Map<String, String> params;
        if (null != query) {
            params = Arrays.asList(query.split("&")).stream().map(qp -> {
                final String[] split = qp.split("=");
                if (split.length != 2) {
                    throw new IllegalArgumentException(String.format("Broken jdbc url parameter [%s]", qp));
                }
                return split;
            }).collect(Collectors.toMap(s -> s[0], s -> s[1]));
        } else {
            params = Collections.emptyMap();
        }
        log.debug("Retrieved jdbcUrl params \n{}", params);
        return params;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        log.debug("Called");
        return null;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String s, final Properties properties) throws SQLException {
        log.debug("Called");
        return new DriverPropertyInfo[0];
    }

    @Override
    public boolean jdbcCompliant() {
        log.debug("Called");
        return true;
    }

    private void preloadSqliteDriver() throws ClassNotFoundException {
        log.debug("Called");
        Class.forName("org.sqlite.JDBC");
    }

}
