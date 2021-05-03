package com.anyqn.adbjdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AdbJdbcMetadata implements DatabaseMetaData {

    @Delegate
    private final DatabaseMetaData delegate;

    @Override
    public ResultSet getColumns(final String catalog, final String schemaPattern, final String tableNamePattern,
            final String columnNamePattern) throws SQLException {
        log.debug("Called");
        return delegate.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
    }

    @Override
    public ResultSet getTables(final String catalog, final String schemaPattern, final String tableNamePattern,
            final String[] types) throws SQLException {
        log.debug("Called");
        return delegate.getTables(catalog, schemaPattern, tableNamePattern, types);
    }
}
