package com.cognidb.engine.columnar;

import com.cognidb.engine.schema.ColumnType;
import java.util.*;

public class ColumnSegment {
    private final String columnName;
    private final ColumnType type;
    private final List<Object> values = new ArrayList<>();

    public ColumnSegment(String columnName, ColumnType type) {
        this.columnName = columnName.toLowerCase();
        this.type = type;
    }

    public void add(Object val) {
        values.add(val);
    }

    public double sum() {
        double total = 0;
        for (Object v : values) {
            if (v instanceof Number) total += ((Number) v).doubleValue();
        }
        return total;
    }

    public double average() {
        return values.isEmpty() ? 0 : sum() / values.size();
    }

    public double min() {
        double min = Double.MAX_VALUE;
        for (Object v : values) {
            if (v instanceof Number) {
                double val = ((Number) v).doubleValue();
                if (val < min) min = val;
            }
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }

    public double max() {
        double max = -Double.MAX_VALUE;
        for (Object v : values) {
            if (v instanceof Number) {
                double val = ((Number) v).doubleValue();
                if (val > max) max = val;
            }
        }
        return max == -Double.MAX_VALUE ? 0 : max;
    }

    public int count() {
        return values.size();
    }

    public Object get(int rowIdx) {
        return (rowIdx >= 0 && rowIdx < values.size()) ? values.get(rowIdx) : null;
    }

    public String getColumnName() { return columnName; }
    public ColumnType getType() { return type; }
}
