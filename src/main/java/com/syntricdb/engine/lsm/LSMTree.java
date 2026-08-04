package com.syntricdb.engine.lsm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSMTree implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(LSMTree.class);

    private final Path dataDir;
    private final String tableName;
    private final long memTableThresholdBytes;
    
    private volatile MemTable activeMemTable;
    private final List<MemTable> immutableMemTables = new CopyOnWriteArrayList<>();
    private final List<SSTable> ssTables = new CopyOnWriteArrayList<>();
    private final WALWriter walWriter;
    private final AtomicInteger sstableCounter = new AtomicInteger(0);

    public LSMTree(Path dataDir, String tableName, long memTableThresholdBytes) throws IOException {
        this.dataDir = dataDir;
        this.tableName = tableName;
        this.memTableThresholdBytes = memTableThresholdBytes;
        this.activeMemTable = new MemTable();
        this.walWriter = new WALWriter(dataDir, tableName);
    }

    public synchronized void put(String key, byte[] value) throws IOException {
        walWriter.appendPut(key, value);
        activeMemTable.put(key, value);
        checkFlushTrigger();
    }

    public synchronized void delete(String key) throws IOException {
        walWriter.appendDelete(key);
        activeMemTable.delete(key);
        checkFlushTrigger();
    }

    public byte[] get(String key) throws IOException {
        // 1. Search Active MemTable
        byte[] val = activeMemTable.get(key);
        if (val != null) {
            return activeMemTable.isTombstone(val) ? null : val;
        }

        // 2. Search Immutable MemTables (newest to oldest)
        for (int i = immutableMemTables.size() - 1; i >= 0; i--) {
            MemTable imm = immutableMemTables.get(i);
            val = imm.get(key);
            if (val != null) {
                return imm.isTombstone(val) ? null : val;
            }
        }

        // 3. Search SSTables (newest to oldest)
        for (int i = ssTables.size() - 1; i >= 0; i--) {
            SSTable sstable = ssTables.get(i);
            val = sstable.get(key);
            if (val != null) {
                return val.length == 0 ? null : val;
            }
        }

        return null;
    }

    private void checkFlushTrigger() throws IOException {
        if (activeMemTable.getByteSize() >= memTableThresholdBytes) {
            flushMemTable();
        }
    }

    public synchronized void flushMemTable() throws IOException {
        if (activeMemTable.size() == 0) return;

        MemTable freezingMemTable = activeMemTable;
        activeMemTable = new MemTable();
        immutableMemTables.add(freezingMemTable);

        String id = System.currentTimeMillis() + "_" + sstableCounter.incrementAndGet();
        SSTable sstable = SSTable.create(dataDir, id, freezingMemTable.getSortedData());
        ssTables.add(sstable);
        immutableMemTables.remove(freezingMemTable);

        walWriter.truncate();
        log.info("Flushed MemTable to SSTable [{}] for table {}", id, tableName);
    }

    public int getSSTableCount() {
        return ssTables.size();
    }

    public long getActiveMemTableBytes() {
        return activeMemTable.getByteSize();
    }

    @Override
    public void close() throws IOException {
        walWriter.close();
    }
}
