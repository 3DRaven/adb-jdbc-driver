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
