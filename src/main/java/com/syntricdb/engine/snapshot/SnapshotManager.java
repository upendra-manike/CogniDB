package com.syntricdb.engine.snapshot;

import com.syntricdb.engine.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SnapshotManager {
    private static final Logger log = LoggerFactory.getLogger(SnapshotManager.class);

    private final Path dataDir;
    private final Path snapshotsDir;

    public SnapshotManager(Path dataDir) throws Exception {
        this.dataDir = dataDir;
        this.snapshotsDir = dataDir.resolve("snapshots");
        Files.createDirectories(snapshotsDir);
    }

    public synchronized String createSnapshot() throws Exception {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String snapshotName = "syntricdb_snapshot_" + timestamp;
        Path targetDir = snapshotsDir.resolve(snapshotName);
        Files.createDirectories(targetDir);

        log.info("Creating Point-in-Time Disaster Recovery Snapshot: {}", snapshotName);

        try (var stream = Files.walk(dataDir)) {
            stream.filter(p -> !p.startsWith(snapshotsDir) && Files.isRegularFile(p))
                  .forEach(file -> {
                      try {
                          Path relPath = dataDir.relativize(file);
                          Path destPath = targetDir.resolve(relPath);
                          Files.createDirectories(destPath.getParent());
                          Files.copy(file, destPath, StandardCopyOption.REPLACE_EXISTING);
                      } catch (Exception e) {
                          log.error("Error backing up file during snapshot", e);
                      }
                  });
        }

        log.info("✅ Snapshot created successfully at: {}", targetDir);
        return snapshotName;
    }

    public synchronized void restoreSnapshot(String snapshotName) throws Exception {
        Path snapshotDir = snapshotsDir.resolve(snapshotName);
        if (!Files.exists(snapshotDir)) {
            throw new IllegalArgumentException("Snapshot directory " + snapshotName + " does not exist.");
        }

        log.info("Restoring SyntricDB from Disaster Recovery Snapshot: {}", snapshotName);

        try (var stream = Files.walk(snapshotDir)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    Path relPath = snapshotDir.relativize(file);
                    Path destPath = dataDir.resolve(relPath);
                    Files.createDirectories(destPath.getParent());
                    Files.copy(file, destPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    log.error("Error restoring file from snapshot", e);
                }
            });
        }

        log.info("✅ Database restoration complete from snapshot: {}", snapshotName);
    }
}
