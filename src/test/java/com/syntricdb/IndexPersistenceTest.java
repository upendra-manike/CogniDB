package com.syntricdb;

import com.syntricdb.engine.fulltext.InvertedIndex;
import com.syntricdb.engine.vector.DistanceMetric;
import com.syntricdb.engine.vector.HNSWIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IndexPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testHNSWIndexPersistence() throws Exception {
        Path file = tempDir.resolve("vector.json");

        HNSWIndex index = new HNSWIndex(3, DistanceMetric.COSINE);
        index.insert("v1", new float[]{1.0f, 0.0f, 0.0f});
        index.insert("v2", new float[]{0.0f, 1.0f, 0.0f});
        assertEquals(2, index.size());

        index.saveToFile(file);

        HNSWIndex rehydrated = new HNSWIndex(3, DistanceMetric.COSINE);
        rehydrated.loadFromFile(file);
        assertEquals(2, rehydrated.size());

        List<HNSWIndex.VectorSearchResult> res = rehydrated.search(new float[]{0.9f, 0.1f, 0.0f}, 1);
        assertFalse(res.isEmpty());
        assertEquals("v1", res.get(0).getId());
    }

    @Test
    public void testInvertedIndexPersistence() throws Exception {
        Path file = tempDir.resolve("inverted.json");

        InvertedIndex index = new InvertedIndex();
        index.indexDocument("doc1", "syntricdb high performance vector database and vector search engine");
        index.indexDocument("doc2", "relational database with sql engine");

        index.saveToFile(file);

        InvertedIndex rehydrated = new InvertedIndex();
        rehydrated.loadFromFile(file);

        List<InvertedIndex.SearchResult> searchResult = rehydrated.search("vector engine", 5);
        assertFalse(searchResult.isEmpty());
        assertEquals("doc1", searchResult.get(0).getDocId());
    }
}
