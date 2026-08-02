package com.cognidb.ai;

import com.cognidb.engine.vector.DistanceMetric;

public class AIEngine {
    private final EmbeddingProvider embeddingProvider;

    public AIEngine() {
        this.embeddingProvider = new EmbeddingProvider(128);
    }

    public AIEngine(int defaultDimension) {
        this.embeddingProvider = new EmbeddingProvider(defaultDimension);
    }

    public float[] aiEmbed(String text) {
        return embeddingProvider.generateEmbedding(text);
    }

    public float[] aiEmbed(String text, int dim) {
        return embeddingProvider.generateEmbedding(text, dim);
    }

    public String aiSummarize(String text) {
        if (text == null || text.isBlank()) return "";
        text = text.trim();
        if (text.length() <= 100) return text;

        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length <= 2) return text;

        // Extract key summary sentences
        return sentences[0] + " " + (sentences.length > 2 ? sentences[1] : "");
    }

    public String aiClassify(String text, String[] candidateLabels) {
        if (text == null || candidateLabels == null || candidateLabels.length == 0) return "UNKNOWN";
        float[] textVec = aiEmbed(text);

        String bestLabel = candidateLabels[0];
        float maxSim = -1.0f;

        for (String label : candidateLabels) {
            float[] labelVec = aiEmbed(label);
            float sim = DistanceMetric.cosineSimilarity(textVec, labelVec);
            if (sim > maxSim) {
                maxSim = sim;
                bestLabel = label;
            }
        }

        return bestLabel;
    }

    public double aiSimilarity(String text1, String text2) {
        float[] v1 = aiEmbed(text1);
        float[] v2 = aiEmbed(text2);
        return DistanceMetric.cosineSimilarity(v1, v2);
    }

    public EmbeddingProvider getEmbeddingProvider() {
        return embeddingProvider;
    }
}
