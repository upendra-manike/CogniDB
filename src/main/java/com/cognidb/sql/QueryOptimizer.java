package com.cognidb.sql;

import com.cognidb.engine.StorageEngine;
import com.cognidb.engine.schema.TableSchema;

public class QueryOptimizer {
    private final StorageEngine storageEngine;

    public QueryOptimizer(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    public ExecutionPlan optimize(AST.SelectStatement stmt) {
        TableSchema schema = storageEngine.getSchema(stmt.getTableName());
        if (schema == null) {
            throw new IllegalArgumentException("Table '" + stmt.getTableName() + "' does not exist.");
        }

        // 1. Check if query is vector similarity search
        if (stmt.getVectorSearchCondition() != null) {
            AST.VectorSearchCondition vecCond = stmt.getVectorSearchCondition();
            return new ExecutionPlan(
                ExecutionPlan.ExecutionStrategy.INDEX_VECTOR_HNSW,
                "HNSW Vector Index Scan on column '" + vecCond.getVectorColumn() + "' (Dimension=" + schema.getColumn(vecCond.getVectorColumn()).getVectorDimension() + ", BeamWidth=64)",
                1.5,
                stmt
            );
        }

        // 2. Check if full-text inverted index search
        if (stmt.getFullTextCondition() != null) {
            return new ExecutionPlan(
                ExecutionPlan.ExecutionStrategy.INDEX_INVERTED_FULLTEXT,
                "Inverted BM25 Index Search for query '" + stmt.getFullTextCondition().getQueryText() + "'",
                2.0,
                stmt
            );
        }

        // 3. Check for primary key point lookup
        String pkCol = schema.getPrimaryKeyColumn();
        if (pkCol != null) {
            for (AST.Condition cond : stmt.getWhereConditions()) {
                if (pkCol.equalsIgnoreCase(cond.getColumn()) && "=".equals(cond.getOperator())) {
                    return new ExecutionPlan(
                        ExecutionPlan.ExecutionStrategy.INDEX_PRIMARY_KEY,
                        "LSM Primary Key Index Point Lookup on key='" + cond.getValue() + "'",
                        0.1,
                        stmt
                    );
                }
            }
        }

        // 4. Fallback: Full Table Scan with Predicate Pushdown
        return new ExecutionPlan(
            ExecutionPlan.ExecutionStrategy.FULL_TABLE_SCAN,
            "Full Table Scan with Predicate Pushdown filter",
            10.0,
            stmt
        );
    }
}
