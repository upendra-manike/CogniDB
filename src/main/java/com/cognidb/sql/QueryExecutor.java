package com.cognidb.sql;

import com.cognidb.ai.AIEngine;
import com.cognidb.engine.StorageEngine;
import com.cognidb.engine.fulltext.InvertedIndex;
import com.cognidb.engine.schema.*;
import com.cognidb.engine.vector.HNSWIndex;

import java.util.*;

public class QueryExecutor {
    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;
    private final QueryOptimizer optimizer;
    private final SQLParser parser;

    public static class QueryResult {
        private final List<Map<String, Object>> rows;
        private final ExecutionPlan executionPlan;
        private final long executionTimeNs;
        private final String message;

        public QueryResult(List<Map<String, Object>> rows, ExecutionPlan executionPlan, long executionTimeNs, String message) {
            this.rows = rows;
            this.executionPlan = executionPlan;
            this.executionTimeNs = executionTimeNs;
            this.message = message;
        }

        public List<Map<String, Object>> getRows() { return rows; }
        public ExecutionPlan getExecutionPlan() { return executionPlan; }
        public double getExecutionTimeMs() { return executionTimeNs / 1_000_000.0; }
        public String getMessage() { return message; }
    }

    public QueryExecutor(StorageEngine storageEngine, AIEngine aiEngine) {
        this.storageEngine = storageEngine;
        this.aiEngine = aiEngine;
        this.optimizer = new QueryOptimizer(storageEngine);
        this.parser = new SQLParser(aiEngine);
    }

    public QueryResult execute(String sql) throws Exception {
        long startTime = System.nanoTime();
        AST.Statement stmt = parser.parse(sql);

        if (stmt instanceof AST.CreateTableStatement) {
            AST.CreateTableStatement createStmt = (AST.CreateTableStatement) stmt;
            TableSchema schema = new TableSchema(createStmt.getTableName());
            for (ColumnDef col : createStmt.getColumns()) {
                schema.addColumn(col);
            }
            storageEngine.createTable(schema);
            long elapsed = System.nanoTime() - startTime;
            return new QueryResult(Collections.emptyList(), null, elapsed, "Table '" + createStmt.getTableName() + "' created successfully.");
        }

        if (stmt instanceof AST.InsertStatement) {
            AST.InsertStatement insertStmt = (AST.InsertStatement) stmt;
            TableSchema schema = storageEngine.getSchema(insertStmt.getTableName());
            if (schema == null) {
                throw new IllegalArgumentException("Table '" + insertStmt.getTableName() + "' does not exist.");
            }

            Tuple rawTuple = insertStmt.getTuple();
            Tuple alignedTuple = new Tuple();
            List<ColumnDef> cols = schema.getColumnList();

            // Align positional vs key-based inserts
            int positionalIndex = 0;
            for (ColumnDef col : cols) {
                Object val = rawTuple.get(col.getName());
                if (val == null) {
                    val = rawTuple.get("val_" + positionalIndex);
                    positionalIndex++;
                }

                if (col.getType() == ColumnType.FLOAT_VECTOR && val instanceof String) {
                    val = aiEngine.aiEmbed(val.toString(), col.getVectorDimension() > 0 ? col.getVectorDimension() : 128);
                }
                alignedTuple.set(col.getName(), val);
            }

            storageEngine.insert(insertStmt.getTableName(), alignedTuple);
            long elapsed = System.nanoTime() - startTime;
            return new QueryResult(Collections.emptyList(), null, elapsed, "1 row inserted successfully into '" + insertStmt.getTableName() + "'.");
        }

        if (stmt instanceof AST.StreamPublishStatement) {
            AST.StreamPublishStatement pub = (AST.StreamPublishStatement) stmt;
            storageEngine.getStreamEngine().publish(pub.getTopic(), pub.getPayload());
            long elapsed = System.nanoTime() - startTime;
            return new QueryResult(Collections.emptyList(), null, elapsed, "Message published to stream topic '" + pub.getTopic() + "'.");
        }

        if (stmt instanceof AST.SelectStatement) {
            AST.SelectStatement selectStmt = (AST.SelectStatement) stmt;
            ExecutionPlan plan = optimizer.optimize(selectStmt);

            List<Tuple> candidateTuples = new ArrayList<>();

            switch (plan.getStrategy()) {
                case INDEX_VECTOR_HNSW: {
                    AST.VectorSearchCondition vecCond = selectStmt.getVectorSearchCondition();
                    if (selectStmt.getLimit() <= 0 && vecCond.getK() > 0) {
                        selectStmt.setLimit(vecCond.getK());
                    }
                    HNSWIndex hnsw = storageEngine.getVectorIndex(selectStmt.getTableName(), vecCond.getVectorColumn());
                    if (hnsw != null) {
                        float[] targetVec = vecCond.getTargetVector() != null ? vecCond.getTargetVector() : aiEngine.aiEmbed(vecCond.getQueryText());
                        List<HNSWIndex.VectorSearchResult> searchResults = hnsw.search(targetVec, vecCond.getK());
                        for (HNSWIndex.VectorSearchResult res : searchResults) {
                            Tuple tuple = storageEngine.getByPrimaryKey(selectStmt.getTableName(), res.getId());
                            if (tuple != null) {
                                tuple.set("_similarity_score", res.getSimilarity());
                                tuple.set("_vector_distance", res.getDistance());
                                candidateTuples.add(tuple);
                            }
                        }
                    } else {
                        candidateTuples = storageEngine.scanAll(selectStmt.getTableName());
                    }
                    break;
                }

                case INDEX_INVERTED_FULLTEXT: {
                    AST.FullTextCondition ftCond = selectStmt.getFullTextCondition();
                    InvertedIndex invIdx = storageEngine.getInvertedIndex(selectStmt.getTableName());
                    if (invIdx != null) {
                        List<InvertedIndex.SearchResult> results = invIdx.search(ftCond.getQueryText(), selectStmt.getLimit() > 0 ? selectStmt.getLimit() : 100);
                        for (InvertedIndex.SearchResult res : results) {
                            Tuple tuple = storageEngine.getByPrimaryKey(selectStmt.getTableName(), res.getDocId());
                            if (tuple != null) {
                                tuple.set("_bm25_score", res.getScore());
                                candidateTuples.add(tuple);
                            }
                        }
                    } else {
                        candidateTuples = storageEngine.scanAll(selectStmt.getTableName());
                    }
                    break;
                }

                case INDEX_PRIMARY_KEY: {
                    TableSchema schema = storageEngine.getSchema(selectStmt.getTableName());
                    String pkCol = schema.getPrimaryKeyColumn();
                    String pkVal = null;
                    for (AST.Condition c : selectStmt.getWhereConditions()) {
                        if (pkCol.equalsIgnoreCase(c.getColumn())) {
                            pkVal = c.getValue().toString();
                            break;
                        }
                    }
                    if (pkVal != null) {
                        Tuple tuple = storageEngine.getByPrimaryKey(selectStmt.getTableName(), pkVal);
                        if (tuple != null) candidateTuples.add(tuple);
                    }
                    break;
                }

                case FULL_TABLE_SCAN:
                default:
                    candidateTuples = storageEngine.scanAll(selectStmt.getTableName());
                    break;
            }

            // Apply WHERE scalar filtering pushdown
            List<Tuple> filtered = new ArrayList<>();
            for (Tuple tuple : candidateTuples) {
                if (matchesWhereConditions(tuple, selectStmt.getWhereConditions())) {
                    filtered.add(tuple);
                }
            }

            // Apply Ordering
            if (selectStmt.getOrderByColumn() != null) {
                String sortCol = selectStmt.getOrderByColumn();
                filtered.sort((t1, t2) -> {
                    Object v1 = t1.get(sortCol);
                    Object v2 = t2.get(sortCol);
                    if (v1 == null) return 1;
                    if (v2 == null) return -1;
                    if (v1 instanceof Comparable && v2 instanceof Comparable) {
                        int cmp = ((Comparable) v1).compareTo(v2);
                        return selectStmt.isOrderByDesc() ? -cmp : cmp;
                    }
                    return 0;
                });
            }

            // Apply Limit
            if (selectStmt.getLimit() > 0 && filtered.size() > selectStmt.getLimit()) {
                filtered = filtered.subList(0, selectStmt.getLimit());
            }

            // Apply Projection & AI Function Evaluation
            List<Map<String, Object>> outputRows = new ArrayList<>();
            for (Tuple tuple : filtered) {
                Map<String, Object> projectedRow = new LinkedHashMap<>();
                boolean isStar = selectStmt.getSelectItems().size() == 1 && "*".equals(selectStmt.getSelectItems().get(0).getColumnName());

                if (isStar) {
                    projectedRow.putAll(tuple.asMap());
                } else {
                    for (AST.SelectItem item : selectStmt.getSelectItems()) {
                        String colName = item.getColumnName();
                        if (item.getAiFunction() != null) {
                            if ("AI_SUMMARIZE".equalsIgnoreCase(item.getAiFunction())) {
                                String text = tuple.getString(colName);
                                projectedRow.put(item.getAlias(), aiEngine.aiSummarize(text));
                            } else if ("AI_CLASSIFY".equalsIgnoreCase(item.getAiFunction())) {
                                String text = tuple.getString(colName);
                                String[] labels = Arrays.copyOfRange(item.getAiArgs(), 1, item.getAiArgs().length);
                                projectedRow.put(item.getAlias(), aiEngine.aiClassify(text, labels));
                            }
                        } else {
                            projectedRow.put(item.getAlias(), tuple.get(colName));
                        }
                    }
                    if (tuple.get("_similarity_score") != null) projectedRow.put("_similarity_score", tuple.get("_similarity_score"));
                    if (tuple.get("_bm25_score") != null) projectedRow.put("_bm25_score", tuple.get("_bm25_score"));
                }

                outputRows.add(projectedRow);
            }

            long elapsed = System.nanoTime() - startTime;
            return new QueryResult(outputRows, plan, elapsed, "Query executed successfully. " + outputRows.size() + " rows returned.");
        }

        throw new IllegalArgumentException("Unknown SQL statement.");
    }

    private boolean matchesWhereConditions(Tuple tuple, List<AST.Condition> conditions) {
        for (AST.Condition cond : conditions) {
            Object actualVal = tuple.get(cond.getColumn());
            if (actualVal == null) return false;
            Object targetVal = cond.getValue();

            switch (cond.getOperator()) {
                case "=":
                    if (!actualVal.toString().equalsIgnoreCase(targetVal.toString())) return false;
                    break;
                case "!=":
                    if (actualVal.toString().equalsIgnoreCase(targetVal.toString())) return false;
                    break;
                case ">":
                    if (!(compareNumbers(actualVal, targetVal) > 0)) return false;
                    break;
                case "<":
                    if (!(compareNumbers(actualVal, targetVal) < 0)) return false;
                    break;
                case ">=":
                    if (!(compareNumbers(actualVal, targetVal) >= 0)) return false;
                    break;
                case "<=":
                    if (!(compareNumbers(actualVal, targetVal) <= 0)) return false;
                    break;
            }
        }
        return true;
    }

    private int compareNumbers(Object n1, Object n2) {
        double d1 = n1 instanceof Number ? ((Number) n1).doubleValue() : Double.parseDouble(n1.toString());
        double d2 = n2 instanceof Number ? ((Number) n2).doubleValue() : Double.parseDouble(n2.toString());
        return Double.compare(d1, d2);
    }
}
