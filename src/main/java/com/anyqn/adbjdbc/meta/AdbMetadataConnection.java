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
package com.anyqn.adbjdbc.meta;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import lombok.NonNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public class AdbMetadataConnection implements java.sql.Connection {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    @Delegate
    private Connection dbConnection;

    public AdbMetadataConnection(@NonNull final String deviceName, final String packageName,
            final Map<String, String> params, @NonNull final JadbConnection jadb) throws SQLException {
        log.debug("Called");
        createMetadataDb(createMetadataDbConnection());
        startMetadataPooling(createPoolingTask(deviceName, packageName, params, jadb),
                Long.valueOf(params.getOrDefault("initialDelay", "0")),
                Long.valueOf(params.getOrDefault("period", "60000")));
    }

    private void createMetadataDb(final Connection connection) throws SQLException {
        log.debug("Called");
        try (Statement statement = dbConnection.createStatement()) {
            final String metadataSql = IOUtils
                    .resourceToString("metadata-db.sql", Charset.defaultCharset(), getClass().getClassLoader());
            log.debug("Execute metadata query \n{}", metadataSql);
            statement.executeUpdate(metadataSql);
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to load metadata-db.sql");
        }
    }

    private Connection createMetadataDbConnection() throws SQLException {
        log.debug("Called");
        dbConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        return dbConnection;
    }

    private MetaDataFetchingTask createPoolingTask(@NonNull final String deviceIdentifier,
            @NonNull final String packageName, @NonNull final Map<String, String> params,
            @NonNull final JadbConnection jadb) throws SQLException {
        log.debug("Called try to start with package name {} and db name {}", packageName);
        return new MetaDataFetchingTask(deviceIdentifier, packageName, params, dbConnection, jadb);
    }

    private void startMetadataPooling(final MetaDataFetchingTask dataFetchingTask, final long initialDelay,
            final long period) {
        log.debug("Called");
        executor.scheduleWithFixedDelay(dataFetchingTask, initialDelay, period, TimeUnit.MILLISECONDS);
    }

}
