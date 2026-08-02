package com.cognidb;

import com.cognidb.engine.cache.MemoryCacheEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryCacheEngineTest {

    private MemoryCacheEngine cache;

    @BeforeEach
    public void setup() {
        cache = new MemoryCacheEngine(3); // Capacity = 3 items
    }

    @Test
    public void testCachePutGet() {
        cache.put("key1", "val1");
        cache.put("key2", "val2");

        assertEquals("val1", cache.get("key1"));
        assertEquals("val2", cache.get("key2"));
        assertNull(cache.get("key3"));

        assertTrue(cache.getHitRate() >= 0);
    }

    @Test
    public void testCacheCapacityLimit() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        cache.put("k4", "v4"); // Triggers eviction of oldest item

        assertNotNull(cache.get("k4"));
    }
}
