# ⚡ CogniDB Demo & Test Client Project

This project demonstrates real-time database connectivity and full **CRUD (Create, Read, Update, Delete)** operations against the **CogniDB AI-Native Database Engine**.

---

## 📁 Project Structure

```
cognidb-demo/
├── cognidb_sdk.py   # Official Python Client SDK for CogniDB REST API
├── main.py          # Real-time connectivity & CRUD test suite runner
└── README.md        # Documentation
```

---

## 🚀 How to Run the Real-Time Test Suite

### 1. Ensure CogniDB Server is Running
```bash
cognidb status
# If stopped, start it:
cognidb start
```

### 2. Run the Client Test Suite
```bash
python3 cognidb-demo/main.py
```

---

## 💻 Sample SDK Usage in Python

```python
from cognidb_sdk import CogniDBClient

# Connect to CogniDB Engine
db = CogniDBClient("http://localhost:8080")

# 1. Connectivity Check
is_ok, info = db.test_connection()
print(f"Connected to CogniDB! Active Nodes: {len(info['nodes'])}")

# 2. CREATE TABLE
db.execute_sql("""
CREATE TABLE items (
    id VARCHAR PRIMARY KEY,
    title VARCHAR,
    price DOUBLE,
    embedding FLOAT_VECTOR(128)
)
""")

# 3. INSERT Record with AI Embeddings
db.execute_sql("""
INSERT INTO items VALUES ('item_1', 'AI Workstation', 2499.0, AI_EMBED('Nvidia GPU Server'))
""")

# 4. SELECT Query
res = db.execute_sql("SELECT * FROM items WHERE price > 1000.0")
print(res['data'])

# 5. HNSW Vector Similarity Search
vector_results = db.search_vector("items", "embedding", "Deep Learning Hardware", top_k=3)
print(vector_results['data'])
```
