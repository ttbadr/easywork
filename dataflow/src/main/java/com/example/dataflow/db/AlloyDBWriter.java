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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A DoFn that writes messages to AlloyDB based on table mappings.
 */
public class AlloyDBWriter extends DoFn<String, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(AlloyDBWriter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                setParameterValue(stmt, i + 1, value, column.getColumnType());
            }
            stmt.executeUpdate();
        }
    }

    private String createInsertStatement(String tableName, List<String> columnNames) {
        String columns = String.join(", ", columnNames);
        String placeholders = String.join(", ", columnNames.stream().map(c -> "?").collect(Collectors.toList()));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

    private void setParameterValue(PreparedStatement stmt, int index, JsonNode value, String columnType) throws SQLException {
        if (value.isNull()) {
            stmt.setNull(index, java.sql.Types.NULL);
            return;
        }

        switch (columnType.toUpperCase()) {
            case "STRING":
                stmt.setString(index, value.asText());
                break;
            case "INTEGER":
                stmt.setInt(index, value.asInt());
                break;
            case "BIGINT":
                stmt.setLong(index, value.asLong());
                break;
            case "DOUBLE":
                stmt.setDouble(index, value.asDouble());
                break;
            case "BOOLEAN":
                stmt.setBoolean(index, value.asBoolean());
                break;
            default:
                stmt.setString(index, value.asText());
        }
    }

    @Teardown
    public void teardown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}