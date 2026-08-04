package com.syntricdb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SyntricConfig {
    private static final Logger log = LoggerFactory.getLogger(SyntricConfig.class);

    private String bindAddress = "0.0.0.0";
    private int port = 8080;
    private boolean authEnabled = true;
    private String adminUser = "admin";
    private String adminPassword = "syntricdb_secret_pass";
    private String dataDir = System.getProperty("user.home") + "/.syntricdb/data";
    private String clusterSeeds = "127.0.0.1:8080";
    private boolean sslEnabled = false;

    public SyntricConfig() {
        loadConfig();
    }

    public void loadConfig() {
        Properties props = new Properties();

        // 1. Check system config file /etc/syntricdb/syntricdb.conf
        Path confPath = Paths.get("/etc/syntricdb/syntricdb.conf");
        if (!Files.exists(confPath)) {
            confPath = Paths.get(System.getProperty("user.home"), ".syntricdb", "syntricdb.conf");
        }

        if (Files.exists(confPath)) {
            try (InputStream is = Files.newInputStream(confPath)) {
                props.load(is);
                log.info("Loaded SyntricDB configuration from: {}", confPath);
            } catch (Exception e) {
                log.warn("Failed to load config file: {}", confPath, e);
            }
        }

        // 2. Override with Environment Variables if present
        bindAddress = getEnvOrProp("SYNTRICDB_BIND_ADDRESS", props.getProperty("bind_address", bindAddress));
        port = Integer.parseInt(getEnvOrProp("SYNTRICDB_PORT", props.getProperty("port", String.valueOf(port))));
        authEnabled = Boolean.parseBoolean(getEnvOrProp("SYNTRICDB_AUTH_ENABLED", props.getProperty("auth_enabled", String.valueOf(authEnabled))));
        adminUser = getEnvOrProp("SYNTRICDB_ADMIN_USER", props.getProperty("admin_user", adminUser));
        adminPassword = getEnvOrProp("SYNTRICDB_ADMIN_PASSWORD", props.getProperty("admin_password", adminPassword));
        dataDir = getEnvOrProp("SYNTRICDB_DATA_DIR", props.getProperty("data_dir", dataDir));
        clusterSeeds = getEnvOrProp("SYNTRICDB_CLUSTER_SEEDS", props.getProperty("cluster_seeds", clusterSeeds));
        sslEnabled = Boolean.parseBoolean(getEnvOrProp("SYNTRICDB_SSL_ENABLED", props.getProperty("ssl_enabled", String.valueOf(sslEnabled))));
    }

    private String getEnvOrProp(String envKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        return envVal != null ? envVal : defaultValue;
    }

    public String getBindAddress() { return bindAddress; }
    public int getPort() { return port; }
    public boolean isAuthEnabled() { return authEnabled; }
    public String getAdminUser() { return adminUser; }
    public String getAdminPassword() { return adminPassword; }
    public String getDataDir() { return dataDir; }
    public String getClusterSeeds() { return clusterSeeds; }
    public boolean isSslEnabled() { return sslEnabled; }

    public String toConnectionString(String host) {
        return String.format("syntricdb://%s:%s@%s:%d/default", adminUser, adminPassword, host, port);
    }
}
