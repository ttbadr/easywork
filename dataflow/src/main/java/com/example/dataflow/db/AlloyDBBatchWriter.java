package com.example.dataflow.db;

import com.example.dataflow.config.TableMapping;
import com.example.dataflow.metrics.AlloyDBMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles batch writing of messages to AlloyDB.
 * Provides buffering, automatic flushing, and error handling capabilities.
 */
public class AlloyDBBatchWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AlloyDBBatchWriter.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long FLUSH_INTERVAL_MS = 1000; // 1 second flush interval
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // Initial retry delay of 1 second
    
    private final AlloyDBConnectionPool connectionPool;
    private final AlloyDBMetrics metrics;
    private final Map<String, List<JsonNode>> batchBuffer;
    private final Map<String, TableMapping> tableMappings;
    private final int batchSize;
    private final ScheduledExecutorService scheduler;
    private long lastFlushTime;

    /**
     * Creates a new batch writer with default batch size
     */
    public AlloyDBBatchWriter(AlloyDBConnectionPool connectionPool, List<TableMapping> mappings, String metricsName) {
        this(connectionPool, mappings, metricsName, DEFAULT_BATCH_SIZE);
    }

    /**
     * Creates a new batch writer with specified batch size
     */
    public AlloyDBBatchWriter(AlloyDBConnectionPool connectionPool, List<TableMapping> mappings, 
                            String metricsName, int batchSize) {
        this.connectionPool = connectionPool;
        this.metrics = new AlloyDBMetrics(metricsName);
        this.batchBuffer = new ConcurrentHashMap<>();
        this.tableMappings = mappings.stream()
            .collect(Collectors.toMap(TableMapping::getMessageType, m -> m));
        this.batchSize = batchSize;
        this.lastFlushTime = System.currentTimeMillis();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule periodic flush
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS) {
                    flushAll();
                }
            } catch (Exception e) {
                LOG.error("Error in scheduled flush", e);
            }
        }, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds a message to the batch buffer
     */
    public void addMessage(String messageType, JsonNode message) {
        TableMapping mapping = tableMappings.get(messageType);
        if (mapping == null) {
            LOG.warn("No table mapping found for message type: {}", messageType);
            return;
        }
        
        List<JsonNode> buffer = batchBuffer.computeIfAbsent(messageType, k -> new ArrayList<>());
        buffer.add(message);
        
        // Check if we should flush based on size or time
        if (buffer.size() >= batchSize || 
            System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS) {
            flush(messageType);
        }
    }

    /**
     * Flushes all message types in the batch buffer
     */
    public void flushAll() {
        for (String messageType : new ArrayList<>(batchBuffer.keySet())) {
            flush(messageType);
        }
    }

    /**
     * Flushes messages of a specific type from the batch buffer
     */
    private void flush(String messageType) {
        List<JsonNode> messages = batchBuffer.get(messageType);
        if (messages == null || messages.isEmpty()) {
            return;
        }

        TableMapping mapping = tableMappings.get(messageType);
        if (mapping == null) {
            LOG.error("No table mapping found for message type: {}", messageType);
            return;
        }

        int retryCount = 0;
        boolean success = false;
        Exception lastException = null;

        while (!success && retryCount < MAX_RETRY_ATTEMPTS) {
            try (Connection conn = connectionPool.getConnection()) {
                // Start transaction
                conn.setAutoCommit(false);
                
                String sql = AlloyDBWriter.createInsertStatement(mapping.getTableName(), 
                    mapping.getColumns().stream()
                        .map(TableMapping.ColumnMapping::getColumnName)
                        .collect(Collectors.toList()));
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    // Batch insert all messages
                    for (JsonNode message : messages) {
                        AlloyDBWriter.setStatementParameters(stmt, message, mapping.getColumns());
                        stmt.addBatch();
                    }
                    
                    // Execute batch and commit
                    stmt.executeBatch();
                    conn.commit();
                    
                    // Update metrics
                    metrics.recordBatchSize(messages.size());
                    metrics.incrementSuccessfulInserts(messages.size());
                    
                    success = true;
                    LOG.info("Successfully inserted {} messages for type: {}", messages.size(), messageType);
                }
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                
                // Handle batch error
                handleBatchError(messageType, messages, e, retryCount);
                
                // Wait before retry with exponential backoff
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount - 1);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!success) {
            // If all retries failed, handle the final failure
            handleFinalFailure(messageType, messages, lastException);
        }

        // Clear the processed messages from buffer
        batchBuffer.remove(messageType);
        lastFlushTime = System.currentTimeMillis();
    }

    /**
     * Handles errors during batch processing
     */
    private void handleBatchError(String messageType, List<JsonNode> messages, Exception e, int retryCount) {
        LOG.error("Error processing batch for message type: {} (attempt {}/{})", 
            messageType, retryCount, MAX_RETRY_ATTEMPTS, e);
        
        metrics.recordRetry();
        metrics.incrementFailedInserts(messages.size());
        
        // Log detailed error information
        LOG.error("Batch processing error details:");
        LOG.error("Message type: {}", messageType);
        LOG.error("Batch size: {}", messages.size());
        LOG.error("Error type: {}", e.getClass().getName());
        LOG.error("Error message: {}", e.getMessage());
        
        // Analyze error type for better handling
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            LOG.error("SQL State: {}", sqlEx.getSQLState());
            LOG.error("Error Code: {}", sqlEx.getErrorCode());
        }
    }

    /**
     * Handles final failure after all retries are exhausted
     */
    private void handleFinalFailure(String messageType, List<JsonNode> messages, Exception e) {
        LOG.error("Final failure processing batch for message type: {} after {} attempts", 
            messageType, MAX_RETRY_ATTEMPTS, e);
        
        // Record metrics for final failure
        metrics.incrementFinalFailures();
        
        // Log each failed message for potential recovery
        for (int i = 0; i < messages.size(); i++) {
            LOG.error("Failed message {}/{}: {}", i + 1, messages.size(), messages.get(i));
        }
        
        // Here you could implement additional recovery mechanisms:
        // 1. Write to a dead letter queue
        // 2. Save to an error table in the database
        // 3. Trigger alerts
        // 4. Write to a failure audit log
        
        // For now, we'll just ensure everything is logged for manual recovery
        LOG.error("Batch requires manual intervention or recovery process");
    }

    /**
     * Closes the batch writer and releases resources
     */
    public void close() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }
}
