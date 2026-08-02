package com.cognidb;

import com.cognidb.cluster.AntiEntropyEngine;
import com.cognidb.engine.schema.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AntiEntropyTest {

    private AntiEntropyEngine antiEntropy;

    @BeforeEach
    public void setup() {
        antiEntropy = new AntiEntropyEngine();
    }

    @Test
    public void testReadRepairResolvesLatestTimestamp() {
        Tuple t1 = new Tuple(); t1.set("val", "stale_data_v1");
        Tuple t2 = new Tuple(); t2.set("val", "latest_data_v2");

        AntiEntropyEngine.RecordVersion replica1 = new AntiEntropyEngine.RecordVersion("usr_100", t1, 1000L, 1);
        AntiEntropyEngine.RecordVersion replica2 = new AntiEntropyEngine.RecordVersion("usr_100", t2, 2000L, 2);

        List<AntiEntropyEngine.RecordVersion> responses = List.of(replica1, replica2);
        AntiEntropyEngine.RecordVersion resolved = antiEntropy.performReadRepair("usr_100", responses);

        assertNotNull(resolved);
        assertEquals(2000L, resolved.getTimestamp());
        assertEquals("latest_data_v2", resolved.getTuple().get("val"));
    }

    @Test
    public void testMerkleTreeAntiEntropyMismatch() {
        List<AntiEntropyEngine.MerkleNode> nodeA = new ArrayList<>();
        List<AntiEntropyEngine.MerkleNode> nodeB = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            AntiEntropyEngine.MerkleNode mA = new AntiEntropyEngine.MerkleNode(i * 100, (i + 1) * 100);
            AntiEntropyEngine.MerkleNode mB = new AntiEntropyEngine.MerkleNode(i * 100, (i + 1) * 100);

            mA.addRecord("key_" + i, 10L);
            mB.addRecord("key_" + i, i == 2 ? 11L : 10L); // Introduce mismatch in range #2

            nodeA.add(mA);
            nodeB.add(mB);
        }

        List<Integer> mismatches = antiEntropy.compareMerkleTrees(nodeA, nodeB);
        assertEquals(1, mismatches.size());
        assertEquals(2, mismatches.get(0)); // Range 2 identified as stale/mismatched
    }
}
