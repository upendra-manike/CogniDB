package com.syntricdb;

import com.syntricdb.security.SecurityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    public void testPbkdf2AuthenticationAndCatalogPersistence() throws Exception {
        Path catalogFile = tempDir.resolve("security_catalog.json");

        SecurityManager sec = new SecurityManager("admin", "super_secret_pass_123");
        sec.createUser("analyst", "analyst_pass_456", SecurityManager.Role.READ_ONLY);
        sec.addApiKey("apiKey_prod_777", SecurityManager.Role.READ_WRITE);

        assertTrue(sec.authenticateUser("admin", "super_secret_pass_123"));
        assertTrue(sec.authenticateUser("analyst", "analyst_pass_456"));
        assertFalse(sec.authenticateUser("admin", "wrong_pass"));

        sec.saveCatalogToFile(catalogFile);

        SecurityManager rehydrated = new SecurityManager("admin", "different_default_pass");
        rehydrated.loadCatalogFromFile(catalogFile);

        assertTrue(rehydrated.authenticateUser("analyst", "analyst_pass_456"));
        assertTrue(rehydrated.validateApiKey("apiKey_prod_777", SecurityManager.Role.READ_WRITE));
    }
}
