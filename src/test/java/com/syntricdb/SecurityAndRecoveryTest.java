package com.syntricdb;

import com.syntricdb.engine.snapshot.SnapshotManager;
import com.syntricdb.security.SecurityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAndRecoveryTest {

    @TempDir
    Path tempDir;

    private SecurityManager securityManager;
    private SnapshotManager snapshotManager;

    @BeforeEach
    public void setup() throws Exception {
        securityManager = new SecurityManager();
        snapshotManager = new SnapshotManager(tempDir);
    }

    @Test
    public void testConnectionStringParsing() {
        String connStr = "syntricdb://admin:syntricdb_secret_pass@10.0.1.50:8080/production";
        SecurityManager.ConnectionString parsed = SecurityManager.parseConnectionString(connStr);

        assertEquals("admin", parsed.getUsername());
        assertEquals("syntricdb_secret_pass", parsed.getPassword());
        assertEquals("10.0.1.50", parsed.getHost());
        assertEquals(8080, parsed.getPort());
        assertEquals("production", parsed.getDatabase());
    }

    @Test
    public void testUserAuthentication() {
        securityManager.createUser("prod_user", "SecurePass123!", SecurityManager.Role.READ_WRITE);

        assertTrue(securityManager.authenticateUser("prod_user", "SecurePass123!"));
        assertFalse(securityManager.authenticateUser("prod_user", "WrongPassword"));
        assertFalse(securityManager.authenticateUser("non_existent_user", "Pass"));
    }

    @Test
    public void testDlpDataMasking() {
        String sensitiveText = "User SSN is 123-45-6789, email is john.doe@example.com, card 4111-2222-3333-4444";
        String masked = securityManager.sanitizeDlp(sensitiveText);

        assertFalse(masked.contains("123-45-6789"));
        assertTrue(masked.contains("***-**-****"));

        assertFalse(masked.contains("john.doe@example.com"));
        assertTrue(masked.contains("dlp-protected.com"));
    }

    @Test
    public void testAuthenticationAndRoles() {
        assertTrue(securityManager.validateApiKey("syntric_master_key_99", SecurityManager.Role.ADMIN));
        assertTrue(securityManager.validateApiKey("syntric_rw_key_22", SecurityManager.Role.READ_WRITE));
        assertFalse(securityManager.validateApiKey("syntric_ro_key_11", SecurityManager.Role.ADMIN));
        assertFalse(securityManager.validateApiKey("invalid_key", SecurityManager.Role.READ_ONLY));
    }

    @Test
    public void testFirewallRateLimiting() {
        String clientIp = "192.168.1.50";
        // Allow up to 3 requests per second
        assertTrue(securityManager.checkRateLimit(clientIp, 3));
        assertTrue(securityManager.checkRateLimit(clientIp, 3));
        assertTrue(securityManager.checkRateLimit(clientIp, 3));
        // 4th request exceeds rate limit
        assertFalse(securityManager.checkRateLimit(clientIp, 3));
    }

    @Test
    public void testSnapshotBackupAndRestore() throws Exception {
        // Create dummy data file
        Path dbFile = tempDir.resolve("users.wal");
        Files.writeString(dbFile, "RECOVERY_LOG_DATA_CONTENT");

        // Take Snapshot
        String snapshotName = snapshotManager.createSnapshot();
        assertNotNull(snapshotName);

        // Delete original file
        Files.delete(dbFile);
        assertFalse(Files.exists(dbFile));

        // Restore Snapshot
        snapshotManager.restoreSnapshot(snapshotName);
        assertTrue(Files.exists(dbFile));
        assertEquals("RECOVERY_LOG_DATA_CONTENT", Files.readString(dbFile));
    }
}
