# ⚡ CogniDB: Next-Generation AI-Native Unified Database Engine

[![Build Status](https://github.com/upendra-manike/CogniDB/workflows/CogniDB%20CI%20Workflow/badge.svg)](https://github.com/upendra-manike/CogniDB/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://jdk.java.net/21/)
[![YouTube Demo](https://img.shields.io/badge/YouTube-Watch%20Demo-red?logo=youtube)](https://www.youtube.com/watch?v=8xxpJwloe30)

> 🎬 **[Watch the Official CogniDB Studio Video Demo on YouTube](https://www.youtube.com/watch?v=8xxpJwloe30)**

**CogniDB** is a distributed, high-performance, AI-native unified database engine built from the ground up on **Java 21 LTS, Netty 4, HNSW Vector Indexing, LSM-Tree Storage, BM25 Full-Text Search, and Raft Consensus**.

Instead of stitching together PostgreSQL, Redis, Elasticsearch, Kafka, and Pinecone over high-latency network boundaries, **CogniDB unifies SQL, Vector Search, In-Memory Caching, Streaming, Full-Text Search, and Built-In AI SQL Functions into ONE engine**.

---

## 📚 Guides, Documentation & Multi-Language Examples

- 💻 **[Multi-Language Code Examples Repository](https://github.com/upendra-manike/CogniDB_Examples)**: Dedicated repo for **Spring Boot JPA, Python, Node.js, Go, C#, Rust, and cURL**.
- 🍃 **[Spring Boot & JPA Integration Example](https://github.com/upendra-manike/CogniDB_Examples/tree/main/springboot-jpa)**: Connect via `@Entity`, `@Repository`, and `@Transactional`.
- 🐍 **[Python Integration Example](https://github.com/upendra-manike/CogniDB_Examples/tree/main/python)**: Connect via REST API using Python `requests`.
- 💚 **[Node.js Integration Example](https://github.com/upendra-manike/CogniDB_Examples/tree/main/nodejs)**: Connect via JavaScript async fetch.
- 🔷 **[Go Integration Example](https://github.com/upendra-manike/CogniDB_Examples/tree/main/go)**: Connect via Golang `net/http`.
- 💜 **[C# / .NET 8 Integration Example](https://github.com/upendra-manike/CogniDB_Examples/tree/main/csharp)**: Connect via C# `HttpClient`.
- 🦀 **[Rust Integration Example](https://github.com/upendra-manike/CogniDB_Examples/tree/main/rust)**: Connect via Rust `tokio` and `reqwest`.
- 🤖 **[AI Agent Specification File (`llms.txt`)](./llms.txt)**: Standard machine-readable AI context file for LLMs & AI coding assistants.
- 🌟 **[Awesome Lists Submission Kit](./AWESOME_LISTS_SUBMISSION_KIT.md)**: PR templates for `awesome-java`, `awesome-vector-search`, and `awesome-database`.
- 🏷️ **[GitHub Metadata & AI SEO Guide](./GITHUB_REPOS_METADATA.md)**: Topic tags and SEO metadata optimization.
- 🎬 **[Official YouTube Video Walkthrough](https://www.youtube.com/watch?v=8xxpJwloe30)**: Watch the full feature demo.
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

## 🍃 Spring Boot & Spring Data JPA Example

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // Native CogniDB Vector Search inside Spring Data Repository!
    @Query(value = "SELECT * FROM products WHERE category = :cat AND embedding SIMILAR TO :term TOP :limit", nativeQuery = true)
    List<Product> searchByVectorSimilarity(@Param("cat") String category, 
                                           @Param("term") String searchTerm, 
                                           @Param("limit") int limit);
}
```

---

## 📜 License

CogniDB is open-source software licensed under the **[Apache License 2.0](./LICENSE)**.
