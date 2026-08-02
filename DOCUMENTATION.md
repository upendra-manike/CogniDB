# 📖 CogniDB Complete Technical Documentation & Reference Guide

Welcome to the official technical documentation for **CogniDB**, the next-generation AI-native unified database engine.

> 🎬 **[Watch the Official CogniDB Studio Video Demo on YouTube](https://www.youtube.com/watch?v=8xxpJwloe30)**

---

## 📚 Table of Contents

1. [Architectural Principles & Engines](#1-architectural-principles--engines)
2. [Connection String & Security](#2-connection-string--security)
3. [ACID Transaction Management](#3-acid-transaction-management)
4. [Spring Boot & JPA Integration Guide](#4-spring-boot--jpa-integration-guide)
5. [Complete SQL & AI Query Reference](#5-complete-sql--ai-query-reference)
6. [Backend Language SDK Integration (Python, Java, Go, Node.js, C#)](#6-backend-language-sdk-integration)
7. [Storage Engine & Recovery Mechanics](#7-storage-engine--recovery-mechanics)
8. [Distributed Consensus & Anti-Entropy](#8-distributed-consensus--anti-entropy)

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
```text
cognidb://<username>:<password>@<host>:<port>/<database>
```

---

## 3. ACID Transaction Management

CogniDB provides full **ACID** transactions with optimistic concurrency control (OCC) and write-ahead logging (WAL):

```sql
BEGIN TRANSACTION;
INSERT INTO products VALUES ('prod_201', 'Logitech MX Master 3S', 'Peripherals', 99.99, 'Ergonomic mouse', AI_EMBED('wireless mouse'));
COMMIT;
```

---

## 4. Spring Boot & JPA Integration Guide

CogniDB seamlessly integrates with **Spring Boot**, **Spring Data JPA**, **Hibernate**, and **Spring JdbcTemplate**.

### 🌟 How CogniDB Transforms Spring Boot Development:
1. **Replaces 4 Database Starters with 1**: Eliminate `spring-boot-starter-data-redis`, `kafka-template`, and `pinecone-client` dependencies.
2. **Native `@Query` Vector Searching**: Run semantic vector similarity search directly inside standard Spring Data Repositories.
3. **Standard `@Transactional` Support**: Spring's `@Transactional` annotation works out-of-the-box with CogniDB's transaction manager.

---

### 💻 Step-by-Step Spring Boot Implementation:

#### 1. `application.properties` Config
```properties
# Spring Boot CogniDB Data Source
spring.datasource.url=jdbc:cognidb://localhost:8080/default
spring.datasource.username=admin
spring.datasource.password=cognidb_secret_pass
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

#### 2. JPA Entity (`Product.java`)
```java
package com.example.cognidbdemo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;
    private String name;
    private String category;
    private Double price;
    private String description;

    // Constructors, Getters & Setters
    public Product() {}
    public Product(String id, String name, String category, Double price, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }
}
```

#### 3. Spring Data Repository with Native Vector Query (`ProductRepository.java`)
```java
package com.example.cognidbdemo.repository;

import com.example.cognidbdemo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // Standard JPA Derived Method
    List<Product> findByCategoryAndPriceLessThan(String category, Double price);

    // Native CogniDB Hybrid SQL + Vector Similarity Query
    @Query(value = "SELECT * FROM products WHERE category = :category AND embedding SIMILAR TO :searchTerm TOP :limit", nativeQuery = true)
    List<Product> searchByVectorSimilarity(@Param("category") String category, 
                                           @Param("searchTerm") String searchTerm, 
                                           @Param("limit") int limit);

    // Native CogniDB In-Engine AI RAG Query
    @Query(value = "SELECT AI_RAG(:prompt)", nativeQuery = true)
    String generateAIRagResponse(@Param("prompt") String prompt);
}
```

#### 4. Spring Boot Service Layer (`ProductService.java`)
```java
package com.example.cognidbdemo.service;

import com.example.cognidbdemo.entity.Product;
import com.example.cognidbdemo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> findSimilarProducts(String query) {
        return productRepository.searchByVectorSimilarity("Electronics", query, 5);
    }
}
```

---

## 5. Complete SQL & AI Query Reference

### 🔹 DDL Statements
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

## 6. Backend Language SDK Integration

> 💻 **Official Multi-Language Code Examples Repository**: 👉 **[https://github.com/upendra-manike/CogniDB_Examples](https://github.com/upendra-manike/CogniDB_Examples)**

### 🍃 Spring Boot 3 & JPA
CogniDB connects via standard PostgreSQL JDBC driver with Spring Data JPA entities and custom repository vector methods:

```java
// ProductEntity.java
@Entity
@Table(name = "products")
public class Product {
    @Id private String id;
    private String name;
    private String category;
    private Double price;
    private String description;
    // getters & setters
}

// ProductRepository.java
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    @Query(value = "SELECT * FROM products WHERE category = :category AND embedding SIMILAR TO :searchTerm TOP :limit", nativeQuery = true)
    List<Product> searchByVectorSimilarity(@Param("category") String category, 
                                           @Param("searchTerm") String searchTerm, 
                                           @Param("limit") int limit);
}
```

---

### 🐍 Python
```python
import requests
import json

COGNIDB_URL = "http://localhost:8080/api/sql"

def execute_query(sql):
    response = requests.post(COGNIDB_URL, json={"sql": sql})
    return response.json()

# 1. Insert with Auto AI Embeddings
execute_query("""
    INSERT INTO developers VALUES (
        'dev_401', 'Alice Johnson', 'Senior AI Engineer', 8,
        AI_EMBED('Senior AI Engineer PyTorch LLM fine-tuning vector index')
    );
""")

# 2. Hybrid SQL + HNSW Vector Search
results = execute_query("""
    SELECT id, name, role FROM developers 
    WHERE experience_years > 4 AND embedding SIMILAR TO 'LLM fine tuning' TOP 1;
""")
print(json.dumps(results, indent=2))
```

---

### 💚 Node.js / JavaScript
```javascript
const COGNIDB_URL = 'http://localhost:8080/api/sql';

async function queryCogniDB(sql) {
    const response = await fetch(COGNIDB_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql })
    });
    return await response.json();
}

// Perform Vector Search
queryCogniDB(`
    SELECT id, name, region, latency_ms FROM node_services 
    WHERE region = 'us-east-1' AND embedding SIMILAR TO 'authentication microservice' TOP 1;
`).then(console.log);
```

---

### 🔷 Go (Golang)
```go
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
)

func executeQuery(sql string) (string, error) {
	body, _ := json.Marshal(map[string]string{"sql": sql})
	resp, err := http.Post("http://localhost:8080/api/sql", "application/json", bytes.NewBuffer(body))
	if err != nil { return "", err }
	defer resp.Body.Close()
	res, _ := io.ReadAll(resp.Body)
	return string(res), nil
}

func main() {
	query := "SELECT id, metric_name, value FROM go_metrics WHERE embedding SIMILAR TO 'cpu load alert' TOP 1;"
	result, _ := executeQuery(query)
	fmt.Println(result)
}
```

---

### 💜 C# / .NET 9
```csharp
using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

class Program {
    private static readonly HttpClient client = new HttpClient();

    static async Task Main() {
        var payload = JsonSerializer.Serialize(new { sql = "SELECT id, event_type FROM dotnet_events WHERE embedding SIMILAR TO 'connection error' TOP 1;" });
        var response = await client.PostAsync("http://localhost:8080/api/sql", new StringContent(payload, Encoding.UTF8, "application/json"));
        Console.WriteLine(await response.Content.ReadAsStringAsync());
    }
}
```

---

### 🦀 Rust
```rust
use serde::Serialize;
use std::error::Error;

#[derive(Serialize)]
struct QueryRequest<'a> { sql: &'a str }

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    let client = reqwest::Client::new();
    let body = QueryRequest { sql: "SELECT id, temp FROM rust_sensors WHERE embedding SIMILAR TO 'temperature warning' TOP 1;" };
    let res = client.post("http://localhost:8080/api/sql").json(&body).send().await?.text().await?;
    println!("{}", res);
    Ok(())
}
```

---

### 💻 cURL / HTTP REST
```bash
curl -X POST "http://localhost:8080/api/sql" \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT id, log_level, message FROM curl_logs WHERE log_level=\"ERROR\" AND embedding SIMILAR TO \"memory allocation failure\" TOP 1;"}'
```
