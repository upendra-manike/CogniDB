# 📖 CogniDB Complete Technical Documentation & Reference Guide

Welcome to the official technical documentation for **CogniDB**, the next-generation AI-native unified database engine.

---

## 📚 Table of Contents

1. [Architectural Principles & Engines](#1-architectural-principles--engines)
2. [Connection String & Security](#2-connection-string--security)
3. [ACID Transaction Management](#3-acid-transaction-management)
4. [Complete SQL & AI Query Reference](#4-complete-sql--ai-query-reference)
5. [Backend Language SDK Integration (Python, Java, Go, Node.js, C#)](#5-backend-language-sdk-integration)
6. [Storage Engine & Recovery Mechanics](#6-storage-engine--recovery-mechanics)
7. [Distributed Consensus & Anti-Entropy](#7-distributed-consensus--anti-entropy)
8. [Cloud Deployment & Configuration Reference](#8-cloud-deployment--configuration-reference)

---

## 1. Architectural Principles & Engines

CogniDB replaces multi-database sprawl by combining 6 core engines into a single JVM process:

- **Unified Netty Server**: High-concurrency async network layer built on Netty 4.1.
- **Java 21 Generational ZGC**: Sub-millisecond pause times (<1ms) for multi-gigabyte memory pools.
- **HNSW Vector Graph Index**: Sub-1.2ms Approximate Nearest Neighbor (ANN) search over high-dimensional vector embeddings.
- **LSM-Tree Core**: Sequential Write-Ahead Log (WAL) + SkipList MemTable + Immutable SSTables for fast write throughput.

---

## 2. Connection String & Security

### 🔗 Connection String Standard
CogniDB uses standard URI formats for driver connections:

```text
cognidb://<username>:<password>@<host>:<port>/<database>
```

#### Examples:
* **Local Development**: `cognidb://admin:cognidb_secret_pass@localhost:8080/default`
* **Production VM**: `cognidb://cogni_admin:SecurePass123!@10.0.1.50:8080/production`

---

## 3. ACID Transaction Management

CogniDB provides full **ACID (Atomicity, Consistency, Isolation, Durability)** transactions managed by `com.cognidb.engine.txn.TransactionManager`:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Transaction Lifecycle                          │
│                                                                        │
│   BEGIN TRANSACTION   ──►   Staged Uncommitted Writes                  │
│                                    │                                   │
│                        ┌───────────┴───────────┐                       │
│                        ▼                       ▼                       │
│               Conflict Detected?       No Conflict & WAL Flush         │
│                        │                       │                       │
│                        ▼                       ▼                       │
│                 ROLLBACK / ABORT            COMMIT                     │
└────────────────────────────────────────────────────────────────────────┘
```

### 🛡️ ACID Guarantees:
* **Atomicity**: Either all operations in a transaction succeed, or `ROLLBACK` reverts all staged modifications.
* **Consistency**: All updates enforce primary key constraints, field types, and 128-dimensional vector sizes.
* **Isolation (Snapshot Isolation / OCC)**: Uses Optimistic Concurrency Control with logical timestamp ordering. If two concurrent transactions attempt to modify the same row, the second transaction detects a **Write-Write Conflict** and automatically aborts to prevent data loss.
* **Durability**: Committed data is synchronously written to the sequential **WAL (Write-Ahead Log)** on disk before the `COMMIT` call completes.

### 💻 SQL Transaction Examples:

```sql
-- 1. Start Transaction
BEGIN TRANSACTION;

-- 2. Stage Updates & Vector Embeddings
INSERT INTO products VALUES (
  'prod_201', 'Logitech MX Master 3S', 'Peripherals', 99.99, 'Ergonomic wireless mouse for coding', AI_EMBED('wireless ergonomic mouse')
);

UPDATE products SET price = 89.99 WHERE id = 'prod_201';

-- 3. Commit All Writes Atomically
COMMIT;
```

If an error occurs or conflict is detected:
```sql
-- Abort & Rollback staged modifications
ROLLBACK;
```

---

## 4. Complete SQL & AI Query Reference

### 🔹 DDL Statements (Data Definition Language)
```sql
CREATE TABLE users (
  id VARCHAR PRIMARY KEY,
  name VARCHAR,
  city VARCHAR,
  age INT,
  role VARCHAR,
  bio VARCHAR,
  embedding FLOAT_VECTOR(128)
);
```

### 🔹 DML Statements (Data Manipulation Language)
```sql
INSERT INTO users VALUES (
  'usr_101',
  'Upendra Kumar',
  'Hyderabad',
  29,
  'Principal AI & Systems Architect',
  'Specializes in high throughput distributed databases, Raft consensus, Netty, Java 21, and vector embeddings.',
  AI_EMBED('Principal AI & Systems Architect Java Raft Netty')
);
```

### 🔹 DQL Statements (Data Query Language)
```sql
SELECT id, name, role, bio 
FROM users 
WHERE city = 'Hyderabad' 
  AND embedding SIMILAR TO 'Java Systems Engineer' 
TOP 5;
```

---

## 5. Backend Language SDK Integration

### 🐍 Python
```python
import requests

API_URL = "http://localhost:8080/api/sql"
payload = {"sql": "BEGIN; INSERT INTO products VALUES (...); COMMIT;"}
response = requests.post(API_URL, json=payload)
print(response.json())
```

### ☕ Java (`java.net.http.HttpClient`)
```java
HttpClient client = HttpClient.newHttpClient();
String payload = "{\"sql\": \"SELECT id, name FROM users LIMIT 5\"}";
HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/api/sql"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build();
System.out.println(client.send(req, HttpResponse.BodyHandlers.ofString()).body());
```

---

## 6. Storage Engine & Recovery Mechanics

CogniDB uses a Log-Structured Merge-Tree (LSM-Tree) engine for data persistence:

1. **Write Path**: Appended to sequential **WAL** -> Inserted into **MemTable** -> Flushed to immutable **SSTables**.
2. **Read Path**: Check **Hot LRFU Cache** -> Check **MemTable** -> Check **Bloom Filter** -> Binary Search **SSTable**.
