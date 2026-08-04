package com.syntricdb.ai;

import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.engine.vector.HNSWIndex;

import java.util.*;

public class RAGEngine {
    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;

    public static class RAGResult {
        private final String prompt;
        private final List<Map<String, Object>> retrievedContext;
        private final String augmentedPrompt;
        private final String generatedAnswer;
        private final double retrievalTimeMs;

        public RAGResult(String prompt, List<Map<String, Object>> retrievedContext, String augmentedPrompt, String generatedAnswer, double retrievalTimeMs) {
            this.prompt = prompt;
            this.retrievedContext = retrievedContext;
            this.augmentedPrompt = augmentedPrompt;
            this.generatedAnswer = generatedAnswer;
            this.retrievalTimeMs = retrievalTimeMs;
        }

        public String getPrompt() { return prompt; }
        public List<Map<String, Object>> getRetrievedContext() { return retrievedContext; }
        public String getAugmentedPrompt() { return augmentedPrompt; }
        public String getGeneratedAnswer() { return generatedAnswer; }
        public double getRetrievalTimeMs() { return retrievalTimeMs; }
    }

    public RAGEngine(StorageEngine storageEngine, AIEngine aiEngine) {
        this.storageEngine = storageEngine;
        this.aiEngine = aiEngine;
    }

    public RAGResult query(String tableName, String vectorColumn, String prompt, int topK) throws Exception {
        long start = System.nanoTime();
        HNSWIndex hnsw = storageEngine.getVectorIndex(tableName, vectorColumn);

        List<Map<String, Object>> retrieved = new ArrayList<>();
        StringBuilder contextText = new StringBuilder();

        if (hnsw != null) {
            float[] queryVec = aiEngine.aiEmbed(prompt, hnsw.getDimension());
            List<HNSWIndex.VectorSearchResult> searchResults = hnsw.search(queryVec, topK);

            for (HNSWIndex.VectorSearchResult res : searchResults) {
                Tuple tuple = storageEngine.getByPrimaryKey(tableName, res.getId());
                if (tuple != null) {
                    Map<String, Object> item = new LinkedHashMap<>(tuple.asMap());
                    item.put("_similarity", res.getSimilarity());
                    retrieved.add(item);

                    if (tuple.get("bio") != null) {
                        contextText.append("- ").append(tuple.getString("name")).append(" (").append(tuple.getString("role")).append("): ").append(tuple.getString("bio")).append("\n");
                    }
                }
            }
        }

        String augmentedPrompt = "Context retrieved from SyntricDB:\n" + contextText + "\nUser Question: " + prompt;
        String generatedAnswer = "Based on SyntricDB vector context: Found " + retrieved.size() + " highly relevant profile matches. Key specialist: " + (retrieved.isEmpty() ? "None" : retrieved.get(0).get("name"));

        long elapsed = System.nanoTime() - start;
        return new RAGResult(prompt, retrieved, augmentedPrompt, generatedAnswer, elapsed / 1_000_000.0);
    }
}
