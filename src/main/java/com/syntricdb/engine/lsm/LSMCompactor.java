package com.syntricdb.engine.lsm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSMCompactor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(LSMCompactor.class);

    private final ScheduledExecutorService compactorExecutor = Executors.newSingleThreadScheduledExecutor();

    public void startCompactionTask(Path dataDir, String tableName, List<SSTable> ssTables) {
        compactorExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (ssTables.size() >= 4) {
                    log.info("Triggering LSM Compaction for table '{}' (SSTables count={})", tableName, ssTables.size());
                    compact(dataDir, tableName, ssTables);
                }
            } catch (Exception e) {
                log.error("Error during LSM compaction cycle for " + tableName, e);
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    private synchronized void compact(Path dataDir, String tableName, List<SSTable> ssTables) throws IOException {
        // Collect sorted keys across SSTables
        Map<String, byte[]> mergedData = new TreeMap<>();

        for (SSTable sstable : new ArrayList<>(ssTables)) {
            // Read keys from SSTable
            // In a real compaction, keys are merged sequentially
        }

        if (!mergedData.isEmpty()) {
            String compactId = "compact_" + System.currentTimeMillis();
            SSTable newSSTable = SSTable.create(dataDir, compactId, mergedData);
            log.info("Compaction completed for '{}'. New SSTable [{}] created.", tableName, compactId);
        }
    }

    @Override
    public void close() {
        compactorExecutor.shutdown();
    }
}
