# ⚡ CogniDB: Next-Generation AI-Native Unified Database Engine

[![Build Status](https://github.com/upendra-manike/CogniDB/workflows/CogniDB%20CI%20Workflow/badge.svg)](https://github.com/upendra-manike/CogniDB/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://jdk.java.net/21/)

**CogniDB** is a distributed, high-performance, AI-native unified database engine built from the ground up on **Java 21 LTS, Netty 4, HNSW Vector Indexing, LSM-Tree Storage, BM25 Full-Text Search, and Raft Consensus**.

Instead of stitching together PostgreSQL, Redis, Elasticsearch, Kafka, and Pinecone over high-latency network boundaries, **CogniDB unifies SQL, Vector Search, In-Memory Caching, Streaming, Full-Text Search, and Built-In AI SQL Functions into ONE engine**.

---

## 📚 Guides & Documentation

- 🎓 **[Step-by-Step Interactive Tutorial](./TUTORIAL.md)**: Hands-on guide from zero to running AI vector queries.
- 📖 **[Complete Technical Documentation](./DOCUMENTATION.md)**: Full Query & Architecture Reference Guide.
- ☁️ **[AWS EC2 & Cloud Deployment Guide](./deploy/aws_ec2_install.sh)**: Enterprise production installation script.

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

---

## 📜 License

CogniDB is open-source software licensed under the **[Apache License 2.0](./LICENSE)**.
