package com.cognidb.engine.lsm;

import java.util.BitSet;

public class BloomFilter {
    private final BitSet bitSet;
    private final int bitSetSize;
    private final int numHashFunctions;

    public BloomFilter(int expectedInsertions, double falsePositiveRate) {
        this.bitSetSize = (int) (-expectedInsertions * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
        this.numHashFunctions = Math.max(1, (int) Math.round((double) bitSetSize / expectedInsertions * Math.log(2)));
        this.bitSet = new BitSet(bitSetSize);
    }

    public void add(String key) {
        int[] hashes = getHashes(key);
        for (int hash : hashes) {
            bitSet.set(Math.abs(hash % bitSetSize));
        }
    }

    public boolean mightContain(String key) {
        int[] hashes = getHashes(key);
        for (int hash : hashes) {
            if (!bitSet.get(Math.abs(hash % bitSetSize))) {
                return false;
            }
        }
        return true;
    }

    private int[] getHashes(String key) {
        int[] hashes = new int[numHashFunctions];
        int hash1 = key.hashCode();
        int hash2 = (hash1 >>> 16) ^ (key.length() * 31);
        for (int i = 0; i < numHashFunctions; i++) {
            hashes[i] = hash1 + i * hash2;
        }
        return hashes;
    }
}
