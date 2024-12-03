package com.example.dataflow.db;

import com.example.dataflow.config.TableMapping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.ValueState;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A DoFn that writes messages to AlloyDB based on table mappings.
 */
public class AlloyDBWriter extends DoFn<String, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(AlloyDBWriter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    private static final int DEFAULT_BATCH_SIZE = 1000;

    // 使用 ValueState 来跟踪已处理的消息
    @StateId("processedMessages")
    private final StateSpec<ValueState<Boolean>> processedSpec = StateSpecs.value();

    private final String jdbcUrl;
    private final List<TableMapping> tableMappings;
    private final int batchSize;
    private transient AlloyDBConnectionPool connectionPool;
    private transient AlloyDBBatchWriter batchWriter;

    public AlloyDBWriter(String jdbcUrl, List<TableMapping> tableMappings) {
        this(jdbcUrl, tableMappings, DEFAULT_BATCH_SIZE);
    }

    public AlloyDBWriter(String jdbcUrl, List<TableMapping> tableMappings, int batchSize) {
        this.jdbcUrl = jdbcUrl;
        this.tableMappings = tableMappings;
        this.batchSize = batchSize;
    }

    @Setup
    public void setup() throws SQLException {
        connectionPool = new AlloyDBConnectionPool(jdbcUrl);
        connectionPool.initialize();
        batchWriter = new AlloyDBBatchWriter(connectionPool, tableMappings, "alloydb_writer", batchSize);
        LOG.info("Initialized AlloyDB connection pool and batch writer");
    }

    @ProcessElement
    public void processElement(
            @Element String message,
            @StateId("processedMessages") ValueState<Boolean> processed,
            BoundedWindow window) {
        try {
            // 检查消息是否已经处理过
            if (Boolean.TRUE.equals(processed.read())) {
                LOG.debug("Message already processed, skipping");
                return;
            }

            JsonNode jsonNode = OBJECT_MAPPER.readTree(message);
            String messageType = jsonNode.path("type").asText();

            if (messageType == null || messageType.isEmpty()) {
                LOG.warn("Message type is missing or empty: {}", message);
                return;
            }

            batchWriter.addMessage(messageType, jsonNode);
            
            // 标记消息为已处理
            processed.write(true);
        } catch (Exception e) {
            LOG.error("Error processing message: " + message, e);
            throw new RuntimeException("Failed to process message", e);
        }
    }

    @FinishBundle
    public void finishBundle() {
        try {
            batchWriter.flushAll();
        } catch (Exception e) {
            LOG.error("Error flushing batch writer", e);
            throw new RuntimeException("Failed to flush batch writer", e);
        }
    }

    @Teardown
    public void teardown() {
        try {
            if (batchWriter != null) {
                batchWriter.flushAll();
            }
            if (connectionPool != null) {
                connectionPool.close();
                LOG.info("Closed AlloyDB connection pool");
            }
        } catch (Exception e) {
            LOG.error("Error during teardown", e);
            throw new RuntimeException("Failed during teardown", e);
        }
    }

    // Static utility method used by AlloyDBBatchWriter
    static void setParameterValue(PreparedStatement stmt, int index, JsonNode value, TableMapping.ColumnMapping column) throws SQLException {
        if (value.isNull()) {
            stmt.setNull(index, Types.NULL);
            return;
        }

        String columnType = column.getColumnType().toUpperCase();
        try {
            switch (columnType) {
                // Character types
                case "CHAR":
                case "CHARACTER":
                case "VARCHAR":
                case "CHARACTER VARYING":
                case "TEXT":
                    stmt.setString(index, value.asText());
                    break;

                // Integer types
                case "SMALLINT":
                case "INT2":
                    stmt.setShort(index, value.shortValue());
                    break;
                case "INT":
                case "INTEGER":
                case "INT4":
                    stmt.setInt(index, value.asInt());
                    break;
                case "BIGINT":
                case "INT8":
                    stmt.setLong(index, value.asLong());
                    break;

                // Floating-point types
                case "REAL":
                case "FLOAT4":
                    stmt.setFloat(index, value.floatValue());
                    break;
                case "DOUBLE PRECISION":
                case "FLOAT8":
                    stmt.setDouble(index, value.asDouble());
                    break;
                case "NUMERIC":
                case "DECIMAL":
                    stmt.setBigDecimal(index, value.decimalValue());
                    break;

                // Boolean types
                case "BOOLEAN":
                case "BOOL":
                    stmt.setBoolean(index, value.asBoolean());
                    break;

                // Date/Time types
                case "DATE":
                    try {
                        String format = column.getFormat();
                        if (format == null || format.isEmpty()) {
                            format = DEFAULT_DATE_FORMAT;
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                        java.util.Date parsedDate = dateFormat.parse(value.asText());
                        stmt.setDate(index, new Date(parsedDate.getTime()));
                    } catch (ParseException e) {
                        LOG.error("Error parsing DATE value: " + value.asText() + " with format: " + column.getFormat(), e);
                        throw new SQLException("Invalid DATE format for column " + column.getColumnName(), e);
                    }
                    break;

                case "TIMESTAMP":
                case "TIMESTAMP WITHOUT TIME ZONE":
                    try {
                        String format = column.getFormat();
                        if (format == null || format.isEmpty()) {
                            format = DEFAULT_TIMESTAMP_FORMAT;
                        }
                        SimpleDateFormat timestampFormat = new SimpleDateFormat(format);
                        java.util.Date parsedDate = timestampFormat.parse(value.asText());
                        stmt.setTimestamp(index, new java.sql.Timestamp(parsedDate.getTime()));
                    } catch (ParseException e) {
                        LOG.error("Error parsing TIMESTAMP value: " + value.asText() + " with format: " + column.getFormat(), e);
                        throw new SQLException("Invalid TIMESTAMP format for column " + column.getColumnName(), e);
                    }
                    break;

                case "TIMESTAMP WITH TIME ZONE":
                case "TIMESTAMPTZ":
                    try {
                        String format = column.getFormat();
                        if (format == null || format.isEmpty()) {
                            format = DEFAULT_TIMESTAMP_FORMAT;
                        }
                        SimpleDateFormat timestampFormat = new SimpleDateFormat(format);
                        java.util.Date parsedDate = timestampFormat.parse(value.asText());
                        stmt.setTimestamp(index, new java.sql.Timestamp(parsedDate.getTime()));
                    } catch (ParseException e) {
                        LOG.error("Error parsing TIMESTAMPTZ value: " + value.asText() + " with format: " + column.getFormat(), e);
                        throw new SQLException("Invalid TIMESTAMPTZ format for column " + column.getColumnName(), e);
                    }
                    break;

                default:
                    LOG.warn("Unrecognized AlloyDB column type: {}. Falling back to string representation.", columnType);
                    stmt.setString(index, value.asText());
            }
        } catch (Exception e) {
            LOG.error("Error setting value for column {}: type={}, value={}", column.getColumnName(), columnType, value.asText(), e);
            throw new SQLException("Error setting value for column " + column.getColumnName(), e);
        }
    }

    // Add the missing createInsertStatement method
    static String createInsertStatement(String tableName, List<String> columnNames) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        if (columnNames == null || columnNames.isEmpty()) {
            throw new IllegalArgumentException("Column names cannot be null or empty");
        }

        // Build the SQL INSERT statement dynamically
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(tableName).append(" (");
        sqlBuilder.append(String.join(", ", columnNames));
        sqlBuilder.append(") VALUES (");
        
        // Add placeholders for each column
        sqlBuilder.append(columnNames.stream()
            .map(col -> "?")
            .collect(Collectors.joining(", ")));
        
        sqlBuilder.append(")");
        
        return sqlBuilder.toString();
    }

    static void setStatementParameters(PreparedStatement stmt, JsonNode message, List<TableMapping.ColumnMapping> columns) throws SQLException {
        // ... existing code remains the same ...
    }
}
