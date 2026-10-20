package com.balderb.research.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

/**
 * Reads the structure (tables and columns) of the configured database using JDBC metadata.
 * It never reads the data itself, only the description of the data.
 */
@Service
public class SchemaService {

    private static final Set<Integer> NUMERIC_TYPES = Set.of(
            Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
            Types.FLOAT, Types.REAL, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL);

    private final DataSource dataSource;

    public SchemaService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<TableInfo> listTables() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();
            List<TableInfo> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(null, schema, "%", new String[] {"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tables.add(new TableInfo(tableName, readColumns(metaData, schema, tableName)));
                }
            }
            return tables;
        }
        catch (SQLException e) {
            throw new IllegalStateException("Could not read the database structure", e);
        }
    }

    public Optional<TableInfo> findTable(String name) {
        return listTables().stream()
                .filter(table -> table.name().equalsIgnoreCase(name))
                .findFirst();
    }

    private List<ColumnInfo> readColumns(DatabaseMetaData metaData, String schema, String tableName)
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(null, schema, tableName, "%")) {
            while (rs.next()) {
                columns.add(new ColumnInfo(
                        rs.getString("COLUMN_NAME"),
                        rs.getString("TYPE_NAME"),
                        NUMERIC_TYPES.contains(rs.getInt("DATA_TYPE")),
                        rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
            }
        }
        return columns;
    }
}
