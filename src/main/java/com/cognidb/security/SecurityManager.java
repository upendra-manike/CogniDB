package com.cognidb.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecurityManager {
    private static final Logger log = LoggerFactory.getLogger(SecurityManager.class);

    public enum Role {
        ADMIN,
        READ_WRITE,
        READ_ONLY
    }

    public static class UserCredentials {
        private final String username;
        private final String passwordHash;
        private final Role role;

        public UserCredentials(String username, String passwordHash, Role role) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getPasswordHash() { return passwordHash; }
        public Role getRole() { return role; }
    }

    public static class ConnectionString {
        private final String username;
        private final String password;
        private final String host;
        private final int port;
        private final String database;

        public ConnectionString(String username, String password, String host, int port, String database) {
            this.username = username;
            this.password = password;
            this.host = host;
            this.port = port;
            this.database = database;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabase() { return database; }
    }

    private final Map<String, Role> apiKeys = new ConcurrentHashMap<>();
    private final Map<String, UserCredentials> users = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimiterMap = new ConcurrentHashMap<>();
    private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
    private boolean firewallEnabled = false;
    private boolean dlpEnabled = true;

    public SecurityManager() {
        // Default Master Keys for demonstration
        apiKeys.put("cogni_master_key_99", Role.ADMIN);
        apiKeys.put("cogni_rw_key_22", Role.READ_WRITE);
        apiKeys.put("cogni_ro_key_11", Role.READ_ONLY);

        // Default Admin User
        createUser("admin", "cognidb_secret_pass", Role.ADMIN);
    }

    // --- 1. CONNECTION STRING PARSER ---
    // Format: cognidb://username:password@host:port/database
    public static ConnectionString parseConnectionString(String uri) {
        Pattern pattern = Pattern.compile("cognidb://([^:]+):([^@]+)@([^:]+):(\\d+)/(.+)");
        Matcher matcher = pattern.matcher(uri);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid CogniDB Connection String format. Expected: cognidb://user:pass@host:port/db");
        }

        return new ConnectionString(
            matcher.group(1),
            matcher.group(2),
            matcher.group(3),
            Integer.parseInt(matcher.group(4)),
            matcher.group(5)
        );
    }

    // --- 2. AUTHENTICATION & USER MANAGEMENT ---
    public void createUser(String username, String rawPassword, Role role) {
        String hash = hashPassword(rawPassword);
        users.put(username, new UserCredentials(username, hash, role));
        log.info("Registered user '{}' with role {}", username, role);
    }

    public boolean authenticateUser(String username, String rawPassword) {
        UserCredentials user = users.get(username);
        if (user == null) return false;
        return user.getPasswordHash().equals(hashPassword(rawPassword));
    }

    public boolean validateApiKey(String apiKey, Role requiredRole) {
        if (apiKey == null || !apiKeys.containsKey(apiKey)) {
            return false;
        }
        Role clientRole = apiKeys.get(apiKey);
        if (requiredRole == Role.ADMIN && clientRole != Role.ADMIN) return false;
        if (requiredRole == Role.READ_WRITE && clientRole == Role.READ_ONLY) return false;
        return true;
    }

    public void addApiKey(String key, Role role) {
        apiKeys.put(key, role);
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // --- 3. FIREWALL & RATE LIMITING ---
    public boolean checkRateLimit(String clientIp, int maxRequestsPerSec) {
        long currentSec = System.currentTimeMillis() / 1000;
        String key = clientIp + ":" + currentSec;
        long reqCount = rateLimiterMap.compute(key, (k, v) -> v == null ? 1L : v + 1);

        if (rateLimiterMap.size() > 5000) {
            rateLimiterMap.entrySet().removeIf(e -> !e.getKey().endsWith(":" + currentSec));
        }

        return reqCount <= maxRequestsPerSec;
    }

    public void enableFirewall(boolean enable) {
        this.firewallEnabled = enable;
    }

    public void allowIp(String ip) {
        allowedIps.add(ip);
    }

    public boolean isIpAllowed(String ip) {
        if (!firewallEnabled || allowedIps.isEmpty()) return true;
        return allowedIps.contains(ip) || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }

    // --- 4. DATA LEAKAGE PROTECTION (DLP) & MASKING ---
    public String sanitizeDlp(String text) {
        if (!dlpEnabled || text == null) return text;

        // Mask SSN (xxx-xx-xxxx)
        text = text.replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "***-**-****");

        // Mask Credit Cards (16 digits)
        text = text.replaceAll("\\b(?:\\d[ -]*?){13,16}\\b", "****-****-****-****");

        // Mask Email addresses
        text = text.replaceAll("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", "m***@dlp-protected.com");

        return text;
    }

    public Map<String, Object> sanitizeMapDlp(Map<String, Object> map) {
        if (!dlpEnabled || map == null) return map;
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (isSensitiveKey(key) && val instanceof String) {
                sanitized.put(key, "████████ [DLP REDACTED]");
            } else if (val instanceof String) {
                sanitized.put(key, sanitizeDlp((String) val));
            } else {
                sanitized.put(key, val);
            }
        }
        return sanitized;
    }

    private boolean isSensitiveKey(String key) {
        String k = key.toLowerCase();
        return k.contains("password") || k.contains("secret") || k.contains("token") || k.contains("ssn") || k.contains("creditcard");
    }
}
