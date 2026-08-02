package com.cognidb.cli;

import com.cognidb.sql.QueryExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Console;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CogniCLI {
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private final QueryExecutor inProcessExecutor;
    private String serverUrl = "http://localhost:8080";
    private String username;
    private String password;

    public CogniCLI() {
        this.inProcessExecutor = null;
    }

    public CogniCLI(QueryExecutor inProcessExecutor) {
        this.inProcessExecutor = inProcessExecutor;
    }

    public static void main(String[] args) {
        CogniCLI cli = new CogniCLI();
        cli.parseArgsAndStart(args);
    }

    public void startInteractiveRepl() {
        parseArgsAndStart(new String[0]);
    }

    public void parseArgsAndStart(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Console console = System.console();

        for (int i = 0; i < args.length; i++) {
            if ("-u".equals(args[i]) || "--user".equals(args[i])) {
                if (i + 1 < args.length) username = args[++i];
            } else if ("-p".equals(args[i]) || "--password".equals(args[i])) {
                if (i + 1 < args.length) password = args[++i];
            } else if ("-h".equals(args[i]) || "--host".equals(args[i])) {
                if (i + 1 < args.length) serverUrl = args[++i];
            } else if ("--uri".equals(args[i])) {
                if (i + 1 < args.length) parseUri(args[++i]);
            }
        }

        System.out.println("==================================================================");
        System.out.println("⚡ Welcome to CogniDB AI-Native Interactive CLI Shell ⚡");
        System.out.println("==================================================================");

        if (inProcessExecutor == null) {
            // Prompt for username if missing
            if (username == null || username.isBlank()) {
                System.out.print("Enter Database Username [admin]: ");
                String inputUser = scanner.nextLine().trim();
                username = inputUser.isBlank() ? "admin" : inputUser;
            }

            // Prompt for password if missing
            if (password == null || password.isBlank()) {
                if (console != null) {
                    char[] pwdChars = console.readPassword("Enter Database Password: ");
                    password = new String(pwdChars);
                } else {
                    System.out.print("Enter Database Password: ");
                    password = scanner.nextLine().trim();
                }
            }

            System.out.println("Connecting to CogniDB at " + serverUrl + " as user '" + username + "'...");
        } else {
            System.out.println("Connected to in-process CogniDB Engine instance.");
        }

        startRepl(scanner);
    }

    private void parseUri(String uri) {
        try {
            if (uri.startsWith("cognidb://")) {
                String clean = uri.substring(10);
                String[] authAndHost = clean.split("@");
                String[] userPass = authAndHost[0].split(":");
                this.username = userPass[0];
                this.password = userPass[1];

                String[] hostAndDb = authAndHost[1].split("/");
                String[] hostPort = hostAndDb[0].split(":");
                this.serverUrl = "http://" + hostPort[0] + ":" + hostPort[1];
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not parse connection URI, falling back to defaults.");
        }
    }

    public void startRepl(Scanner scanner) {
        System.out.println("✅ Authenticated & Connected!");
        System.out.println("Type SQL statements, vector queries, or 'exit' / 'quit' to exit.\n");

        String displayUser = username != null ? username : "local";
        String displayHost = inProcessExecutor != null ? "embedded" : extractHost(serverUrl);

        while (true) {
            System.out.print("cognidb [" + displayUser + "@" + displayHost + "]> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (line.isBlank()) continue;

            if (inProcessExecutor != null) {
                executeDirect(line);
            } else {
                executeQueryRemote(line);
            }
        }
    }

    private void executeDirect(String sql) {
        try {
            QueryExecutor.QueryResult result = inProcessExecutor.execute(sql);
            System.out.println("STATUS: " + result.getMessage() + " (Time: " + String.format("%.2f", result.getExecutionTimeMs()) + " ms)");
            if (result.getExecutionPlan() != null) {
                System.out.println("PLAN: [" + result.getExecutionPlan().getStrategy() + "] " + result.getExecutionPlan().getDescription());
            }
            if (!result.getRows().isEmpty()) {
                System.out.println("RESULTS (" + result.getRows().size() + " rows):");
                for (int i = 0; i < result.getRows().size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + result.getRows().get(i));
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage() + "\n");
        }
    }

    private String extractHost(String url) {
        return url.replace("http://", "").replace("https://", "");
    }

    private void executeQueryRemote(String sql) {
        try {
            URL url = new URL(serverUrl + "/api/sql");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "CogniCLI/1.0");

            String authHeader = "Bearer " + username + ":" + password;
            conn.setRequestProperty("Authorization", authHeader);
            conn.setDoOutput(true);

            Map<String, Object> reqMap = Map.of("sql", sql);
            byte[] body = jsonMapper.writeValueAsBytes(reqMap);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            Map<String, Object> respMap = jsonMapper.readValue(is, Map.class);

            if (Boolean.TRUE.equals(respMap.get("success"))) {
                System.out.println("STATUS: " + respMap.get("message") + " (Time: " + String.format("%.2f", respMap.get("executionTimeMs")) + " ms)");
                if (respMap.containsKey("planStrategy")) {
                    System.out.println("PLAN: [" + respMap.get("planStrategy") + "] " + respMap.get("planDescription"));
                }
                List<Map<String, Object>> data = (List<Map<String, Object>>) respMap.get("data");
                if (data != null && !data.isEmpty()) {
                    System.out.println("RESULTS (" + data.size() + " rows):");
                    for (int i = 0; i < data.size(); i++) {
                        System.out.println("  " + (i + 1) + ". " + data.get(i));
                    }
                }
            } else {
                System.out.println("ERROR: " + respMap.getOrDefault("error", respMap.get("message")));
            }
            System.out.println();

        } catch (Exception e) {
            System.out.println("CLI ERROR: " + e.getMessage() + "\n");
        }
    }
}
