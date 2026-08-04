package com.syntricdb.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class SyntricDBClient {
    private final String host;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SyntricDBClient() {
        this("http://localhost:8080", null);
    }

    public SyntricDBClient(String host) {
        this(host, null);
    }

    public SyntricDBClient(String host, String apiKey) {
        this.host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public QueryResult query(String sql) throws IOException, InterruptedException {
        String endpoint = host + "/api/sql";
        Map<String, String> payload = new HashMap<>();
        payload.put("sql", sql);

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), QueryResult.class);
        } else {
            throw new RuntimeException("SyntricDB Error (" + response.statusCode() + "): " + response.body());
        }
    }

    public QueryResult executeSql(String sql) throws IOException, InterruptedException {
        return query(sql);
    }

    public Map<String, Object> vectorSearch(String table, String column, String queryText, int limit) throws IOException, InterruptedException {
        String endpoint = host + "/api/vector/search";
        Map<String, Object> payload = new HashMap<>();
        payload.put("table", table);
        payload.put("column", column != null ? column : "embedding");
        payload.put("query", queryText);
        payload.put("limit", limit);

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Map.class);
    }

    public Map<String, Object> askRag(String prompt, String table, String column, int limit) throws IOException, InterruptedException {
        String endpoint = host + "/api/ai/rag";
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", prompt);
        payload.put("table", table != null ? table : "users");
        payload.put("column", column != null ? column : "embedding");
        payload.put("limit", limit);

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Map.class);
    }

    public Map<String, Object> getClusterStatus() throws IOException, InterruptedException {
        String endpoint = host + "/api/cluster";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .GET();

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Map.class);
    }

    public String getHost() { return host; }
}
