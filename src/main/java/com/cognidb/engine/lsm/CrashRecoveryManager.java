package com.cognidb.engine.lsm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrashRecoveryManager {
    private static final Logger log = LoggerFactory.getLogger(CrashRecoveryManager.class);

    public static void recoverFromWAL(Path dataDir, String tableName, MemTable memTable) throws IOException {
        Path walPath = dataDir.resolve(tableName + ".wal");
        if (!Files.exists(walPath)) {
            return;
        }

        log.info("Recovering table '{}' state from WAL file: {}", tableName, walPath);
        long recoveredRecords = 0;

        try (FileChannel channel = FileChannel.open(walPath, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(8192);

            while (channel.position() < channel.size()) {
                ByteBuffer lenBuf = ByteBuffer.allocate(13); // 8 LSN + 1 OpType + 4 KeyLen
                if (channel.read(lenBuf) < 13) break;
                lenBuf.flip();

                long lsn = lenBuf.getLong();
                byte opCode = lenBuf.get();
                int keyLen = lenBuf.getInt();

                ByteBuffer keyBuf = ByteBuffer.allocate(keyLen);
                channel.read(keyBuf);
                String key = new String(keyBuf.array());

                ByteBuffer vLenBuf = ByteBuffer.allocate(4);
                channel.read(vLenBuf);
                vLenBuf.flip();
                int valLen = vLenBuf.getInt();

                byte[] valBytes = null;
                if (valLen > 0) {
                    valBytes = new byte[valLen];
                    ByteBuffer valBuf = ByteBuffer.wrap(valBytes);
                    channel.read(valBuf);
                }

                WALWriter.OpType opType = WALWriter.OpType.fromCode(opCode);
                if (opType == WALWriter.OpType.PUT) {
                    memTable.put(key, valBytes);
                } else if (opType == WALWriter.OpType.DELETE) {
                    memTable.delete(key);
                }
                recoveredRecords++;
            }
        } catch (Exception e) {
            log.warn("WAL recovery completed with partial read for {}: {}", tableName, e.getMessage());
        }

        log.info("Successfully recovered {} records from WAL for table '{}'", recoveredRecords, tableName);
    }
}
