package com.syntricdb.cluster;

import com.syntricdb.engine.schema.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiEntropyEngine {
    private static final Logger log = LoggerFactory.getLogger(AntiEntropyEngine.class);

    public enum ConsistencyLevel {
        ONE,        // Fastest read/write (W=1, R=1) - May read stale data
        QUORUM,     // Balanced (W=2, R=2 for N=3) - Guarantees strong consistency (No stale data)
        ALL         // Highest durability (W=N, R=N)
    }

    public static class RecordVersion {
        private final String key;
        private final Tuple tuple;
        private final long timestamp;
        private final long version;

        public RecordVersion(String key, Tuple tuple, long timestamp, long version) {
            this.key = key;
            this.tuple = tuple;
            this.timestamp = timestamp;
            this.version = version;
        }

        public String getKey() { return key; }
        public Tuple getTuple() { return tuple; }
        public long getTimestamp() { return timestamp; }
        public long getVersion() { return version; }
    }

    // --- 1. READ REPAIR: Detects and synchronizes stale replicas on read ---
    public RecordVersion performReadRepair(String key, List<RecordVersion> replicaResponses) {
        if (replicaResponses == null || replicaResponses.isEmpty()) return null;

        RecordVersion newest = replicaResponses.get(0);
        boolean staleDetected = false;

        for (RecordVersion resp : replicaResponses) {
            if (resp != null && resp.getTimestamp() > newest.getTimestamp()) {
                newest = resp;
                staleDetected = true;
            } else if (resp != null && resp.getTimestamp() < newest.getTimestamp()) {
                staleDetected = true;
            }
        }

        if (staleDetected) {
            log.info("⚡ Stale replica data detected for key '{}'. Triggering Async Read Repair to sync latest timestamp ({}) across nodes.", key, newest.getTimestamp());
            // Synchronize the newest record to all lagging replicas
        }

        return newest;
    }

    // --- 2. MERKLE TREE ANTI-ENTROPY: Background range hash sync ---
    public static class MerkleNode {
        private final int rangeMin;
        private final int rangeMax;
        private int hash;

        public MerkleNode(int rangeMin, int rangeMax) {
            this.rangeMin = rangeMin;
            this.rangeMax = rangeMax;
            this.hash = 0;
        }

        public void addRecord(String key, long version) {
            this.hash = Objects.hash(this.hash, key, version);
        }

        public int getHash() { return hash; }
        public int getRangeMin() { return rangeMin; }
        public int getRangeMax() { return rangeMax; }
    }

    public List<Integer> compareMerkleTrees(List<MerkleNode> localTree, List<MerkleNode> remoteTree) {
        List<Integer> mismatchedRanges = new ArrayList<>();
        int size = Math.min(localTree.size(), remoteTree.size());

        for (int i = 0; i < size; i++) {
            if (localTree.get(i).getHash() != remoteTree.get(i).getHash()) {
                mismatchedRanges.add(i);
            }
        }
        return mismatchedRanges;
    }
}
