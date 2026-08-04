package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.sql.QueryExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MultiDatabaseTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;
    private AIEngine aiEngine;
    private QueryExecutor queryExecutor;

    @BeforeEach
    public void setUp() {
        storageEngine = new StorageEngine(tempDir);
        aiEngine = new AIEngine(128);
        queryExecutor = new QueryExecutor(storageEngine, aiEngine);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (storageEngine != null) {
            storageEngine.close();
        }
    }

    @Test
    public void testDatabaseLifecycleAndIsolation() throws Exception {
        // 1. Verify default database exists
        assertTrue(storageEngine.listDatabases().contains("default"));

        // 2. Create new database via StorageEngine
        storageEngine.createDatabase("analytics");
        assertTrue(storageEngine.listDatabases().contains("analytics"));

        // 3. Execute CREATE DATABASE via SQL
        QueryExecutor.QueryResult createRes = queryExecutor.execute("CREATE DATABASE production");
        assertTrue(createRes.getMessage().contains("created successfully"));
        assertTrue(storageEngine.listDatabases().contains("production"));

        // 4. Test SHOW DATABASES
        QueryExecutor.QueryResult showDbsRes = queryExecutor.execute("SHOW DATABASES");
        List<Map<String, Object>> dbRows = showDbsRes.getRows();
        assertTrue(dbRows.size() >= 3);

        // 5. Test USE database
        QueryExecutor.QueryResult useRes = queryExecutor.execute("USE analytics");
        assertEquals("analytics", queryExecutor.getActiveDatabase());

        // 6. Create table in analytics database
        queryExecutor.execute("CREATE TABLE analytics.metrics (id VARCHAR PRIMARY KEY, val INT)");
        queryExecutor.execute("INSERT INTO analytics.metrics VALUES ('m1', 100)");

        // 7. Create table with same name in default database
        queryExecutor.execute("CREATE TABLE default.metrics (id VARCHAR PRIMARY KEY, val INT)");
        queryExecutor.execute("INSERT INTO default.metrics VALUES ('m1', 999)");

        // 8. Query analytics.metrics and default.metrics to verify table isolation
        QueryExecutor.QueryResult resAnalytics = queryExecutor.execute("SELECT * FROM analytics.metrics");
        assertEquals(1, resAnalytics.getRows().size());
        assertEquals("100", resAnalytics.getRows().get(0).get("val").toString());

        QueryExecutor.QueryResult resDefault = queryExecutor.execute("SELECT * FROM default.metrics");
        assertEquals(1, resDefault.getRows().size());
        assertEquals("999", resDefault.getRows().get(0).get("val").toString());

        // 9. Test SHOW TABLES IN analytics
        QueryExecutor.QueryResult showTablesRes = queryExecutor.execute("SHOW TABLES FROM analytics");
        assertEquals(1, showTablesRes.getRows().size());
        assertEquals("metrics", showTablesRes.getRows().get(0).get("Table"));
    }

    @Test
    public void testDropDatabase() throws Exception {
        storageEngine.createDatabase("temp_db");
        assertTrue(storageEngine.listDatabases().contains("temp_db"));

        queryExecutor.execute("DROP DATABASE temp_db");
        assertFalse(storageEngine.listDatabases().contains("temp_db"));
    }
}
