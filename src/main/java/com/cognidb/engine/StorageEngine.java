package com.cognidb.engine;

import com.cognidb.engine.cache.MemoryCacheEngine;
import com.cognidb.engine.fulltext.InvertedIndex;
import com.cognidb.engine.lsm.LSMTree;
import com.cognidb.engine.schema.*;
import com.cognidb.engine.stream.StreamEngine;
import com.cognidb.engine.vector.DistanceMetric;
import com.cognidb.engine.vector.HNSWIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StorageEngine implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(StorageEngine.class);

    private final Path baseDataDir;
    private final Map<String, TableSchema> schemas = new ConcurrentHashMap<>();
    private final Map<String, LSMTree> lsmTrees = new ConcurrentHashMap<>();
    private final Map<String, HNSWIndex> vectorIndexes = new ConcurrentHashMap<>();
    private final Map<String, InvertedIndex> invertedIndexes = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Tuple>> inMemoryTableStore = new ConcurrentHashMap<>();

    private final MemoryCacheEngine cacheEngine;
    private final StreamEngine streamEngine;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final AtomicLong writeOpsCount = new AtomicLong(0);
    private final AtomicLong readOpsCount = new AtomicLong(0);

    public StorageEngine(Path baseDataDir) {
        this.baseDataDir = baseDataDir;
        this.cacheEngine = new MemoryCacheEngine(50000);
        this.streamEngine = new StreamEngine();
    }

    public synchronized void createTable(TableSchema schema) throws IOException {
        String tableName = schema.getTableName();
        schemas.put(tableName, schema);
        inMemoryTableStore.put(tableName, new ConcurrentHashMap<>());

        // LSM Tree initialization (16MB MemTable limit per table)
        LSMTree lsm = new LSMTree(baseDataDir.resolve("data"), tableName, 16 * 1024 * 1024);
        lsmTrees.put(tableName, lsm);

        // Crash Recovery: Replay WAL log into MemTable
        com.cognidb.engine.lsm.CrashRecoveryManager.recoverFromWAL(baseDataDir.resolve("data"), tableName, new com.cognidb.engine.lsm.MemTable());

        // Vector Index initialization if vector column exists
        String vectorCol = schema.getVectorColumn();
        if (vectorCol != null) {
            ColumnDef def = schema.getColumn(vectorCol);
            int dim = def.getVectorDimension() > 0 ? def.getVectorDimension() : 128;
            HNSWIndex hnsw = new HNSWIndex(dim, DistanceMetric.COSINE);
            vectorIndexes.put(tableName + "." + vectorCol, hnsw);
            log.info("Initialized HNSW Vector Index for {}.{} (Dimension={})", tableName, vectorCol, dim);
        }

        // Inverted index initialization for text columns
        InvertedIndex invIdx = new InvertedIndex();
        invertedIndexes.put(tableName, invIdx);

        log.info("Table '{}' created successfully with unified CogniDB engine.", tableName);
    }

    public void insert(String tableName, Tuple tuple) throws IOException {
        tableName = tableName.toLowerCase();
        TableSchema schema = schemas.get(tableName);
        if (schema == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' does not exist.");
        }

        String pkCol = schema.getPrimaryKeyColumn();
        if (pkCol == null) {
            throw new IllegalArgumentException("Table '" + tableName + "' must specify a primary key.");
        }

        Object pkVal = tuple.get(pkCol);
        if (pkVal == null) {
            throw new IllegalArgumentException("Primary key value for '" + pkCol + "' cannot be null.");
        }

        String keyStr = pkVal.toString();
        byte[] serializedBytes = jsonMapper.writeValueAsBytes(tuple.asMap());

        // 1. Write to LSM Tree (WAL + MemTable + SSTable)
        LSMTree lsm = lsmTrees.get(tableName);
        if (lsm != null) {
            lsm.put(keyStr, serializedBytes);
        }

        // 2. Write to in-memory store for instant zero-latency query execution
        inMemoryTableStore.get(tableName).put(keyStr, tuple);

        // 3. Index vector columns in HNSW index
        String vectorCol = schema.getVectorColumn();
        if (vectorCol != null) {
            float[] vec = tuple.getVector(vectorCol);
            if (vec != null) {
                HNSWIndex hnsw = vectorIndexes.get(tableName + "." + vectorCol);
                if (hnsw != null) {
                    hnsw.insert(keyStr, vec);
                }
            }
        }

        // 4. Index text columns in Inverted Index
        InvertedIndex invIdx = invertedIndexes.get(tableName);
        if (invIdx != null) {
            StringBuilder textAcc = new StringBuilder();
            for (ColumnDef col : schema.getColumnList()) {
                if (col.getType() == ColumnType.VARCHAR) {
                    String text = tuple.getString(col.getName());
                    if (text != null) textAcc.append(" ").append(text);
                }
            }
            if (textAcc.length() > 0) {
                invIdx.indexDocument(keyStr, textAcc.toString());
            }
        }

        // 5. Invalidate hot cache & record metric
        cacheEngine.invalidate(tableName + ":" + keyStr);
        writeOpsCount.incrementAndGet();

        // 6. Stream notification trigger
        Map<String, Object> streamEvent = new HashMap<>(tuple.asMap());
        streamEvent.put("_table", tableName);
        streamEvent.put("_op", "INSERT");
        streamEngine.publish("table_" + tableName, streamEvent);
    }

    public Tuple getByPrimaryKey(String tableName, String primaryKey) throws IOException {
        tableName = tableName.toLowerCase();
        readOpsCount.incrementAndGet();

        // Cache check
        String cacheKey = tableName + ":" + primaryKey;
        Object cached = cacheEngine.get(cacheKey);
        if (cached instanceof Tuple) {
            return (Tuple) cached;
        }

        // Memory Store check
        Map<String, Tuple> store = inMemoryTableStore.get(tableName);
        if (store != null && store.containsKey(primaryKey)) {
            Tuple tuple = store.get(primaryKey);
            cacheEngine.put(cacheKey, tuple);
            return tuple;
        }

        // LSM Tree search
        LSMTree lsm = lsmTrees.get(tableName);
        if (lsm != null) {
            byte[] bytes = lsm.get(primaryKey);
            if (bytes != null) {
                Map<String, Object> map = jsonMapper.readValue(bytes, Map.class);
                Tuple tuple = new Tuple(map);
                cacheEngine.put(cacheKey, tuple);
                return tuple;
            }
        }

        return null;
    }

    public List<Tuple> scanAll(String tableName) {
        tableName = tableName.toLowerCase();
        readOpsCount.incrementAndGet();
        Map<String, Tuple> store = inMemoryTableStore.get(tableName);
        if (store == null) return Collections.emptyList();
        return new ArrayList<>(store.values());
    }

    public TableSchema getSchema(String tableName) {
        return schemas.get(tableName.toLowerCase());
    }

    public Map<String, TableSchema> getAllSchemas() {
        return Collections.unmodifiableMap(schemas);
    }

    public HNSWIndex getVectorIndex(String tableName, String columnName) {
        return vectorIndexes.get(tableName.toLowerCase() + "." + columnName.toLowerCase());
    }

    public InvertedIndex getInvertedIndex(String tableName) {
        return invertedIndexes.get(tableName.toLowerCase());
    }

    public MemoryCacheEngine getCacheEngine() {
        return cacheEngine;
    }

    public StreamEngine getStreamEngine() {
        return streamEngine;
    }

    public long getWriteOpsCount() { return writeOpsCount.get(); }
    public long getReadOpsCount() { return readOpsCount.get(); }

    @Override
    public void close() throws Exception {
        for (LSMTree lsm : lsmTrees.values()) {
            lsm.close();
        }
    }
}
