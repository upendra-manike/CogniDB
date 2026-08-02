# ⚡ CogniDB: Next-Generation AI-Native Unified Database Engine

[![Build Status](https://github.com/upendra-manike/CogniDB/workflows/CogniDB%20CI%20Workflow/badge.svg)](https://github.com/upendra-manike/CogniDB/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://jdk.java.net/21/)

**CogniDB** is a distributed, high-performance, AI-native unified database engine built from the ground up on **Java 21 LTS, Netty 4, HNSW Vector Indexing, LSM-Tree Storage, BM25 Full-Text Search, and Raft Consensus**.

Instead of stitching together PostgreSQL, Redis, Elasticsearch, Kafka, and Pinecone over high-latency network boundaries, **CogniDB unifies SQL, Vector Search, In-Memory Caching, Streaming, Full-Text Search, and Built-In AI SQL Functions into ONE engine**.

---

## 📐 Unified Engine Architecture

```
                                  Client Request (REST / SQL / CLI)
                                                  │
                                                  ▼
                                       Unified SQL & AI Parser
                                                  │
                                                  ▼
                                     Cost-Based Optimizer (CBO)
                 ┌────────────────────────────────┼────────────────────────────────┐
                 │                                │                                │
                 ▼                                ▼                                ▼
       HNSW Vector Index               LSM Storage Engine               Inverted BM25 Index
    (High-Dim ANN Search)            (WAL + MemTable + SSTable)       (Full-Text Keyword Search)
                 │                                │                                │
                 └────────────────────────────────┼────────────────────────────────┘
                                                  ▼
                                       In-Memory LRFU Cache
                                                  │
                                                  ▼
                                     Distributed Raft Replication
```

---

## 🌟 Key Capabilities

- 🧠 **Sub-Millisecond Vector Search (HNSW)**: Hierarchical Navigable Small World graph index for high-dimensional vector search with Cosine, Euclidean, and Dot-Product metrics.
- 🗄️ **LSM-Tree Storage Engine**: Sequential Write-Ahead Logging (WAL) + ConcurrentSkipList MemTable + Immutable SSTables with Bloom Filters & Sparse Index.
- 📑 **BM25 Full-Text Inverted Index**: Tokenization, term-frequency scoring, and rapid keyword search.
- ⚡ **In-Memory Hot LRFU Cache**: Zero-latency retrieval for hot rows and query results.
- 📡 **Real-Time Stream Engine**: Topic-based messaging and event streams built into database tables.
- 🤖 **Native AI SQL Functions**: Execute `AI_EMBED(text)`, `AI_RAG(prompt)`, `AI_SUMMARIZE(text)`, `AI_CLASSIFY(text, labels)` directly inside SQL statements.
- 🔒 **Enterprise Security & DLP**: Automated PII masking (SSNs, Credit Cards, Emails), Token Bucket firewall rate limiting, and RBAC authentication.
- 📸 **Disaster Recovery**: Atomic Point-in-Time snapshot backups and instant restoration.

---

## 🚀 Quickstart & One-Line Installers

### 🍏 macOS & Linux (One-Line Terminal Install)
```bash
curl -fsSL https://raw.githubusercontent.com/upendra-manike/CogniDB/main/deploy/mac/install_mac.sh | bash
```

### 🪟 Windows 10 / 11 & Windows Server (One-Line PowerShell Install)
Open PowerShell as Administrator:
```powershell
powershell -ExecutionPolicy Bypass -Command "iwr -useb https://raw.githubusercontent.com/upendra-manike/CogniDB/main/deploy/windows/install_windows.ps1 | iex"
```

### 🐧 AWS EC2 / Production Linux VM
```bash
curl -fsSL https://raw.githubusercontent.com/upendra-manike/CogniDB/main/deploy/aws_ec2_install.sh | bash
```

---

## 🔑 Default Credentials & Access Points

- **Username**: `admin`
- **Password**: `cognidb_secret_pass`
- **Connection URI**: `cognidb://admin:cognidb_secret_pass@localhost:8080/default`
- **Web Dashboard**: 👉 **[http://localhost:8080/](http://localhost:8080/)**
- **REST API**: `http://localhost:8080/api/sql`

---

## 💻 SQL Query Syntax & Examples

### 1. Create Table with Vector Column
```sql
CREATE TABLE products (
  id VARCHAR PRIMARY KEY, 
  name VARCHAR, 
  category VARCHAR, 
  price FLOAT, 
  description VARCHAR, 
  embedding FLOAT_VECTOR(128)
);
```

### 2. Insert Record with Automatic Vector Embedding
```sql
INSERT INTO products VALUES (
  'prod_101', 
  'MacBook Pro M3', 
  'Electronics', 
  1999.99, 
  'High performance Apple Silicon laptop for software engineering and AI.', 
  AI_EMBED('MacBook Pro M3 Apple Silicon software engineering laptop')
);
```

### 3. Hybrid SQL + Semantic Vector Similarity Query
```sql
SELECT id, name, price, description 
FROM products 
WHERE category = 'Electronics' AND price < 2500 
  AND embedding SIMILAR TO 'laptop for software engineering' 
TOP 5;
```

### 4. Native AI RAG (Retrieval-Augmented Generation) Query
```sql
SELECT AI_RAG('Summarize key developer laptops under $2000');
```

### 5. AI Text Summarization & Sentiment Classification
```sql
SELECT name, 
       AI_SUMMARIZE(description) AS summary, 
       AI_CLASSIFY(description, 'Hardware', 'Software', 'Service') AS category 
FROM products;
```

### 6. BM25 Full-Text Keyword Search Query
```sql
SELECT id, name, description 
FROM products 
WHERE MATCH(description, 'Apple Silicon software performance');
```

---

## 📡 REST API & Client Connection Code

### 🐍 Python SDK Connection Example
```python
import requests

API_URL = "http://localhost:8080/api/sql"
HEADERS = {"Authorization": "Bearer cogni_master_key_99", "Content-Type": "application/json"}

# Execute Hybrid Query
payload = {
    "sql": "SELECT id, name FROM products WHERE embedding SIMILAR TO 'high resolution monitor' TOP 3"
}

response = requests.post(API_URL, json=payload, headers=HEADERS)
print(response.json())
```

---

## 🛠️ Complete Documentation & Guides

For detailed architecture specs, disaster recovery guides, and configuration references:
- 📖 **[DOCUMENTATION.md](./DOCUMENTATION.md)**: Full Query & Architecture Reference Guide.
- ☁️ **[AWS EC2 & Cloud Deployment Guide](./deploy/aws_ec2_install.sh)**: Enterprise production installation script.

---

## 📜 License

CogniDB is open-source software licensed under the **[Apache License 2.0](./LICENSE)**.
