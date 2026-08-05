package com.syntricdb.engine.fulltext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InvertedIndex {
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> termFrequencies = new ConcurrentHashMap<>();
    private final Map<String, Integer> documentLengths = new ConcurrentHashMap<>();
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "in", "on", "at", "is", "are", "and", "or", "to", "for", "with");

    public void indexDocument(String docId, String text) {
        if (text == null) return;
        List<String> tokens = tokenize(text);
        documentLengths.put(docId, tokens.size());

        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.put(token, counts.getOrDefault(token, 0) + 1);
            index.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet()).add(docId);
        }
        termFrequencies.put(docId, counts);
    }

    public List<SearchResult> search(String query, int limit) {
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return Collections.emptyList();

        Map<String, Double> scores = new HashMap<>();
        int totalDocs = Math.max(1, documentLengths.size());

        for (String token : queryTokens) {
            Set<String> postingList = index.get(token);
            if (postingList == null) continue;

            double idf = Math.log(1.0 + (totalDocs - postingList.size() + 0.5) / (postingList.size() + 0.5));

            for (String docId : postingList) {
                int tf = termFrequencies.getOrDefault(docId, Collections.emptyMap()).getOrDefault(token, 0);
                double score = tf * idf;
                scores.put(docId, scores.getOrDefault(docId, 0.0) + score);
            }
        }

        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            results.add(new SearchResult(entry.getKey(), entry.getValue()));
        }
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());

        return results.subList(0, Math.min(limit, results.size()));
    }

    public void removeDocument(String docId) {
        documentLengths.remove(docId);
        termFrequencies.remove(docId);
        for (Set<String> docs : index.values()) {
            docs.remove(docId);
        }
    }

    public synchronized void saveToFile(java.nio.file.Path file) throws java.io.IOException {
        if (file.getParent() != null) {
            java.nio.file.Files.createDirectories(file.getParent());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("documentLengths", documentLengths);
        data.put("termFrequencies", termFrequencies);
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(file.toFile(), data);
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadFromFile(java.nio.file.Path file) throws java.io.IOException {
        if (!java.nio.file.Files.exists(file)) return;
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> data = mapper.readValue(file.toFile(), Map.class);
        Map<String, Integer> lengths = (Map<String, Integer>) data.get("documentLengths");
        if (lengths != null) documentLengths.putAll(lengths);
        Map<String, Map<String, Integer>> termFreqs = (Map<String, Map<String, Integer>>) data.get("termFrequencies");
        if (termFreqs != null) {
            for (Map.Entry<String, Map<String, Integer>> entry : termFreqs.entrySet()) {
                String docId = entry.getKey();
                Map<String, Integer> counts = entry.getValue();
                termFrequencies.put(docId, counts);
                for (String token : counts.keySet()) {
                    index.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet()).add(docId);
                }
            }
        }
    }


    private List<String> tokenize(String text) {
        String[] raw = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String r : raw) {
            if (!r.isBlank() && !STOP_WORDS.contains(r)) {
                tokens.add(r);
            }
        }
        return tokens;
    }

    public static class SearchResult {
        private final String docId;
        private final double score;

        public SearchResult(String docId, double score) {
            this.docId = docId;
            this.score = score;
        }

        public String getDocId() { return docId; }
        public double getScore() { return score; }
    }
}
