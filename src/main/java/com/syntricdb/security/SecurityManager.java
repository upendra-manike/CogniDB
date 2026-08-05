package com.syntricdb.security;

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
        this("admin", "syntricdb_secret_pass");
    }

    public SecurityManager(com.syntricdb.config.SyntricConfig config) {
        this(config != null ? config.getAdminUser() : "admin", config != null ? config.getAdminPassword() : "syntricdb_secret_pass");
    }

    public SecurityManager(String adminUser, String adminPassword) {
        // Master Keys for demonstration
        apiKeys.put("syntric_master_key_99", Role.ADMIN);
        apiKeys.put("syntric_rw_key_22", Role.READ_WRITE);
        apiKeys.put("syntric_ro_key_11", Role.READ_ONLY);

        // Admin User from configuration
        createUser(adminUser != null ? adminUser : "admin", adminPassword != null ? adminPassword : "syntricdb_secret_pass", Role.ADMIN);
    }

    // --- 1. CONNECTION STRING PARSER ---
    // Format: syntricdb://username:password@host:port/database
    public static ConnectionString parseConnectionString(String uri) {
        Pattern pattern = Pattern.compile("syntricdb://([^:]+):([^@]+)@([^:]+):(\\d+)/(.+)");
        Matcher matcher = pattern.matcher(uri);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SyntricDB Connection String format. Expected: syntricdb://user:pass@host:port/db");
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
            byte[] salt = "SyntricDBSalt_Prod2026".getBytes(StandardCharsets.UTF_8);
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(rawPassword.toCharArray(), salt, 10000, 256);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
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
            } catch (Exception ex) {
                throw new RuntimeException("Error hashing password", ex);
            }
        }
    }

    public synchronized void saveCatalogToFile(java.nio.file.Path file) throws java.io.IOException {
        if (file.getParent() != null) {
            java.nio.file.Files.createDirectories(file.getParent());
        }
        Map<String, Object> data = new HashMap<>();
        Map<String, Map<String, String>> userMap = new HashMap<>();
        for (Map.Entry<String, UserCredentials> e : users.entrySet()) {
            Map<String, String> u = new HashMap<>();
            u.put("hash", e.getValue().getPasswordHash());
            u.put("role", e.getValue().getRole().name());
            userMap.put(e.getKey(), u);
        }
        data.put("users", userMap);
        Map<String, String> keys = new HashMap<>();
        for (Map.Entry<String, Role> e : apiKeys.entrySet()) {
            keys.put(e.getKey(), e.getValue().name());
        }
        data.put("apiKeys", keys);
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(file.toFile(), data);
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadCatalogFromFile(java.nio.file.Path file) throws java.io.IOException {
        if (!java.nio.file.Files.exists(file)) return;
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> data = mapper.readValue(file.toFile(), Map.class);
        Map<String, Map<String, String>> userMap = (Map<String, Map<String, String>>) data.get("users");
        if (userMap != null) {
            for (Map.Entry<String, Map<String, String>> e : userMap.entrySet()) {
                String name = e.getKey();
                String hash = e.getValue().get("hash");
                Role role = Role.valueOf(e.getValue().get("role"));
                users.put(name, new UserCredentials(name, hash, role));
            }
        }
        Map<String, String> keys = (Map<String, String>) data.get("apiKeys");
        if (keys != null) {
            for (Map.Entry<String, String> e : keys.entrySet()) {
                apiKeys.put(e.getKey(), Role.valueOf(e.getValue()));
            }
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
