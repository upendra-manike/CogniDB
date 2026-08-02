# 📖 CogniDB Complete Technical Documentation & Reference Guide

Welcome to the official technical documentation for **CogniDB**, the next-generation AI-native unified database engine.

---

## 📚 Table of Contents

1. [Architectural Principles & Engines](#1-architectural-principles--engines)
2. [Connection String & Security](#2-connection-string--security)
3. [Complete SQL & AI Query Reference](#3-complete-sql--ai-query-reference)
4. [Backend Language SDK Integration (Python, Java, Go, Node.js, C#)](#4-backend-language-sdk-integration)
5. [Storage Engine & Recovery Mechanics](#5-storage-engine--recovery-mechanics)
6. [Distributed Consensus & Anti-Entropy](#6-distributed-consensus--anti-entropy)
7. [Cloud Deployment & Configuration Reference](#7-cloud-deployment--configuration-reference)

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

## 3. Complete SQL & AI Query Reference

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

## 4. Backend Language SDK Integration

### 🐍 Python
```python
import requests

API_URL = "http://localhost:8080/api/sql"
payload = {"sql": "SELECT id, name FROM users WHERE embedding SIMILAR TO 'AI Architect' TOP 3"}
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

### 🐹 Go (`net/http`)
```go
req, _ := http.NewRequest("POST", "http://localhost:8080/api/sql", bytes.NewBuffer([]byte(`{"sql":"SELECT * FROM users"}`)))
req.Header.Set("Content-Type", "application/json")
resp, _ := (&http.Client{}).Do(req)
body, _ := io.ReadAll(resp.Body)
fmt.Println(string(body))
```

### 🟢 Node.js (`fetch`)
```javascript
const res = await fetch("http://localhost:8080/api/sql", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ sql: "SELECT * FROM users LIMIT 5" })
});
console.log(await res.json());
```

### 🔷 C# (.NET)
```csharp
using var client = new HttpClient();
var content = new StringContent("{\"sql\":\"SELECT * FROM users LIMIT 5\"}", Encoding.UTF8, "application/json");
var response = await client.PostAsync("http://localhost:8080/api/sql", content);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```

---

## 5. Storage Engine & Recovery Mechanics

CogniDB uses a Log-Structured Merge-Tree (LSM-Tree) engine for data persistence:

1. **Write Path**: Appended to sequential **WAL** -> Inserted into **MemTable** -> Flushed to immutable **SSTables**.
2. **Read Path**: Check **Hot LRFU Cache** -> Check **MemTable** -> Check **Bloom Filter** -> Binary Search **SSTable**.

---

## 6. Distributed Consensus & Anti-Entropy

* **Raft Consensus**: Leader election and log replication across multi-node clusters.
* **Read Repair**: Real-time synchronization of stale replicas on reads.
* **Merkle Tree Sync**: Cryptographic range tree comparisons detect and repair missing records in background.
