package com.syntricdb.engine.schema;

import java.io.Serializable;
import java.util.*;

public class Tuple implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> values = new LinkedHashMap<>();

    public Tuple() {}

    public Tuple(Map<String, Object> values) {
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                this.values.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
    }

    public Tuple set(String colName, Object val) {
        values.put(colName.toLowerCase(), val);
        return this;
    }

    public Object get(String colName) {
        return values.get(colName.toLowerCase());
    }

    public String getString(String colName) {
        Object val = get(colName);
        return val != null ? val.toString() : null;
    }

    public Long getLong(String colName) {
        Object val = get(colName);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (Exception ignored) {}
        }
        return null;
    }

    public Integer getInt(String colName) {
        Object val = get(colName);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (Exception ignored) {}
        }
        return null;
    }

    public Double getDouble(String colName) {
        Object val = get(colName);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val != null) {
            try { return Double.parseDouble(val.toString()); } catch (Exception ignored) {}
        }
        return null;
    }

    public float[] getVector(String colName) {
        Object val = get(colName);
        if (val instanceof float[]) return (float[]) val;
        if (val instanceof double[]) {
            double[] d = (double[]) val;
            float[] f = new float[d.length];
            for (int i = 0; i < d.length; i++) f[i] = (float) d[i];
            return f;
        }
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            float[] f = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                f[i] = item instanceof Number ? ((Number) item).floatValue() : 0f;
            }
            return f;
        }
        return null;
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        return "Tuple" + values;
    }
}
