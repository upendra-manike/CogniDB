package com.syntricdb.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class EmbeddingProvider {
    private final int defaultDimension;

    public EmbeddingProvider(int defaultDimension) {
        this.defaultDimension = defaultDimension;
    }

    public float[] generateEmbedding(String text) {
        return generateEmbedding(text, defaultDimension);
    }

    public float[] generateEmbedding(String text, int dim) {
        if (text == null) text = "";
        float[] vector = new float[dim];

        try {
            // High-dimensional semantic feature hashing projection
            String cleanText = text.toLowerCase().trim();
            String[] tokens = cleanText.split("\\s+");

            for (String token : tokens) {
                byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(bytes);

                for (int i = 0; i < dim; i++) {
                    byte b = hash[i % hash.length];
                    float val = ((b & 0xFF) - 128) / 128.0f;
                    vector[i] += val;
                }
            }

            // L2 normalize
            float norm = 0f;
            for (float v : vector) norm += v * v;
            norm = (float) Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < dim; i++) vector[i] /= norm;
            } else {
                for (int i = 0; i < dim; i++) vector[i] = 1.0f / (float) Math.sqrt(dim);
            }

        } catch (Exception e) {
            for (int i = 0; i < dim; i++) vector[i] = (float) Math.sin(i + text.hashCode());
        }

        return vector;
    }
}
