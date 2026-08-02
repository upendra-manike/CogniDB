package com.cognidb;

import com.cognidb.ai.AIEngine;
import com.cognidb.engine.StorageEngine;
import com.cognidb.sql.QueryExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SQLQueryTest {

    @TempDir
    Path tempDir;

    private QueryExecutor executor;

    @BeforeEach
    public void setup() throws Exception {
        StorageEngine engine = new StorageEngine(tempDir);
        AIEngine ai = new AIEngine(128);
        executor = new QueryExecutor(engine, ai);

        executor.execute("CREATE TABLE users (id VARCHAR PRIMARY KEY, name VARCHAR, city VARCHAR, age INT, bio VARCHAR, embedding FLOAT_VECTOR(128))");
    }

    @Test
    public void testSqlInsertAndSelect() throws Exception {
        executor.execute("INSERT INTO users VALUES ('u1', 'Alice', 'Hyderabad', 30, 'Principal Java & AI Engineer', AI_EMBED('Java Engineer'))");

        QueryExecutor.QueryResult result = executor.execute("SELECT id, name, city FROM users WHERE id='u1'");

        assertEquals(1, result.getRows().size());
        assertEquals("Alice", result.getRows().get(0).get("name"));
        assertEquals("Hyderabad", result.getRows().get(0).get("city"));
    }

    @Test
    public void testVectorSimilarityQuery() throws Exception {
        executor.execute("INSERT INTO users VALUES ('u1', 'Alice', 'Hyderabad', 30, 'Java Tech Lead', AI_EMBED('Java Tech Lead'))");
        executor.execute("INSERT INTO users VALUES ('u2', 'Bob', 'London', 40, 'Quantum Physicist', AI_EMBED('Quantum Physicist'))");

        QueryExecutor.QueryResult result = executor.execute("SELECT id, name FROM users WHERE embedding SIMILAR TO 'Java Developer' TOP 1");

        assertEquals(1, result.getRows().size());
        assertEquals("u1", result.getRows().get(0).get("id"));
        assertNotNull(result.getExecutionPlan());
        assertEquals("INDEX_VECTOR_HNSW", result.getExecutionPlan().getStrategy().name());
    }
}
