package com.example.dataflow.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for managing AlloyDB connections.
 */
public class AlloyDBConnectionUtil {
    
    public static Connection getConnection(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
    
    private AlloyDBConnectionUtil() {
        // Private constructor to prevent instantiation
    }
}
