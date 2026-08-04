package com.syntricdb;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.sql.AST;
import com.syntricdb.sql.SQLParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLParserTest {

    private SQLParser parser;

    @BeforeEach
    public void setup() {
        AIEngine aiEngine = new AIEngine(128);
        parser = new SQLParser(aiEngine);
    }

    @Test
    public void testParseCreateTable() throws Exception {
        AST.Statement stmt = parser.parse("CREATE TABLE products (id VARCHAR PRIMARY KEY, title VARCHAR, price DOUBLE)");
        assertTrue(stmt instanceof AST.CreateTableStatement);

        AST.CreateTableStatement create = (AST.CreateTableStatement) stmt;
        assertEquals("products", create.getTableName());
        assertEquals(3, create.getColumns().size());
    }

    @Test
    public void testParseVectorSearchSelect() throws Exception {
        AST.Statement stmt = parser.parse("SELECT id, name FROM users WHERE embedding SIMILAR TO 'Java Systems' TOP 5");
        assertTrue(stmt instanceof AST.SelectStatement);

        AST.SelectStatement select = (AST.SelectStatement) stmt;
        assertEquals("users", select.getTableName());
        assertNotNull(select.getVectorSearchCondition());
        assertEquals("embedding", select.getVectorSearchCondition().getVectorColumn());
        assertEquals("Java Systems", select.getVectorSearchCondition().getQueryText());
        assertEquals(5, select.getVectorSearchCondition().getK());
    }

    @Test
    public void testParseFullTextMatchSelect() throws Exception {
        AST.Statement stmt = parser.parse("SELECT id, bio FROM users WHERE MATCH(bio, 'vector search')");
        assertTrue(stmt instanceof AST.SelectStatement);

        AST.SelectStatement select = (AST.SelectStatement) stmt;
        assertNotNull(select.getFullTextCondition());
        assertEquals("bio", select.getFullTextCondition().getColumn());
        assertEquals("vector search", select.getFullTextCondition().getQueryText());
    }
}
