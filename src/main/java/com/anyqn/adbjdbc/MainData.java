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

//package com.anyqn.adbsql;
//
//import java.nio.charset.Charset;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//
//import org.apache.commons.io.IOUtils;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class MainData {
//
//    private static void logTable(final Connection conn, final String tablesListSql) throws SQLException {
//        final Statement stmt = conn.createStatement();
//        final ResultSet rs = stmt.executeQuery(tablesListSql);
//        final StringBuilder logged = new StringBuilder();
//        while (rs.next()) {
//            for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
//                logged.append(rs.getString(i + 1));
//                logged.append("|");
//            }
//            log.info("Tables: {}", logged.toString());
//        }
//    }
//
//    public static void main(final String[] args) throws Exception {
//
//        new AdbSqlDriver();
//
//        final Connection conn = DriverManager.getConnection(
//                "jdbc:adb://SM-G973F/com.anyqn.amhere/databases/main_database.db?period=1000&initialDelay=0&showAllFilesInPackage=true");
//        final String tablesListSql = IOUtils.resourceToString("tables-list.sql",
//                Charset.defaultCharset(),
//                Thread.currentThread().getContextClassLoader());
//        Thread.sleep(5000);
//        logTable(conn, tablesListSql);
//
//        logTable(conn, "Select * from android_db.android_metadata");
//
//    }
//}
