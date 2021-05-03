//package com.anyqn.adbsql;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//public class MainStar {
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
//        new AdbSqlDriver();
//        DriverManager.getConnection("jdbc:adb://*/*");
//    }
//}
