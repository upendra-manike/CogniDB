package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.ColumnDef;
import com.syntricdb.engine.schema.ColumnType;
import com.syntricdb.engine.schema.TableSchema;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.sql.QueryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionDMLTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private QueryExecutor executor;

    @BeforeEach
    public void setup() throws Exception {
        storageEngine = new StorageEngine(tempDir);
        executor = new QueryExecutor(storageEngine, new AIEngine());

        TableSchema schema = new TableSchema("users")
                .addColumn(new ColumnDef("id", ColumnType.VARCHAR, true, true))
                .addColumn(new ColumnDef("name", ColumnType.VARCHAR, false, true))
                .addColumn(new ColumnDef("role", ColumnType.VARCHAR, false, false))
                .addColumn(new ColumnDef("salary", ColumnType.DOUBLE, false, false));
        storageEngine.createTable("default", schema);

        Tuple u1 = new Tuple();
        u1.set("id", "u1");
        u1.set("name", "Alice");
        u1.set("role", "Engineer");
        u1.set("salary", 100000.0);
        storageEngine.insert("default", "users", u1);

        Tuple u2 = new Tuple();
        u2.set("id", "u2");
        u2.set("name", "Bob");
        u2.set("role", "Manager");
        u2.set("salary", 120000.0);
        storageEngine.insert("default", "users", u2);
    }

    @Test
    public void testSqlUpdate() throws Exception {
        QueryExecutor.QueryResult result = executor.execute("UPDATE users SET salary = 115000.0 WHERE id = 'u1'");
        assertTrue(result.getMessage().contains("1 rows updated"));

        Tuple updated = storageEngine.getByPrimaryKey("default", "users", "u1");
        assertNotNull(updated);
        assertEquals(115000.0, updated.getDouble("salary"));
    }

    @Test
    public void testSqlDelete() throws Exception {
        QueryExecutor.QueryResult result = executor.execute("DELETE FROM users WHERE role = 'Manager'");
        assertTrue(result.getMessage().contains("1 rows deleted"));

        List<Tuple> remaining = storageEngine.scanAll("default", "users");
        assertEquals(1, remaining.size());
        assertEquals("u1", remaining.get(0).getString("id"));
    }
}
