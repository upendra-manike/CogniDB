package com.syntricdb.engine.vector;

public enum DistanceMetric {
    COSINE,
    EUCLIDEAN,
    DOT_PRODUCT;

    public static float cosineDistance(float[] v1, float[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException("Vector length mismatch");
        float dot = 0f, norm1 = 0f, norm2 = 0f;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0f || norm2 == 0f) return 1f;
        float similarity = dot / ((float) Math.sqrt(norm1) * (float) Math.sqrt(norm2));
        return 1f - Math.max(-1f, Math.min(1f, similarity)); // Distance range [0, 2]
    }

    public static float cosineSimilarity(float[] v1, float[] v2) {
        return 1f - cosineDistance(v1, v2);
    }

    public static float euclideanDistance(float[] v1, float[] v2) {
        if (v1.length != v2.length) throw new IllegalArgumentException("Vector length mismatch");
        float sum = 0f;
        for (int i = 0; i < v1.length; i++) {
            float diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }

    public static float compute(DistanceMetric metric, float[] v1, float[] v2) {
        switch (metric) {
            case EUCLIDEAN:
                return euclideanDistance(v1, v2);
            case DOT_PRODUCT:
                float dot = 0f;
                for (int i = 0; i < v1.length; i++) dot += v1[i] * v2[i];
                return -dot; // lower is closer
            case COSINE:
            default:
                return cosineDistance(v1, v2);
        }
    }
}
