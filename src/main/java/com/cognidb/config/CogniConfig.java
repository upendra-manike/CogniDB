package com.cognidb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class CogniConfig {
    private static final Logger log = LoggerFactory.getLogger(CogniConfig.class);

    private String bindAddress = "0.0.0.0";
    private int port = 8080;
    private boolean authEnabled = true;
    private String adminUser = "admin";
    private String adminPassword = "cognidb_secret_pass";
    private String dataDir = System.getProperty("user.home") + "/.cognidb/data";
    private String clusterSeeds = "127.0.0.1:8080";
    private boolean sslEnabled = false;

    public CogniConfig() {
        loadConfig();
    }

    public void loadConfig() {
        Properties props = new Properties();

        // 1. Check system config file /etc/cognidb/cognidb.conf
        Path confPath = Paths.get("/etc/cognidb/cognidb.conf");
        if (!Files.exists(confPath)) {
            confPath = Paths.get(System.getProperty("user.home"), ".cognidb", "cognidb.conf");
        }

        if (Files.exists(confPath)) {
            try (InputStream is = Files.newInputStream(confPath)) {
                props.load(is);
                log.info("Loaded CogniDB configuration from: {}", confPath);
            } catch (Exception e) {
                log.warn("Failed to load config file: {}", confPath, e);
            }
        }

        // 2. Override with Environment Variables if present
        bindAddress = getEnvOrProp("COGNIDB_BIND_ADDRESS", props.getProperty("bind_address", bindAddress));
        port = Integer.parseInt(getEnvOrProp("COGNIDB_PORT", props.getProperty("port", String.valueOf(port))));
        authEnabled = Boolean.parseBoolean(getEnvOrProp("COGNIDB_AUTH_ENABLED", props.getProperty("auth_enabled", String.valueOf(authEnabled))));
        adminUser = getEnvOrProp("COGNIDB_ADMIN_USER", props.getProperty("admin_user", adminUser));
        adminPassword = getEnvOrProp("COGNIDB_ADMIN_PASSWORD", props.getProperty("admin_password", adminPassword));
        dataDir = getEnvOrProp("COGNIDB_DATA_DIR", props.getProperty("data_dir", dataDir));
        clusterSeeds = getEnvOrProp("COGNIDB_CLUSTER_SEEDS", props.getProperty("cluster_seeds", clusterSeeds));
        sslEnabled = Boolean.parseBoolean(getEnvOrProp("COGNIDB_SSL_ENABLED", props.getProperty("ssl_enabled", String.valueOf(sslEnabled))));
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
        return String.format("cognidb://%s:%s@%s:%d/default", adminUser, adminPassword, host, port);
    }
}
