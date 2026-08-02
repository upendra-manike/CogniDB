# 💚 CogniDB Official Node.js SDK (`npm install cognidb-client`)

Official Node.js Client for **CogniDB**—the AI-Native Unified Database Engine.

[![npm version](https://img.shields.io/npm/v/cognidb-client.svg)](https://www.npmjs.com/package/cognidb-client)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

CogniDB unifies relational SQL queries, HNSW vector similarity search (`SIMILAR TO`), in-memory key-value caching, and in-engine AI RAG into a single low-latency engine.

---

## ⚡ Quick Start

### 1. Installation

```bash
npm install cognidb-client
```

### 2. Usage Example

```javascript
const { CogniDBClient } = require('cognidb-client');

// Initialize CogniDB Client
const client = new CogniDBClient({ host: 'http://localhost:8080' });

async function runDemo() {
    // 1. Execute SQL Query
    const createRes = await client.query(`
        CREATE TABLE IF NOT EXISTS node_services (
            id VARCHAR PRIMARY KEY,
            name VARCHAR,
            region VARCHAR,
            latency_ms FLOAT,
            embedding FLOAT_VECTOR(128)
        );
    `);
    console.log('Table Created:', createRes);

    // 2. Perform HNSW Vector Similarity Search
    const searchResults = await client.vectorSearch(
        'node_services',
        'authentication microservice',
        1,
        "region = 'us-east-1'"
    );
    console.log('Vector Search Results:', searchResults);
}

runDemo().catch(console.error);
```

---

## 🔗 Useful Links

- 🐙 **GitHub Repository**: [https://github.com/upendra-manike/CogniDB](https://github.com/upendra-manike/CogniDB)
- 💻 **Multi-Language Examples**: [https://github.com/upendra-manike/CogniDB_Examples](https://github.com/upendra-manike/CogniDB_Examples)
- 🎬 **Video Walkthrough**: [https://www.youtube.com/watch?v=8xxpJwloe30](https://www.youtube.com/watch?v=8xxpJwloe30)
- 📖 **Technical Documentation**: [CogniDB Documentation](https://github.com/upendra-manike/CogniDB/blob/main/DOCUMENTATION.md)
