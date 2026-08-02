package com.cognidb.sql;

import com.cognidb.ai.AIEngine;
import com.cognidb.engine.schema.ColumnDef;
import com.cognidb.engine.schema.ColumnType;
import com.cognidb.engine.schema.Tuple;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLParser {
    private final AIEngine aiEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SQLParser(AIEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    public AST.Statement parse(String sql) throws Exception {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query cannot be empty.");
        }

        String trimmed = sql.trim().replaceAll(";$", "");
        String uppercase = trimmed.toUpperCase();

        if (uppercase.startsWith("CREATE TABLE")) {
            return parseCreateTable(trimmed);
        } else if (uppercase.startsWith("INSERT INTO")) {
            return parseInsert(trimmed);
        } else if (uppercase.startsWith("SELECT")) {
            return parseSelect(trimmed);
        } else if (uppercase.startsWith("PUBLISH INTO")) {
            return parseStreamPublish(trimmed);
        }

        throw new IllegalArgumentException("Unsupported SQL statement syntax: " + trimmed);
    }

    private AST.CreateTableStatement parseCreateTable(String sql) {
        // Syntax: CREATE TABLE users (id VARCHAR PRIMARY KEY, age INT, bio VARCHAR, embedding FLOAT_VECTOR(128))
        Pattern p = Pattern.compile("CREATE\\s+TABLE\\s+([a-zA-Z0-9_]+)\\s*\\((.*)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid CREATE TABLE syntax.");
        }

        String tableName = m.group(1).trim();
        String colsBody = m.group(2).trim();

        AST.CreateTableStatement stmt = new AST.CreateTableStatement(tableName);
        String[] colDefs = colsBody.split(",");

        for (String colDefStr : colDefs) {
            colDefStr = colDefStr.trim();
            String[] parts = colDefStr.split("\\s+");
            if (parts.length < 2) continue;

            String colName = parts[0];
            String typeStr = parts[1].toUpperCase();
            boolean isPk = colDefStr.toUpperCase().contains("PRIMARY KEY");
            boolean isIndexed = colDefStr.toUpperCase().contains("INDEX") || isPk;

            ColumnType type = ColumnType.VARCHAR;
            int vectorDim = 128;

            if (typeStr.startsWith("INT")) {
                type = ColumnType.INT;
            } else if (typeStr.startsWith("BIGINT")) {
                type = ColumnType.BIGINT;
            } else if (typeStr.startsWith("DOUBLE") || typeStr.startsWith("FLOAT")) {
                type = ColumnType.DOUBLE;
            } else if (typeStr.startsWith("BOOLEAN")) {
                type = ColumnType.BOOLEAN;
            } else if (typeStr.startsWith("FLOAT_VECTOR") || typeStr.startsWith("VECTOR")) {
                type = ColumnType.FLOAT_VECTOR;
                Pattern dimPattern = Pattern.compile("\\((\\d+)\\)");
                Matcher dimM = dimPattern.matcher(typeStr);
                if (dimM.find()) {
                    vectorDim = Integer.parseInt(dimM.group(1));
                }
            }

            stmt.addColumn(new ColumnDef(colName, type, vectorDim, isPk, isIndexed));
        }

        return stmt;
    }

    private AST.InsertStatement parseInsert(String sql) throws Exception {
        // Syntax: INSERT INTO users VALUES ('u1', 32, 'Java Tech Lead', AI_EMBED('Java Tech Lead'))
        // or JSON-style: INSERT INTO users VALUES {"id":"u1", "age":32, "bio":"Java Tech Lead"}
        Pattern p = Pattern.compile("INSERT\\s+INTO\\s+([a-zA-Z0-9_]+)\\s+VALUES\\s*(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid INSERT INTO syntax.");
        }

        String tableName = m.group(1).trim();
        String valuesBody = m.group(2).trim();

        Tuple tuple = new Tuple();

        if (valuesBody.startsWith("{")) {
            Map<String, Object> map = objectMapper.readValue(valuesBody, Map.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof String && entry.getValue().toString().startsWith("AI_EMBED(")) {
                    String text = extractAiEmbedArg(entry.getValue().toString());
                    tuple.set(entry.getKey(), aiEngine.aiEmbed(text));
                } else {
                    tuple.set(entry.getKey(), entry.getValue());
                }
            }
        } else {
            // Expression values list parser
            if (valuesBody.startsWith("(") && valuesBody.endsWith(")")) {
                valuesBody = valuesBody.substring(1, valuesBody.length() - 1);
            }
            List<String> tokens = parseCSVValues(valuesBody);
            // Will map positions or named parameters in executor
            for (int i = 0; i < tokens.size(); i++) {
                String valStr = tokens.get(i).trim();
                if (valStr.toUpperCase().startsWith("AI_EMBED(")) {
                    String text = extractAiEmbedArg(valStr);
                    tuple.set("val_" + i, aiEngine.aiEmbed(text));
                } else {
                    tuple.set("val_" + i, unquote(valStr));
                }
            }
        }

        return new AST.InsertStatement(tableName, tuple);
    }

    private AST.SelectStatement parseSelect(String sql) {
        // Syntax: SELECT id, name, AI_SUMMARIZE(bio) FROM users WHERE embedding SIMILAR TO 'Java Engineer' AND city='Hyderabad' AND age>30 LIMIT 10
        Pattern p = Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+([a-zA-Z0-9_]+)(?:\\s+WHERE\\s+(.*?))?(?:\\s+ORDER\\s+BY\\s+([a-zA-Z0-9_]+)(?:\\s+(ASC|DESC))?)?(?:\\s+LIMIT\\s+(\\d+))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid SELECT query syntax.");
        }

        String selectBody = m.group(1).trim();
        String tableName = m.group(2).trim();
        String whereBody = m.group(3) != null ? m.group(3).trim() : null;
        String orderByCol = m.group(4) != null ? m.group(4).trim() : null;
        String orderDir = m.group(5) != null ? m.group(5).trim() : "ASC";
        String limitStr = m.group(6) != null ? m.group(6).trim() : null;

        AST.SelectStatement stmt = new AST.SelectStatement(tableName);

        // Select items
        String[] items = selectBody.split(",");
        for (String item : items) {
            item = item.trim();
            if (item.toUpperCase().startsWith("AI_SUMMARIZE(")) {
                String arg = extractFunctionArg(item);
                stmt.getSelectItems().add(new AST.SelectItem(arg, "ai_summary", "AI_SUMMARIZE", new String[]{arg}));
            } else if (item.toUpperCase().startsWith("AI_CLASSIFY(")) {
                String[] args = extractFunctionArgs(item);
                stmt.getSelectItems().add(new AST.SelectItem(args[0], "ai_class", "AI_CLASSIFY", args));
            } else {
                stmt.getSelectItems().add(new AST.SelectItem(item));
            }
        }

        // Where clauses
        if (whereBody != null && !whereBody.isBlank()) {
            String[] conds = whereBody.split("(?i)\\s+AND\\s+");
            for (String cond : conds) {
                cond = cond.trim();
                // Vector similarity check: e.g., embedding SIMILAR TO 'Java Engineer'
                if (cond.toUpperCase().contains("SIMILAR TO")) {
                    Pattern simP = Pattern.compile("([a-zA-Z0-9_]+)\\s+SIMILAR\\s+TO\\s+['\"](.*?)['\"](?:\\s+TOP\\s+(\\d+))?", Pattern.CASE_INSENSITIVE);
                    Matcher simM = simP.matcher(cond);
                    if (simM.find()) {
                        String vecCol = simM.group(1);
                        String queryText = simM.group(2);
                        int k = simM.group(3) != null ? Integer.parseInt(simM.group(3)) : 10;
                        stmt.setVectorSearchCondition(new AST.VectorSearchCondition(vecCol, queryText, aiEngine.aiEmbed(queryText), k, 1.0));
                    }
                }
                // Full text match: e.g., MATCH(bio, 'engineer')
                else if (cond.toUpperCase().startsWith("MATCH(")) {
                    String[] args = extractFunctionArgs(cond);
                    stmt.setFullTextCondition(new AST.FullTextCondition(args[0], unquote(args[1])));
                }
                // Scalar conditions: e.g., city='Hyderabad' or age>30
                else {
                    Pattern scalarP = Pattern.compile("([a-zA-Z0-9_]+)\\s*(=|!=|>|<|>=|<=)\\s*(.*)");
                    Matcher scalarM = scalarP.matcher(cond);
                    if (scalarM.find()) {
                        String col = scalarM.group(1);
                        String op = scalarM.group(2);
                        String val = unquote(scalarM.group(3));
                        stmt.getWhereConditions().add(new AST.Condition(col, op, parseLiteral(val)));
                    }
                }
            }
        }

        if (orderByCol != null) {
            stmt.setOrderByColumn(orderByCol);
            stmt.setOrderByDesc("DESC".equalsIgnoreCase(orderDir));
        }

        if (limitStr != null) {
            stmt.setLimit(Integer.parseInt(limitStr));
        }

        return stmt;
    }

    private AST.StreamPublishStatement parseStreamPublish(String sql) throws Exception {
        Pattern p = Pattern.compile("PUBLISH\\s+INTO\\s+([a-zA-Z0-9_]+)\\s+VALUES\\s*(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sql);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid PUBLISH INTO syntax.");
        }
        String topic = m.group(1).trim();
        String json = m.group(2).trim();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        return new AST.StreamPublishStatement(topic, map);
    }

    private String extractAiEmbedArg(String expr) {
        Pattern p = Pattern.compile("AI_EMBED\\(['\"]?(.*?)['\"]?\\)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(expr);
        return m.find() ? m.group(1) : expr;
    }

    private String extractFunctionArg(String expr) {
        int start = expr.indexOf("(");
        int end = expr.lastIndexOf(")");
        return (start >= 0 && end > start) ? expr.substring(start + 1, end).trim() : expr;
    }

    private String[] extractFunctionArgs(String expr) {
        String inner = extractFunctionArg(expr);
        String[] parts = inner.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = unquote(parts[i].trim());
        }
        return parts;
    }

    private List<String> parseCSVValues(String csv) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '\'' || c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    private String unquote(String val) {
        if (val == null) return null;
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private Object parseLiteral(String val) {
        if (val == null) return null;
        try { return Integer.parseInt(val); } catch (Exception ignored) {}
        try { return Double.parseDouble(val); } catch (Exception ignored) {}
        if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) return Boolean.parseBoolean(val);
        return val;
    }
}
