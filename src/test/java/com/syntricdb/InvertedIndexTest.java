package com.syntricdb;

import com.syntricdb.engine.fulltext.InvertedIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InvertedIndexTest {

    private InvertedIndex invertedIndex;

    @BeforeEach
    public void setup() {
        invertedIndex = new InvertedIndex();
    }

    @Test
    public void testInvertedIndexSearch() {
        invertedIndex.indexDocument("doc1", "Distributed databases with LSM trees and HNSW vector search.");
        invertedIndex.indexDocument("doc2", "Quantum physics research on black holes and quantum mechanics.");
        invertedIndex.indexDocument("doc3", "Java backend development with Netty network servers.");

        List<InvertedIndex.SearchResult> results = invertedIndex.search("LSM vector search", 2);

        assertEquals(1, results.size());
        assertEquals("doc1", results.get(0).getDocId());
        assertTrue(results.get(0).getScore() > 0);
    }

    @Test
    public void testInvertedIndexMultiMatch() {
        invertedIndex.indexDocument("doc1", "Java high concurrency distributed systems");
        invertedIndex.indexDocument("doc2", "Java Spring Boot microservices");

        List<InvertedIndex.SearchResult> results = invertedIndex.search("Java", 5);

        assertEquals(2, results.size());
    }
}
