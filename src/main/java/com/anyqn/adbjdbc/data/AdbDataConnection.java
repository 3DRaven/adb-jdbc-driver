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
package com.anyqn.adbjdbc.data;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.anyqn.adbjdbc.ConnectionHolder;
import com.anyqn.adbjdbc.util.ConsumerWithException;
import com.anyqn.adbjdbc.util.FunctionWithException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.vidstige.jadb.JadbConnection;

@Slf4j
public class AdbDataConnection implements java.sql.Connection {

    private DataFetchingTask dataFetchingTask;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ConnectionHolder connectionHolder = new ConnectionHolder();

    public AdbDataConnection(@NonNull final String deviceName, @NonNull final String packageName, final String dbName,
            final Map<String, String> params, @NonNull final JadbConnection jadb) throws SQLException {
        log.debug("Called");
        createPoolingTask(deviceName, packageName, dbName, params, jadb);
        startMetadataPooling(Long.valueOf(params.getOrDefault("initialDelay", "0")),
                Long.valueOf(params.getOrDefault("period", "60000")));
        connectionHolder.waitConnection();
    }

    @Override
    public void abort(final Executor executor) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.abort(executor));
    }

    @Override
    public void clearWarnings() throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.clearWarnings());
    }

    @Override
    public void close() throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.close());
    }

    @Override
    public void commit() throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.commit());
    }

    @Override
    public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createArrayOf(typeName, elements));
    }

    @Override
    public Blob createBlob() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createBlob());
    }

    @Override
    public Clob createClob() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createClob());
    }

    @Override
    public NClob createNClob() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createNClob());
    }

    private void createPoolingTask(@NonNull final String deviceIdentifier, @NonNull final String packageName,
            final String dbPath, @NonNull final Map<String, String> params, @NonNull final JadbConnection jadb)
            throws SQLException {
        log.debug("Called trying to start with package name {} and db name {}", packageName, dbPath);
        dataFetchingTask = new DataFetchingTask(deviceIdentifier, packageName, dbPath, params, connectionHolder, jadb);
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createSQLXML());
    }

    @Override
    public Statement createStatement() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createStatement());
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createStatement(resultSetType, resultSetConcurrency));
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        log.debug("Called");
        return getFromConnection(
                (final Connection c) -> c.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.createStruct(typeName, attributes));
    }

    private void executeInConnection(final ConsumerWithException<Connection, ? extends Exception> consumer) {
        try {
            if (connectionHolder.isExists()) {
                connectionHolder.acquire();
                consumer.accept(connectionHolder.getConnection());
                connectionHolder.release();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getAutoCommit());
    }

    @Override
    public String getCatalog() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getCatalog());
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getClientInfo());
    }

    @Override
    public String getClientInfo(final String name) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getClientInfo(name));
    }

    private <R> R getFromConnection(final FunctionWithException<Connection, R, Exception> consumer)
            throws SQLException {
        try {
            if (connectionHolder.isExists()) {
                connectionHolder.acquire();
                final R response = consumer.accept(connectionHolder.getConnection());
                connectionHolder.release();
                log.debug("Method call response {}", response);
                return response;
            } else {
                throw new IllegalStateException("Connection temporary closed");
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getHoldability());
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getMetaData());
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getNetworkTimeout());
    }

    @Override
    public String getSchema() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getSchema());
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getTransactionIsolation());
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getTypeMap());
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.getWarnings());
    }

    @Override
    public boolean isClosed() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.isClosed());
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.isReadOnly());
    }

    @Override
    public boolean isValid(final int timeout) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.isValid(timeout));
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.isWrapperFor(iface));
    }

    @Override
    public String nativeSQL(final String sql) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.nativeSQL(sql));
    }

    @Override
    public CallableStatement prepareCall(final String sql) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareCall(sql));
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareCall(sql, resultSetType, resultSetConcurrency));
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        log.debug("Called");
        return getFromConnection(
                (final Connection c) -> c.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareStatement(sql));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareStatement(sql, autoGeneratedKeys));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c
                .prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareStatement(sql, columnIndexes));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.prepareStatement(sql, columnNames));
    }

    @Override
    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.releaseSavepoint(savepoint));
    }

    @Override
    public void rollback() throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.rollback());
    }

    @Override
    public void rollback(final Savepoint savepoint) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.rollback());
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setAutoCommit(autoCommit));
    }

    @Override
    public void setCatalog(final String catalog) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setCatalog(catalog));
    }

    @Override
    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setClientInfo(properties));
    }

    @Override
    public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setClientInfo(name, value));
    }

    @Override
    public void setHoldability(final int holdability) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setHoldability(holdability));
    }

    @Override
    public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setNetworkTimeout(executor, milliseconds));
    }

    @Override
    public void setReadOnly(final boolean readOnly) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setReadOnly(readOnly));
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.setSavepoint());
    }

    @Override
    public Savepoint setSavepoint(final String name) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.setSavepoint(name));
    }

    @Override
    public void setSchema(final String schema) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setSchema(schema));
    }

    @Override
    public void setTransactionIsolation(final int level) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setTransactionIsolation(level));
    }

    @Override
    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        log.debug("Called");
        executeInConnection((final Connection c) -> c.setTypeMap(map));
    }

    private void startMetadataPooling(final long initialDelay, final long period) {
        log.debug("Called");
        executor.scheduleWithFixedDelay(dataFetchingTask, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        log.debug("Called");
        return getFromConnection((final Connection c) -> c.unwrap(iface));
    }

}
