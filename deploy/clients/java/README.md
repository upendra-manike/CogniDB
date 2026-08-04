# ☕ SyntricDB Java Client & JDBC Driver

Official Java Client SDK and JDBC Driver for **SyntricDB**, the Next-Generation AI-Native Unified Database Engine.

## 📦 Maven Dependency

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.syntricdb</groupId>
    <artifactId>syntricdb-java-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🚀 Usage Example

### 1. Java Client SDK
```java
import com.syntricdb.client.SyntricDBClient;
import com.syntricdb.client.QueryResult;

public class Main {
    public static void main(String[] args) throws Exception {
        SyntricDBClient client = new SyntricDBClient("http://localhost:8080");

        // Execute SQL Query
        QueryResult result = client.query("SELECT * FROM users WHERE age > 25");
        System.out.println("Execution Time: " + result.getExecutionTimeMs() + " ms");
        System.out.println("Rows: " + result.getData());

        // HNSW Vector Search
        var vecResult = client.vectorSearch("users", "embedding", "Java Systems Architect", 3);
        System.out.println("Vector Matches: " + vecResult);
    }
}
```

## 📜 License
Apache License 2.0
