package com.example.dataflow.db;

import com.example.dataflow.config.TableMapping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private final String jdbcUrl;
    private final List<TableMapping> tableMappings;
    private transient Connection connection;

    public AlloyDBWriter(String jdbcUrl, List<TableMapping> tableMappings) {
        this.jdbcUrl = jdbcUrl;
        this.tableMappings = tableMappings;
    }

    @Setup
    public void setup() throws SQLException {
        connection = AlloyDBConnectionUtil.getConnection(jdbcUrl);
    }

    @ProcessElement
    public void processElement(@Element String message, OutputReceiver<Void> receiver) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(message);
            String messageType = jsonNode.path("type").asText();

            TableMapping mapping = tableMappings.stream()
                    .filter(m -> m.getMessageType().equals(messageType))
                    .findFirst()
                    .orElse(null);

            if (mapping == null) {
                LOG.warn("No table mapping found for message type: {}", messageType);
                return;
            }

            insertIntoTable(mapping, jsonNode);
        } catch (Exception e) {
            LOG.error("Error processing message: " + message, e);
        }
    }

    private void insertIntoTable(TableMapping mapping, JsonNode jsonNode) throws SQLException {
        List<String> columnNames = mapping.getColumns().stream()
                .map(TableMapping.ColumnMapping::getColumnName)
                .collect(Collectors.toList());

        String sql = createInsertStatement(mapping.getTableName(), columnNames);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < mapping.getColumns().size(); i++) {
                TableMapping.ColumnMapping column = mapping.getColumns().get(i);
                JsonNode value = jsonNode.at(column.getJsonPath());
                setParameterValue(stmt, i + 1, value, column);
            }
            stmt.executeUpdate();
        }
    }

    private String createInsertStatement(String tableName, List<String> columnNames) {
        String columns = String.join(", ", columnNames);
        String placeholders = String.join(", ", columnNames.stream().map(c -> "?").collect(Collectors.toList()));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

    private void setParameterValue(PreparedStatement stmt, int index, JsonNode value, TableMapping.ColumnMapping column) throws SQLException {
        if (value.isNull()) {
            stmt.setNull(index, java.sql.Types.NULL);
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
                        stmt.setDate(index, new java.sql.Date(parsedDate.getTime()));
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

    @Teardown
    public void teardown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
