package com.syntricdb.engine.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryCacheEngine {
    private final int maxCapacity;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public static class CacheEntry {
        private final Object value;
        private final long createdTime;
        private volatile long lastAccessed;
        private final AtomicLong accessCount = new AtomicLong(1);

        public CacheEntry(Object value) {
            this.value = value;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessed = this.createdTime;
        }

        public Object getValue() {
            this.lastAccessed = System.currentTimeMillis();
            this.accessCount.incrementAndGet();
            return value;
        }
    }

    public MemoryCacheEngine(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void put(String key, Object value) {
        if (cache.size() >= maxCapacity && !cache.containsKey(key)) {
            evictOne();
        }
        cache.put(key, new CacheEntry(value));
    }

    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            hits.incrementAndGet();
            return entry.getValue();
        }
        misses.incrementAndGet();
        return null;
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    private void evictOne() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastAccessed < oldestTime) {
                oldestTime = entry.getValue().lastAccessed;
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }
    public int size() { return cache.size(); }
    public double getHitRate() {
        long h = hits.get();
        long total = h + misses.get();
        return total == 0 ? 0.0 : (double) h / total;
    }
}
