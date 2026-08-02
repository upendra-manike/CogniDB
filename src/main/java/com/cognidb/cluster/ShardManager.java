package com.cognidb.cluster;

import java.util.*;

public class ShardManager {
    private final SortedMap<Integer, String> ring = new TreeMap<>();
    private final int virtualNodes;

    public ShardManager(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    public void addNode(String nodeId) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hash(nodeId + "-vn-" + i);
            ring.put(hash, nodeId);
        }
    }

    public void removeNode(String nodeId) {
        for (int i = 0; i < virtualNodes; i++) {
            int hash = hash(nodeId + "-vn-" + i);
            ring.remove(hash);
        }
    }

    public String getNodeForKey(String key) {
        if (ring.isEmpty()) return null;
        int hash = hash(key);
        if (!ring.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    private int hash(String key) {
        return Math.abs(key.hashCode() * 31 + 17);
    }

    public Map<Integer, String> getRing() {
        return Collections.unmodifiableMap(ring);
    }
}
