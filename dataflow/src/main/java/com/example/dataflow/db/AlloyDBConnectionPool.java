package com.example.dataflow.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool manager for AlloyDB using HikariCP.
 */
public class AlloyDBConnectionPool implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(AlloyDBConnectionPool.class);
    private static final long serialVersionUID = 1L;
    
    // Transient because HikariDataSource is not serializable
    private transient HikariDataSource dataSource;
    private final String jdbcUrl;
    
    // Connection pool configuration
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int MINIMUM_IDLE = 5;
    private static final long MAX_LIFETIME = 1800000; // 30 minutes
    private static final long CONNECTION_TIMEOUT = 30000; // 30 seconds
    private static final long IDLE_TIMEOUT = 600000; // 10 minutes
    
    public AlloyDBConnectionPool(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }
    
    /**
     * Initializes the connection pool with HikariCP configuration.
     */
    public void initialize() {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(jdbcUrl);
                    config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
                    config.setMinimumIdle(MINIMUM_IDLE);
                    config.setMaxLifetime(MAX_LIFETIME);
                    config.setConnectionTimeout(CONNECTION_TIMEOUT);
                    config.setIdleTimeout(IDLE_TIMEOUT);
                    
                    // Connection health check
                    config.setConnectionTestQuery("SELECT 1");
                    config.setValidationTimeout(5000); // 5 seconds
                    
                    // Enable auto-commit
                    config.setAutoCommit(true);
                    
                    // Enable connection leak detection
                    config.setLeakDetectionThreshold(60000); // 1 minute
                    
                    dataSource = new HikariDataSource(config);
                    LOG.info("Initialized connection pool with configuration: {}", config);
                }
            }
        }
    }
    
    /**
     * Gets a connection from the pool with retry mechanism.
     */
    public Connection getConnection() throws SQLException {
        int maxRetries = 3;
        int retryCount = 0;
        SQLException lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                if (dataSource == null) {
                    initialize();
                }
                Connection conn = dataSource.getConnection();
                if (conn.isValid(5)) { // Test connection with 5 second timeout
                    return conn;
                }
            } catch (SQLException e) {
                lastException = e;
                LOG.warn("Failed to get connection (attempt {}/{}): {}", 
                    retryCount + 1, maxRetries, e.getMessage());
                retryCount++;
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupted while waiting to retry", ie);
                    }
                }
            }
        }
        
        throw new SQLException("Failed to get connection after " + maxRetries + " attempts", 
            lastException);
    }
    
    /**
     * Closes the connection pool.
     */
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            LOG.info("Closed connection pool");
        }
    }
    
    /**
     * Returns pool statistics for monitoring.
     */
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format("Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return "Pool not initialized";
    }
}
