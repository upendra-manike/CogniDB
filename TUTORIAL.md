# 🎓 CogniDB Step-by-Step Interactive Tutorial

Welcome to the **CogniDB Hands-On Tutorial**! This step-by-step guide will take you from zero to running advanced hybrid vector SQL queries, ACID transactions, AI RAG prompts, full-text searches, and disaster recovery snapshots across multiple backend programming languages.

> 🎬 **[Watch the Official CogniDB Studio Video Demo on YouTube](https://www.youtube.com/watch?v=8xxpJwloe30)**

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
  'Electronics', 
  699.99, 
  '4K USB-C hub display for coding and graphic design.', 
  AI_EMBED('Dell 4K monitor USB-C hub display coding design')
);
```

---

## 🛡️ Step 4: Execute ACID Transactions (Begin, Commit, Rollback)

CogniDB supports full **ACID transactions** with optimistic concurrency control (OCC) and write-ahead logging (WAL):

```sql
-- 1. Begin Atomic Transaction
BEGIN TRANSACTION;

-- 2. Stage Multiple Operations
INSERT INTO products VALUES (
  'prod_104', 
  'Keychron K2 Mechanical Keyboard', 
  'Peripherals', 
  89.99, 
  'Wireless mechanical keyboard with tactile switches for coding.', 
  AI_EMBED('mechanical keyboard tactile wireless coding')
);

UPDATE products SET price = 84.99 WHERE id = 'prod_104';

-- 3. Commit All Staged Modifications Atomically to WAL & MemTable
COMMIT;
```

If an error or conflict occurs, safely roll back:
```sql
ROLLBACK;
```

---

## 🔍 Step 5: Run Hybrid SQL + Semantic Vector Similarity Queries

Combine standard SQL relational filters (`WHERE price < 2500`) with semantic vector similarity search (`embedding SIMILAR TO ...`):

```sql
SELECT id, name, price, description 
FROM products 
WHERE category = 'Electronics' AND price < 2500 
  AND embedding SIMILAR TO 'developer laptop for AI' 
TOP 2;
```

---

## 🤖 Step 6: Execute In-Engine AI RAG & Summarization

### 1. In-Engine RAG (Retrieval-Augmented Generation)
Ask CogniDB to synthesize answers from your stored data:
```sql
SELECT AI_RAG('Which products are best suited for software developers?');
```

---

## 📸 Step 7: Create & Restore Disaster Recovery Snapshots

### 1. Take a Point-in-Time Snapshot
```sql
SNAPSHOT CREATE;
```

### 2. Restore Database State
```sql
SNAPSHOT RESTORE 'cognidb_snapshot_20260802_...';
```

---

## 🎉 Summary

You have completed the **CogniDB Tutorial**! You now know how to execute **ACID transactions**, run vector similarity queries, and manage database snapshots!
