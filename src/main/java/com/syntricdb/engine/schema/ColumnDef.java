package com.syntricdb.engine.schema;

import java.util.Objects;

public class ColumnDef {
    private final String name;
    private final ColumnType type;
    private final int vectorDimension; // only relevant for FLOAT_VECTOR
    private final boolean primaryKey;
    private final boolean indexed;

    public ColumnDef(String name, ColumnType type, boolean primaryKey, boolean indexed) {
        this(name, type, 0, primaryKey, indexed);
    }

    public ColumnDef(String name, ColumnType type, int vectorDimension, boolean primaryKey, boolean indexed) {
        this.name = name.toLowerCase();
        this.type = type;
        this.vectorDimension = vectorDimension;
        this.primaryKey = primaryKey;
        this.indexed = indexed;
    }

    public String getName() {
        return name;
    }

    public ColumnType getType() {
        return type;
    }

    public int getVectorDimension() {
        return vectorDimension;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isIndexed() {
        return indexed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDef columnDef = (ColumnDef) o;
        return vectorDimension == columnDef.vectorDimension &&
                primaryKey == columnDef.primaryKey &&
                indexed == columnDef.indexed &&
                Objects.equals(name, columnDef.name) &&
                type == columnDef.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, vectorDimension, primaryKey, indexed);
    }
}
