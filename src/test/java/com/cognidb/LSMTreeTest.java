package com.cognidb;

import com.cognidb.engine.lsm.BloomFilter;
import com.cognidb.engine.lsm.LSMTree;
import com.cognidb.engine.lsm.MemTable;
import com.cognidb.engine.lsm.SSTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class LSMTreeTest {

    @TempDir
    Path tempDir;

    private LSMTree lsmTree;

    @BeforeEach
    public void setup() throws Exception {
        lsmTree = new LSMTree(tempDir, "users_test", 1024); // 1KB threshold to trigger flushes
    }

    @Test
    public void testMemTablePutGetDelete() {
        MemTable memTable = new MemTable();
        memTable.put("key_1", "value_1".getBytes());
        memTable.put("key_2", "value_2".getBytes());

        assertArrayEquals("value_1".getBytes(), memTable.get("key_1"));
        assertArrayEquals("value_2".getBytes(), memTable.get("key_2"));
        assertNull(memTable.get("key_3"));

        memTable.delete("key_1");
        byte[] tombstone = memTable.get("key_1");
        assertNotNull(tombstone);
        assertEquals(0, tombstone.length); // Tombstone indicator
    }

    @Test
    public void testBloomFilterAccuracy() {
        BloomFilter bf = new BloomFilter(100, 0.01);
        bf.add("user_100");
        bf.add("user_101");

        assertTrue(bf.mightContain("user_100"));
        assertTrue(bf.mightContain("user_101"));
        assertFalse(bf.mightContain("non_existent_key"));
    }

    @Test
    public void testSSTableCreateAndRead() throws Exception {
        Map<String, byte[]> data = new TreeMap<>();
        data.put("usr_a", "Alice Data".getBytes());
        data.put("usr_b", "Bob Data".getBytes());

        SSTable sstable = SSTable.create(tempDir, "sst_test_1", data);
        assertNotNull(sstable);

        byte[] valA = sstable.get("usr_a");
        assertNotNull(valA);
        assertEquals("Alice Data", new String(valA));

        assertNull(sstable.get("usr_c"));
    }

    @Test
    public void testLSMTreeFlushAndGet() throws Exception {
        // Insert enough data to trigger MemTable flush to SSTable
        for (int i = 0; i < 50; i++) {
            lsmTree.put("key_" + i, ("val_data_content_" + i).getBytes());
        }

        byte[] val5 = lsmTree.get("key_5");
        assertNotNull(val5);
        assertEquals("val_data_content_5", new String(val5));
    }
}
