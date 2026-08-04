package com.syntricdb.engine.schema;

import java.util.*;

public class TableSchema {
    private final String tableName;
    private final Map<String, ColumnDef> columns = new LinkedHashMap<>();
    private String primaryKeyColumn;
    private String vectorColumn;

    public TableSchema(String tableName) {
        this.tableName = tableName.toLowerCase();
    }

    public TableSchema addColumn(ColumnDef col) {
        columns.put(col.getName(), col);
        if (col.isPrimaryKey()) {
            this.primaryKeyColumn = col.getName();
        }
        if (col.getType() == ColumnType.FLOAT_VECTOR) {
            this.vectorColumn = col.getName();
        }
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, ColumnDef> getColumns() {
        return Collections.unmodifiableMap(columns);
    }

    public ColumnDef getColumn(String name) {
        return columns.get(name.toLowerCase());
    }

    public boolean hasColumn(String name) {
        return columns.containsKey(name.toLowerCase());
    }

    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public String getVectorColumn() {
        return vectorColumn;
    }

    public List<ColumnDef> getColumnList() {
        return new ArrayList<>(columns.values());
    }
}
