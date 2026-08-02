# 🎓 CogniDB Step-by-Step Interactive Tutorial

Welcome to the **CogniDB Hands-On Tutorial**! This step-by-step guide will take you from zero to running advanced hybrid vector SQL queries, AI RAG prompts, full-text searches, and disaster recovery snapshots.

---

## 📌 Prerequisites
Make sure **CogniDB** is installed and running on your system:
- **macOS / Linux**: `cognidb start`
- **Windows**: `cognidb start` (in PowerShell / Command Prompt)
- **Verify Web Dashboard**: Open [http://localhost:8080/](http://localhost:8080/) in your browser.

---

## 🚀 Step 1: Launch the Interactive CLI Shell

Open your terminal and run:

```bash
cognidb cli
```

You will see the CogniDB interactive prompt:
```text
==================================================================
⚡ CogniDB Interactive CLI Shell (v1.0.0) ⚡
Type 'exit;' or 'quit;' to exit. Type 'help;' for commands.
==================================================================
cognidb> 
```

---

## 🗄️ Step 2: Create Your First AI-Native Table

Let's create a `products` table supporting relational columns, full-text search, and a **128-dimensional vector embedding column**.

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

---

## 📝 Step 3: Insert Records with Automatic AI Vector Embeddings

Insert product records into your table. Notice how `AI_EMBED('text')` automatically generates 128-dimensional vector embeddings on-the-fly!

```sql
INSERT INTO products VALUES (
  'prod_101', 
  'MacBook Pro M3', 
  'Electronics', 
  1999.99, 
  'High performance Apple Silicon laptop for software engineering and AI.', 
  AI_EMBED('MacBook Pro M3 Apple Silicon software engineering laptop')
);

INSERT INTO products VALUES (
  'prod_102', 
  'Sony WH-1000XM5', 
  'Electronics', 
  399.99, 
  'Industry leading noise cancelling wireless headphones for music and calls.', 
  AI_EMBED('Sony noise cancelling headphones wireless audio')
);

INSERT INTO products VALUES (
  'prod_103', 
  'Dell UltraSharp 27 Monitor', 
  'Electronics', 699.99, 
  '4K USB-C hub display for coding and graphic design.', 
  AI_EMBED('Dell 4K monitor USB-C hub display coding design')
);
```

---

## 🔍 Step 4: Run Hybrid SQL + Semantic Vector Similarity Queries

Combine standard SQL relational filters (`WHERE price < 2500`) with semantic vector similarity search (`embedding SIMILAR TO ...`):

```sql
SELECT id, name, price, description 
FROM products 
WHERE category = 'Electronics' AND price < 2500 
  AND embedding SIMILAR TO 'developer laptop for AI' 
TOP 2;
```

---

## 🤖 Step 5: Execute In-Engine AI RAG & Summarization

### 1. In-Engine RAG (Retrieval-Augmented Generation)
Ask CogniDB to synthesize answers from your stored data:
```sql
SELECT AI_RAG('Which products are best suited for software developers?');
```

### 2. AI Text Summarization & Sentiment Classification
Generate summaries and auto-classify descriptions on-the-fly:
```sql
SELECT name, 
       AI_SUMMARIZE(description) AS summary, 
       AI_CLASSIFY(description, 'Hardware', 'Software', 'Audio') AS category 
FROM products;
```

---

## 📑 Step 6: Perform Full-Text Keyword Search (BM25 Engine)

Use the built-in BM25 inverted index engine to search documents by keyword relevance:

```sql
SELECT id, name, description 
FROM products 
WHERE MATCH(description, 'noise cancelling audio');
```

---

## 📡 Step 7: Publish Real-Time Stream Events

CogniDB tables support real-time pub-sub event topics out-of-the-box:

```sql
PUBLISH topic 'store_activity' MESSAGE 'User 42 purchased prod_101';
```

---

## 📸 Step 8: Create & Restore Disaster Recovery Snapshots

### 1. Take a Point-in-Time Snapshot
```sql
SNAPSHOT CREATE;
```
*Output*: `✅ Snapshot created successfully at: ~/.cognidb/snapshots/cognidb_snapshot_20260802_...`

### 2. Restore Database State
```sql
SNAPSHOT RESTORE 'cognidb_snapshot_20260802_...';
```

---

## 🐍 Step 9: Connect via Python (REST API)

Create a file named `cognidb_demo.py` and run it:

```python
import requests

API_URL = "http://localhost:8080/api/sql"
HEADERS = {"Content-Type": "application/json"}

# 1. Execute Hybrid Query
query_payload = {
    "sql": "SELECT id, name, price FROM products WHERE embedding SIMILAR TO '4K display' TOP 2"
}

response = requests.post(API_URL, json=query_payload, headers=HEADERS)
print("Response:", response.json())
```

Run the Python script:
```bash
python3 cognidb_demo.py
```

---

## 🎉 Congratulations!

You have completed the **CogniDB Core Tutorial**! You now know how to:
- Build hybrid vector + relational schemas.
- Run vector similarity searches with $O(\log N)$ HNSW indexing.
- Use AI RAG and text processing natively inside SQL.
- Connect via CLI, Web Studio, and Python REST API.
