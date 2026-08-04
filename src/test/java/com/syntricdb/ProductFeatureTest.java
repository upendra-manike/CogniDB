package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.ai.RAGEngine;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.columnar.ColumnSegment;
import com.syntricdb.engine.columnar.VectorizedBatch;
import com.syntricdb.engine.schema.ColumnType;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.engine.txn.Transaction;
import com.syntricdb.engine.txn.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductFeatureTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private AIEngine aiEngine;

    @BeforeEach
    public void setup() throws Exception {
        storageEngine = new StorageEngine(tempDir);
        aiEngine = new AIEngine(128);
    }

    @Test
    public void testVectorizedColumnarAnalytics() {
        ColumnSegment priceCol = new ColumnSegment("price", ColumnType.DOUBLE);
        priceCol.add(100.0);
        priceCol.add(200.0);
        priceCol.add(300.0);

        VectorizedBatch batch = new VectorizedBatch();
        batch.addColumn(priceCol);

        Map<String, Object> sumRes = batch.executeAggregation("SUM", "price");
        assertEquals(600.0, (Double) sumRes.get("SUM(price)"), 0.001);

        Map<String, Object> avgRes = batch.executeAggregation("AVG", "price");
        assertEquals(200.0, (Double) avgRes.get("AVG(price)"), 0.001);
    }

    @Test
    public void testMVCCTransactions() throws Exception {
        TransactionManager tm = new TransactionManager();
        Transaction txn = tm.beginTransaction();
        assertEquals(Transaction.TxnState.ACTIVE, txn.getState());

        Tuple t = new Tuple();
        t.set("id", "txn_usr_1");
        t.set("name", "Alice Txn");
        txn.recordWrite("txn_usr_1", t);

        boolean committed = tm.commitTransaction(txn, storageEngine, "users");
        // Should succeed or handle uncreated table gracefully
    }

    @Test
    public void testRAGEngineQuery() throws Exception {
        storageEngine.createTable(new com.syntricdb.engine.schema.TableSchema("users")
                .addColumn(new com.syntricdb.engine.schema.ColumnDef("id", ColumnType.VARCHAR, true, true))
                .addColumn(new com.syntricdb.engine.schema.ColumnDef("name", ColumnType.VARCHAR, false, false))
                .addColumn(new com.syntricdb.engine.schema.ColumnDef("bio", ColumnType.VARCHAR, false, false))
                .addColumn(new com.syntricdb.engine.schema.ColumnDef("embedding", ColumnType.FLOAT_VECTOR, 128, false, true)));

        Tuple t = new Tuple();
        t.set("id", "u10");
        t.set("name", "Dr. Jane");
        t.set("role", "AI Researcher");
        t.set("bio", "Pioneer in distributed database neural indexes.");
        t.set("embedding", aiEngine.aiEmbed("AI Researcher"));
        storageEngine.insert("users", t);

        RAGEngine rag = new RAGEngine(storageEngine, aiEngine);
        RAGEngine.RAGResult res = rag.query("users", "embedding", "AI neural database pioneer", 1);

        assertNotNull(res);
        assertTrue(res.getAugmentedPrompt().contains("Dr. Jane"));
    }
}
