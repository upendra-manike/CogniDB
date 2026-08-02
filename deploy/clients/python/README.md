# 🐍 CogniDB Official Python SDK (`pip install cognidb-client`)

Official Python Client for **CogniDB**—the AI-Native Unified Database Engine.

---

## ⚡ Quick Start

```bash
pip install cognidb-client
```

```python
from cognidb import CogniDBClient

client = CogniDBClient(host="http://localhost:8080")

# Perform Hybrid SQL + Vector Search
results = client.vector_search(
    table="developers",
    query_text="LLM fine-tuning vector index",
    top_k=3,
    where_clause="experience_years > 4"
)

print(results)
```
