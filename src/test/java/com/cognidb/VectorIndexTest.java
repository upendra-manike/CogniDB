package com.cognidb;

import com.cognidb.ai.EmbeddingProvider;
import com.cognidb.engine.vector.DistanceMetric;
import com.cognidb.engine.vector.HNSWIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VectorIndexTest {

    private HNSWIndex hnsw;
    private EmbeddingProvider provider;

    @BeforeEach
    public void setup() {
        hnsw = new HNSWIndex(128, DistanceMetric.COSINE);
        provider = new EmbeddingProvider(128);
    }

    @Test
    public void testVectorInsertAndSearch() {
        float[] v1 = provider.generateEmbedding("Java Systems Engineer");
        float[] v2 = provider.generateEmbedding("Java Backend Developer");
        float[] v3 = provider.generateEmbedding("Quantum Physics Researcher");

        hnsw.insert("doc_1", v1);
        hnsw.insert("doc_2", v2);
        hnsw.insert("doc_3", v3);

        assertEquals(3, hnsw.size());

        float[] queryVec = provider.generateEmbedding("Java Developer");
        List<HNSWIndex.VectorSearchResult> results = hnsw.search(queryVec, 2);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getId().equals("doc_1") || results.get(0).getId().equals("doc_2"));
    }
}
