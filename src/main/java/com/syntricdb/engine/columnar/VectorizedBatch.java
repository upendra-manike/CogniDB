package com.syntricdb.engine.columnar;

import com.syntricdb.engine.schema.Tuple;
import java.util.*;

public class VectorizedBatch {
    private final Map<String, ColumnSegment> columns = new LinkedHashMap<>();
    private int size = 0;

    public void addColumn(ColumnSegment col) {
        columns.put(col.getColumnName(), col);
    }

    public void appendTuple(Tuple tuple) {
        for (Map.Entry<String, Object> entry : tuple.asMap().entrySet()) {
            ColumnSegment col = columns.get(entry.getKey());
            if (col != null) {
                col.add(entry.getValue());
            }
        }
        size++;
    }

    public Map<String, Object> executeAggregation(String function, String columnName) {
        ColumnSegment col = columns.get(columnName.toLowerCase());
        Map<String, Object> res = new HashMap<>();

        if (col == null) {
            res.put("error", "Column not found");
            return res;
        }

        switch (function.toUpperCase()) {
            case "SUM":
                res.put("SUM(" + columnName + ")", col.sum());
                break;
            case "AVG":
                res.put("AVG(" + columnName + ")", col.average());
                break;
            case "MIN":
                res.put("MIN(" + columnName + ")", col.min());
                break;
            case "MAX":
                res.put("MAX(" + columnName + ")", col.max());
                break;
            case "COUNT":
                res.put("COUNT(" + columnName + ")", col.count());
                break;
        }
        return res;
    }

    public int getSize() { return size; }
}
