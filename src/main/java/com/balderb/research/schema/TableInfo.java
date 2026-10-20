package com.balderb.research.schema;

import java.util.List;

/**
 * A database table and its columns.
 */
public record TableInfo(String name, List<ColumnInfo> columns) {

    public TableInfo {
        columns = List.copyOf(columns);
    }
}
