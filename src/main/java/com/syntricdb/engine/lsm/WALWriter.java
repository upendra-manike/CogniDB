package com.syntricdb.engine.lsm;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WALWriter implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(WALWriter.class);
    
    private final Path walFile;
    private FileChannel channel;
    private final AtomicLong lsnCounter = new AtomicLong(0);

    public enum OpType {
        PUT((byte) 1),
        DELETE((byte) 2);

        private final byte code;
        OpType(byte code) { this.code = code; }
        public byte getCode() { return code; }
        public static OpType fromCode(byte c) {
            return c == 1 ? PUT : DELETE;
        }
    }

    public WALWriter(Path dataDir, String tableName) throws IOException {
        Files.createDirectories(dataDir);
        this.walFile = dataDir.resolve(tableName + ".wal");
        this.channel = FileChannel.open(walFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    public synchronized long appendPut(String key, byte[] valueBytes) throws IOException {
        long lsn = lsnCounter.incrementAndGet();
        byte[] keyBytes = key.getBytes();
        
        int recordLen = 8 + 1 + 4 + keyBytes.length + 4 + (valueBytes != null ? valueBytes.length : 0);
        ByteBuffer buf = ByteBuffer.allocate(recordLen);
        
        buf.putLong(lsn);
        buf.put(OpType.PUT.getCode());
        buf.putInt(keyBytes.length);
        buf.put(keyBytes);
        buf.putInt(valueBytes != null ? valueBytes.length : 0);
        if (valueBytes != null) {
            buf.put(valueBytes);
        }
        
        buf.flip();
        channel.write(buf);
        channel.force(false); // sync to disk for WAL durability
        return lsn;
    }

    public synchronized long appendDelete(String key) throws IOException {
        long lsn = lsnCounter.incrementAndGet();
        byte[] keyBytes = key.getBytes();
        
        int recordLen = 8 + 1 + 4 + keyBytes.length + 4;
        ByteBuffer buf = ByteBuffer.allocate(recordLen);
        
        buf.putLong(lsn);
        buf.put(OpType.DELETE.getCode());
        buf.putInt(keyBytes.length);
        buf.put(keyBytes);
        buf.putInt(0);
        
        buf.flip();
        channel.write(buf);
        channel.force(false);
        return lsn;
    }

    public synchronized void truncate() throws IOException {
        if (channel != null) {
            channel.close();
        }
        Files.deleteIfExists(walFile);
        this.channel = FileChannel.open(walFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    @Override
    public synchronized void close() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }
}
