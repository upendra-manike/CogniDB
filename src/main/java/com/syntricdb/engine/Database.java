package com.syntricdb.engine;

import com.syntricdb.engine.fulltext.InvertedIndex;
import com.syntricdb.engine.lsm.LSMTree;
import com.syntricdb.engine.schema.ColumnDef;
import com.syntricdb.engine.schema.TableSchema;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.engine.vector.DistanceMetric;
import com.syntricdb.engine.vector.HNSWIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private final String name;
    private final Path dbDataDir;
    private final Map<String, TableSchema> schemas = new ConcurrentHashMap<>();
    private final Map<String, LSMTree> lsmTrees = new ConcurrentHashMap<>();
    private final Map<String, HNSWIndex> vectorIndexes = new ConcurrentHashMap<>();
    private final Map<String, InvertedIndex> invertedIndexes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Tuple>> inMemoryTableStore = new ConcurrentHashMap<>();

    public Database(String name, Path baseDataDir) {
        this.name = name.toLowerCase();
        this.dbDataDir = baseDataDir.resolve(this.name);
    }

    public String getName() {
        return name;
    }

    public synchronized void createTable(TableSchema schema) throws IOException {
        String tableName = schema.getTableName().toLowerCase();
        schemas.put(tableName, schema);
        inMemoryTableStore.put(tableName, new ConcurrentHashMap<>());

        Path tableDataPath = dbDataDir.resolve(tableName);
        LSMTree lsm = new LSMTree(tableDataPath, tableName, 16 * 1024 * 1024);
        lsmTrees.put(tableName, lsm);

        com.syntricdb.engine.lsm.CrashRecoveryManager.recoverFromWAL(tableDataPath, tableName, new com.syntricdb.engine.lsm.MemTable());

        String vectorCol = schema.getVectorColumn();
        if (vectorCol != null) {
            ColumnDef def = schema.getColumn(vectorCol);
            int dim = def.getVectorDimension() > 0 ? def.getVectorDimension() : 128;
            HNSWIndex hnsw = new HNSWIndex(dim, DistanceMetric.COSINE);
            vectorIndexes.put(tableName + "." + vectorCol.toLowerCase(), hnsw);
            log.info("Initialized HNSW Vector Index for {}.{}.{} (Dimension={})", name, tableName, vectorCol, dim);
        }

        InvertedIndex invIdx = new InvertedIndex();
        invertedIndexes.put(tableName, invIdx);

        log.info("Table '{}.{}' created successfully.", name, tableName);
    }

    public void dropTable(String tableName) {
        tableName = tableName.toLowerCase();
        schemas.remove(tableName);
        inMemoryTableStore.remove(tableName);
        LSMTree lsm = lsmTrees.remove(tableName);
        if (lsm != null) {
            try { lsm.close(); } catch (Exception ignored) {}
        }
        invertedIndexes.remove(tableName);
        String prefix = tableName + ".";
        vectorIndexes.keySet().removeIf(k -> k.startsWith(prefix));
        log.info("Table '{}.{}' dropped.", name, tableName);
    }

    public Map<String, TableSchema> getSchemas() {
        return Collections.unmodifiableMap(schemas);
    }

    public TableSchema getSchema(String tableName) {
        return schemas.get(tableName.toLowerCase());
    }

    public Map<String, Map<String, Tuple>> getInMemoryTableStore() {
        return inMemoryTableStore;
    }

    public Map<String, LSMTree> getLsmTrees() {
        return lsmTrees;
    }

    public Map<String, HNSWIndex> getVectorIndexes() {
        return vectorIndexes;
    }

    public Map<String, InvertedIndex> getInvertedIndexes() {
        return invertedIndexes;
    }

    public HNSWIndex getVectorIndex(String tableName, String columnName) {
        return vectorIndexes.get(tableName.toLowerCase() + "." + columnName.toLowerCase());
    }

    public InvertedIndex getInvertedIndex(String tableName) {
        return invertedIndexes.get(tableName.toLowerCase());
    }

    public void close() {
        for (LSMTree lsm : lsmTrees.values()) {
            try { lsm.close(); } catch (Exception ignored) {}
        }
    }
}
