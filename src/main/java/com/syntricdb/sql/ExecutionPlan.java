package com.syntricdb.sql;

public class ExecutionPlan {
    public enum ExecutionStrategy {
        CACHE_LOOKUP,
        INDEX_VECTOR_HNSW,
        INDEX_INVERTED_FULLTEXT,
        INDEX_PRIMARY_KEY,
        FULL_TABLE_SCAN
    }

    private final ExecutionStrategy strategy;
    private final String description;
    private final double estimatedCost;
    private final AST.SelectStatement statement;

    public ExecutionPlan(ExecutionStrategy strategy, String description, double estimatedCost, AST.SelectStatement statement) {
        this.strategy = strategy;
        this.description = description;
        this.estimatedCost = estimatedCost;
        this.statement = statement;
    }

    public ExecutionStrategy getStrategy() { return strategy; }
    public String getDescription() { return description; }
    public double getEstimatedCost() { return estimatedCost; }
    public AST.SelectStatement getStatement() { return statement; }
}
