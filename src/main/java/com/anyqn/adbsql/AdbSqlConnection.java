package com.anyqn.adbsql;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public class AdbSqlConnection implements java.sql.Connection {

    private DataFetchingTask dataFetchingTask;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    @Delegate
    private Connection metadataDbConnection;
    private final Pattern pathPatternWithDb = Pattern.compile("^/(.*?)(/.*)$");

    AdbSqlConnection(@NonNull final String jdbcUrl, @NonNull final JadbConnection jadb) throws SQLException {
        final String cleanURI = jdbcUrl.substring(5);
        final URI uri = URI.create(cleanURI);
        final String deviceName = StringUtils.defaultIfBlank(uri.getHost(), uri.getAuthority());
        final String path = uri.getPath();
        final String query = uri.getQuery();
        log.info("Connect to device: [{}] db [{}] params [{}]", deviceName, path, query);
        if (Objects.equals(uri.getScheme(), "adb")) {
            createMetadataDbConnection();
            createMetadataDb();
            final Map<String, String> params = getParams(query);
            createPoolingTask(deviceName, path, params, jadb);
            startMetadataPooling(Long.valueOf(params.getOrDefault("initialDelay", "0")),
                    Long.valueOf(params.getOrDefault("period", "1000")));
        } else {
            throw new IllegalArgumentException("Unknown connection url type");
        }
    }

    private void createMetadataDb() throws SQLException {
        try (Statement statement = metadataDbConnection.createStatement()) {
            statement.setQueryTimeout(30);
            final String metadataDdl = IOUtils
                    .resourceToString("metadata-db.sql", Charset.defaultCharset(), getClass().getClassLoader());
            statement.executeUpdate(metadataDdl);
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to load metadata-db.sql");
        }
    }

    private void createMetadataDbConnection() throws SQLException {
        log.debug("Called %M");
        metadataDbConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    private void createPoolingTask(@NonNull final String deviceIdentifier, @NonNull final String dbPath,
            @NonNull final Map<String, String> params, @NonNull final JadbConnection jadb) throws SQLException {
        log.debug("Called %M");

        final Matcher matcherWithDb = pathPatternWithDb.matcher(dbPath);

        String packageName = null;
        String dbName = null;

        if (matcherWithDb.find()) {
            packageName = matcherWithDb.group(1);
            dbName = matcherWithDb.group(2);
            dataFetchingTask = new DataFetchingTask(deviceIdentifier,
                    packageName,
                    dbName,
                    params,
                    metadataDbConnection,
                    jadb);
        } else {
            throw new NoSuchElementException(String.format("Unable to find package and db name in [%s]", dbPath));
        }
    }

    private Map<String, String> getParams(final String query) {
        log.debug("Called %M");
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
        return params;
    }

    private void startMetadataPooling(final long initialDelay, final long period) {
        executor.scheduleWithFixedDelay(dataFetchingTask, initialDelay, period, TimeUnit.MILLISECONDS);
    }

}
