package com.syntricdb.engine.lsm;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class MemTable {
    private final ConcurrentSkipListMap<String, byte[]> map = new ConcurrentSkipListMap<>();
    private final AtomicLong byteSize = new AtomicLong(0);
    private static final byte[] TOMBSTONE = new byte[0];

    public void put(String key, byte[] value) {
        byte[] prev = map.put(key, value);
        long newSize = key.getBytes().length + (value != null ? value.length : 0);
        long oldSize = prev != null ? key.getBytes().length + prev.length : 0;
        byteSize.addAndGet(newSize - oldSize);
    }

    public void delete(String key) {
        put(key, TOMBSTONE);
    }

    public byte[] get(String key) {
        return map.get(key);
    }

    public boolean isTombstone(byte[] val) {
        return val != null && val.length == 0;
    }

    public long getByteSize() {
        return byteSize.get();
    }

    public int size() {
        return map.size();
    }

    public Map<String, byte[]> getSortedData() {
        return Collections.unmodifiableMap(map);
    }

    public void clear() {
        map.clear();
        byteSize.set(0);
    }
}
