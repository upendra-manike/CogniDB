package com.syntricdb.net;

import com.syntricdb.ai.AIEngine;
import com.syntricdb.cluster.ClusterState;
import com.syntricdb.cluster.RaftNode;
import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.Tuple;
import com.syntricdb.engine.stream.StreamEngine;
import com.syntricdb.engine.vector.HNSWIndex;
import com.syntricdb.sql.QueryExecutor;

import com.syntricdb.config.SyntricConfig;
import com.syntricdb.security.SecurityManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HTTPHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger log = LoggerFactory.getLogger(HTTPHandler.class);

    private final StorageEngine storageEngine;
    private final AIEngine aiEngine;
    private final QueryExecutor queryExecutor;
    private final ClusterState clusterState;
    private final SecurityManager securityManager;
    private final SyntricConfig config;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public HTTPHandler(StorageEngine storageEngine, AIEngine aiEngine, QueryExecutor queryExecutor, ClusterState clusterState) {
        this(storageEngine, aiEngine, queryExecutor, clusterState, new SecurityManager(), new SyntricConfig());
    }

    public HTTPHandler(StorageEngine storageEngine, AIEngine aiEngine, QueryExecutor queryExecutor, ClusterState clusterState, SecurityManager securityManager, SyntricConfig config) {
        this.storageEngine = storageEngine;
        this.aiEngine = aiEngine;
        this.queryExecutor = queryExecutor;
        this.clusterState = clusterState;
        this.config = config != null ? config : new SyntricConfig();
        this.securityManager = securityManager != null ? securityManager : new SecurityManager(this.config);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();
        HttpMethod method = req.method();

        if (method == HttpMethod.OPTIONS) {
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization, X-Syntric-Auth");
            ctx.writeAndFlush(response);
            return;
        }

        if (uri.startsWith("/api/")) {
            handleApi(ctx, req);
            return;
        }

        // Serve Static Web Dashboard UI
        handleStaticWeb(ctx, req);
    }

    private String authenticateRequest(FullHttpRequest req) {
        String authHeader = req.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring(6).trim();
                byte[] decoded = Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(decoded, StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    if (securityManager.authenticateUser(parts[0], parts[1])) {
                        return parts[0];
                    }
                }
            } catch (Exception ignored) {}
        }

        String apiKey = req.headers().get("X-Syntric-Auth");
        if (apiKey != null) {
            if (securityManager.validateApiKey(apiKey, SecurityManager.Role.READ_WRITE)) {
                return "api_key_user";
            }
        }

        return null;
    }

    private void handleApi(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();
        String body = req.content().toString(CharsetUtil.UTF_8);

        Map<String, Object> responseMap = new LinkedHashMap<>();

        try {
            if ("/api/auth/login".equals(uri) && req.method() == HttpMethod.POST) {
                Map<String, Object> reqJson = jsonMapper.readValue(body, Map.class);
                String username = reqJson.getOrDefault("username", "").toString();
                String password = reqJson.getOrDefault("password", "").toString();

                if (securityManager.authenticateUser(username, password)) {
                    String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                    sendJsonResponse(ctx, OK, Map.of(
                        "success", true,
                        "username", username,
                        "token", "Basic " + token,
                        "message", "Authentication successful"
                    ));
                } else {
                    sendJsonResponse(ctx, UNAUTHORIZED, Map.of(
                        "success", false,
                        "error", "Invalid username or password"
                    ));
                }
                return;
            }

            if ("/api/auth/verify".equals(uri) && req.method() == HttpMethod.GET) {
                String user = authenticateRequest(req);
                if (user != null) {
                    sendJsonResponse(ctx, OK, Map.of("success", true, "username", user));
                } else {
                    sendJsonResponse(ctx, UNAUTHORIZED, Map.of("success", false, "error", "Unauthorized"));
                }
                return;
            }

            if (config.isAuthEnabled()) {
                String authenticatedUser = authenticateRequest(req);
                if (authenticatedUser == null) {
                    sendJsonResponse(ctx, UNAUTHORIZED, Map.of(
                        "success", false,
                        "error", "Unauthorized. Please log in to SyntricDB Web Studio with valid credentials."
                    ));
                    return;
                }
            }

            if ("/api/sql".equals(uri) && req.method() == HttpMethod.POST) {
                Map<String, Object> reqJson = jsonMapper.readValue(body, Map.class);
                String sql = reqJson.get("sql").toString();
                QueryExecutor.QueryResult res = queryExecutor.execute(sql);

                responseMap.put("success", true);
                responseMap.put("message", res.getMessage());
                responseMap.put("executionTimeMs", res.getExecutionTimeMs());
                responseMap.put("rowCount", res.getRows().size());
                if (res.getExecutionPlan() != null) {
                    responseMap.put("planStrategy", res.getExecutionPlan().getStrategy().name());
                    responseMap.put("planDescription", res.getExecutionPlan().getDescription());
                    responseMap.put("estimatedCost", res.getExecutionPlan().getEstimatedCost());
                }
                responseMap.put("data", res.getRows());

            } else if ("/api/vector/search".equals(uri) && req.method() == HttpMethod.POST) {
                Map<String, Object> reqJson = jsonMapper.readValue(body, Map.class);
                String table = reqJson.get("table").toString();
                String column = reqJson.get("column").toString();
                String queryText = reqJson.get("query").toString();
                int limit = reqJson.containsKey("limit") ? Integer.parseInt(reqJson.get("limit").toString()) : 5;

                HNSWIndex hnsw = storageEngine.getVectorIndex(table, column);
                if (hnsw == null) {
                    throw new IllegalArgumentException("Vector index on " + table + "." + column + " not found.");
                }

                float[] queryVec = aiEngine.aiEmbed(queryText, hnsw.getDimension());
                long start = System.nanoTime();
                List<HNSWIndex.VectorSearchResult> vecResults = hnsw.search(queryVec, limit);
                long elapsed = System.nanoTime() - start;

                List<Map<String, Object>> rows = new ArrayList<>();
                for (HNSWIndex.VectorSearchResult r : vecResults) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", r.getId());
                    row.put("similarity", r.getSimilarity());
                    row.put("distance", r.getDistance());
                    Tuple t = storageEngine.getByPrimaryKey(table, r.getId());
                    if (t != null) {
                        row.put("record", t.asMap());
                    }
                    rows.add(row);
                }

                responseMap.put("success", true);
                responseMap.put("executionTimeMs", elapsed / 1_000_000.0);
                responseMap.put("query", queryText);
                responseMap.put("results", rows);

            } else if ("/api/ai/rag".equals(uri) && req.method() == HttpMethod.POST) {
                Map<String, Object> reqJson = jsonMapper.readValue(body, Map.class);
                String table = reqJson.getOrDefault("table", "users").toString();
                String column = reqJson.getOrDefault("column", "embedding").toString();
                String prompt = reqJson.get("prompt").toString();
                int limit = reqJson.containsKey("limit") ? Integer.parseInt(reqJson.get("limit").toString()) : 3;

                com.syntricdb.ai.RAGEngine rag = new com.syntricdb.ai.RAGEngine(storageEngine, aiEngine);
                com.syntricdb.ai.RAGEngine.RAGResult ragRes = rag.query(table, column, prompt, limit);

                responseMap.put("success", true);
                responseMap.put("prompt", ragRes.getPrompt());
                responseMap.put("augmentedPrompt", ragRes.getAugmentedPrompt());
                responseMap.put("generatedAnswer", ragRes.getGeneratedAnswer());
                responseMap.put("retrievalTimeMs", ragRes.getRetrievalTimeMs());
                responseMap.put("context", ragRes.getRetrievedContext());

            } else if ("/api/cluster".equals(uri) && req.method() == HttpMethod.GET) {
                responseMap.put("success", true);
                List<Map<String, Object>> nodeList = new ArrayList<>();
                for (RaftNode n : clusterState.getNodes().values()) {
                    Map<String, Object> nm = new LinkedHashMap<>();
                    nm.put("nodeId", n.getNodeId());
                    nm.put("role", n.getRole().name());
                    nm.put("term", n.getCurrentTerm());
                    nm.put("logCount", n.getLogCount());
                    nodeList.add(nm);
                }
                responseMap.put("nodes", nodeList);
                responseMap.put("writeOps", storageEngine.getWriteOpsCount());
                responseMap.put("readOps", storageEngine.getReadOpsCount());
                responseMap.put("cacheHitRate", storageEngine.getCacheEngine().getHitRate() * 100.0);
                responseMap.put("tableCount", storageEngine.getAllSchemas().size());

            } else if ("/api/tables".equals(uri) && req.method() == HttpMethod.GET) {
                responseMap.put("success", true);
                List<Map<String, Object>> tableList = new ArrayList<>();
                for (var schema : storageEngine.getAllSchemas().values()) {
                    Map<String, Object> tm = new LinkedHashMap<>();
                    tm.put("tableName", schema.getTableName());
                    tm.put("primaryKey", schema.getPrimaryKeyColumn());
                    tm.put("vectorColumn", schema.getVectorColumn());
                    tm.put("columns", schema.getColumnList());
                    tm.put("rowCount", storageEngine.scanAll(schema.getTableName()).size());
                    tableList.add(tm);
                }
                responseMap.put("tables", tableList);

            } else if ("/api/benchmark".equals(uri) && req.method() == HttpMethod.POST) {
                Map<String, Object> reqJson = jsonMapper.readValue(body, Map.class);
                int count = reqJson.containsKey("count") ? Integer.parseInt(reqJson.get("count").toString()) : 1000;

                long startWrite = System.currentTimeMillis();
                for (int i = 0; i < count; i++) {
                    Tuple t = new Tuple();
                    t.set("id", "bench_" + i);
                    t.set("title", "Software Engineer Record #" + i);
                    t.set("city", i % 2 == 0 ? "Hyderabad" : "Bengaluru");
                    t.set("age", 20 + (i % 30));
                    t.set("embedding", aiEngine.aiEmbed("Software Engineer Record " + i));
                    storageEngine.insert("users", t);
                }
                long writeTimeMs = Math.max(1, System.currentTimeMillis() - startWrite);
                double writesPerSec = (count * 1000.0) / writeTimeMs;

                long startSearch = System.currentTimeMillis();
                int searchCount = Math.min(count, 500);
                for (int i = 0; i < searchCount; i++) {
                    queryExecutor.execute("SELECT * FROM users WHERE embedding SIMILAR TO 'Engineer' TOP 5");
                }
                long searchTimeMs = Math.max(1, System.currentTimeMillis() - startSearch);
                double searchesPerSec = (searchCount * 1000.0) / searchTimeMs;

                responseMap.put("success", true);
                responseMap.put("insertedCount", count);
                responseMap.put("writeTimeMs", writeTimeMs);
                responseMap.put("writesPerSec", (long) writesPerSec);
                responseMap.put("searchCount", searchCount);
                responseMap.put("searchTimeMs", searchTimeMs);
                responseMap.put("searchesPerSec", (long) searchesPerSec);

            } else {
                sendJsonResponse(ctx, NOT_FOUND, Map.of("error", "Endpoint not found"));
                return;
            }

            sendJsonResponse(ctx, OK, responseMap);

        } catch (Exception e) {
            log.error("API Execution Error", e);
            sendJsonResponse(ctx, INTERNAL_SERVER_ERROR, Map.of("success", false, "error", e.getMessage()));
        }
    }

    private void handleStaticWeb(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();
        if ("/".equals(uri) || uri.isBlank()) {
            uri = "/index.html";
        }

        String path = "/web" + uri;
        InputStream is = getClass().getResourceAsStream(path);

        if (is == null) {
            is = getClass().getResourceAsStream("/web/index.html");
        }

        if (is == null) {
            sendJsonResponse(ctx, NOT_FOUND, Map.of("error", "Static asset not found"));
            return;
        }

        byte[] content = is.readAllBytes();
        String contentType = "text/html";
        if (uri.endsWith(".css")) contentType = "text/css";
        else if (uri.endsWith(".js")) contentType = "application/javascript";
        else if (uri.endsWith(".json")) contentType = "application/json";

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(content));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        ctx.writeAndFlush(response);
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, Object obj) throws Exception {
        byte[] bytes = jsonMapper.writeValueAsBytes(obj);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        ctx.writeAndFlush(response);
    }
}
