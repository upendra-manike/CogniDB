# 📖 CogniDB Complete Technical Documentation & Reference Guide

Welcome to the official technical documentation for **CogniDB**, the next-generation AI-native unified database engine.

---

## 📚 Table of Contents

1. [Architectural Principles & Engines](#1-architectural-principles--engines)
2. [Connection String & Security](#2-connection-string--security)
3. [Complete SQL & AI Query Reference](#3-complete-sql--ai-query-reference)
4. [Storage Engine & Recovery Mechanics](#4-storage-engine--recovery-mechanics)
5. [Distributed Consensus & Anti-Entropy](#5-distributed-consensus--anti-entropy)
6. [Cloud Deployment & Configuration Reference](#6-cloud-deployment--configuration-reference)

---

## 1. Architectural Principles & Engines

CogniDB replaces multi-database sprawl by combining 6 core engines into a single JVM process:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ⚡ CogniDB Engine Kernel ⚡                      │
├───────────────┬────────────────┬───────────────┬───────────────┬───────┤
│ SQL Relational│ HNSW Vector    │ BM25 Inverted │ In-Memory     │ Stream│
│ Storage       │ Index          │ Search        │ LRFU Cache    │ Engine│
└───────────────┴────────────────┴───────────────┴───────────────┴───────┘
```

- **Unified Netty Server**: High-concurrency async network layer built on Netty 4.1.
- **Java 21 Generational ZGC**: Sub-millisecond pause times (<1ms) for multi-gigabyte memory pools.
- **HNSW Vector Graph Index**: Sub-1.2ms Approximate Nearest Neighbor (ANN) search over high-dimensional vector embeddings.
- **LSM-Tree Core**: Sequential Write-Ahead Log (WAL) + SkipList MemTable + Immutable SSTables for fast write throughput.

---

## 2. Connection String & Security

### 🔗 Connection String Standard
CogniDB uses standard URI formats for driver connections:

```
cognidb://<username>:<password>@<host>:<port>/<database>
```

#### Examples:
* **Local Development**: `cognidb://admin:cognidb_secret_pass@localhost:8080/default`
* **Production VM**: `cognidb://cogni_admin:SecurePass123!@10.0.1.50:8080/production`

### 🔒 Enterprise Security Features
* **Role-Based Access Control (RBAC)**: Supports `ADMIN`, `READ_WRITE`, and `READ_ONLY` roles.
* **Token Bucket Firewall**: IP-based rate limiting (default 1000 req/sec) to defend against DDoS attacks.
* **Automatic DLP (Data Leakage Protection)**: Automated regex masking of sensitive PII:
  * **SSN**: Masked as `***-**-****`
  * **Credit Card**: Masked as `****-****-****-****`
  * **Email**: Masked as `m***@dlp-protected.com`

---

## 3. Complete SQL & AI Query Reference

### 🔹 DDL Statements (Data Definition Language)

#### Create Table with Relational & Vector Columns
```sql
CREATE TABLE users (
  id VARCHAR PRIMARY KEY,
  name VARCHAR,
  city VARCHAR,
  age INT,
  role VARCHAR,
  bio VARCHAR,
  skills VARCHAR,
  embedding FLOAT_VECTOR(128)
);
```

---

### 🔹 DML Statements (Data Manipulation Language)

#### Insert Record with Automatic AI Vector Embedding Generation
```sql
INSERT INTO users VALUES (
  'usr_101',
  'Upendra Kumar',
  'Hyderabad',
  29,
  'Principal AI & Systems Architect',
  'Specializes in high throughput distributed databases, Raft consensus, Netty, Java 21, and vector embeddings.',
  'Java, Systems Engineering, Vector Search',
  AI_EMBED('Principal AI & Systems Architect Java Raft Netty')
);
```

#### Update Record
```sql
UPDATE users SET age = 30 WHERE id = 'usr_101';
```

#### Delete Record
```sql
DELETE FROM users WHERE id = 'usr_101';
```

---

### 🔹 DQL Statements (Data Query Language)

#### Standard Relational SQL Query
```sql
SELECT id, name, city, role 
FROM users 
WHERE city = 'Hyderabad' AND age >= 25 
ORDER BY age DESC;
```

#### Hybrid Relational + Semantic Vector Search Query
```sql
SELECT id, name, role, bio 
FROM users 
WHERE city = 'Hyderabad' 
  AND embedding SIMILAR TO 'Java Systems Engineer' 
TOP 5;
```

#### AI Text Summarization & Classification Functions
```sql
SELECT name, 
       AI_SUMMARIZE(bio) AS bio_summary, 
       AI_CLASSIFY(bio, 'Engineer', 'Researcher', 'Manager') AS role_category 
FROM users;
```

#### Native In-Engine RAG (Retrieval-Augmented Generation)
```sql
SELECT AI_RAG('Which architects specialize in high throughput databases in Hyderabad?');
```

#### Full-Text Keyword Search Query (BM25 Engine)
```sql
SELECT id, name, bio 
FROM users 
WHERE MATCH(bio, 'distributed consensus raft');
```

---

### 🔹 Real-Time Streaming & Admin Commands

#### Publish Event to Stream Topic
```sql
PUBLISH topic 'user_activity' MESSAGE 'User usr_101 logged in';
```

#### Create Point-in-Time Disaster Recovery Snapshot
```sql
SNAPSHOT CREATE;
```

#### Restore Point-in-Time Disaster Recovery Snapshot
```sql
SNAPSHOT RESTORE 'cognidb_snapshot_20260802_155138';
```

---

## 4. Storage Engine & Recovery Mechanics

CogniDB uses a Log-Structured Merge-Tree (LSM-Tree) engine for data persistence:

1. **Write Path**:
   - Write appended to sequential **WAL (Write-Ahead Log)** on disk.
   - Inserted into memory **MemTable (ConcurrentSkipListMap)**.
   - When MemTable reaches 64MB, it is asynchronously flushed to an immutable **SSTable file**.
2. **Read Path**:
   - Check **LRFU Hot Cache** ($O(1)$).
   - Check **MemTable** ($O(\log N)$).
   - Check **Bloom Filter** to avoid unnecessary disk I/O.
   - Perform Sparse Index Binary Search on **SSTable**.

---

## 5. Distributed Consensus & Anti-Entropy

CogniDB maintains consistency across multi-node clusters:

* **Raft Consensus**: Leader election, heartbeats, and log replication across nodes.
* **Virtual Node Consistent Hashing Ring**: Automatic partition sharding across cluster topology.
* **Read Repair**: Syncs stale replicas during read operations.
* **Merkle Tree Sync**: Cryptographic range trees perform background data comparison to repair missing records across nodes.

---

## 6. Cloud Deployment & Configuration Reference

### Production Configuration (`/etc/cognidb/cognidb.conf`)

```ini
# Network & Binding
bind_address=0.0.0.0
port=8080

# Security & Credentials
auth_enabled=true
admin_user=admin
admin_password=CogniDB_Secure_Pass_123!

# Storage Locations
data_dir=/var/lib/cognidb/data
wal_dir=/var/lib/cognidb/wal
snapshot_dir=/var/lib/cognidb/snapshots

# Security Features
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
```

---

## 📜 Summary

CogniDB gives developers a fast, AI-native database experience with zero operational friction. For support or contributions, visit [GitHub Repository](https://github.com/upendra-manike/CogniDB).
