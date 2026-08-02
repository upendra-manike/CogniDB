# 📖 CogniDB Complete Technical Documentation & Reference Guide

Welcome to the official technical documentation for **CogniDB**, the next-generation AI-native unified database engine.

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

### 🐍 Python
```python
import requests

API_URL = "http://localhost:8080/api/sql"
payload = {"sql": "SELECT id, name FROM users WHERE embedding SIMILAR TO 'AI Architect' TOP 3"}
response = requests.post(API_URL, json=payload)
print(response.json())
```
