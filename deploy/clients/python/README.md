# 🐍 SyntricDB Official Python SDK (`pip install syntricdb-client`)

Official Python Client for **SyntricDB**—the AI-Native Unified Database Engine.

---

## ⚡ Quick Start

```bash
pip install syntricdb-client
```

```python
from syntricdb import SyntricDBClient

client = SyntricDBClient(host="http://localhost:8080")

# Perform Hybrid SQL + Vector Search
results = client.vector_search(
    table="developers",
    query_text="LLM fine-tuning vector index",
    top_k=3,
    where_clause="experience_years > 4"
)

print(results)
```
