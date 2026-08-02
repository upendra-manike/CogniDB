# 🍃 CogniDB Spring Boot & JPA Integration Example

This example demonstrates how to integrate **CogniDB** into a Spring Boot 3 application using standard **Spring Data JPA**, `@Entity`, `@Repository`, `@Transactional`, and native CogniDB `@Query` vector similarity methods.

---

## 🛠️ How to Run

1. Make sure CogniDB is running locally:
   ```bash
   cognidb start
   ```

2. Navigate to this directory:
   ```bash
   cd examples/springboot-jpa
   ```

3. Run with Maven:
   ```bash
   mvn spring-boot:run
   ```

---

## 💻 Code Breakdown

- **`Product.java`**: Standard JPA `@Entity` mapped to CogniDB unified table.
- **`ProductRepository.java`**: Spring Data JPA repository extending `JpaRepository` with native CogniDB `@Query` annotations for `embedding SIMILAR TO` and `AI_RAG(...)`.
- **`ProductService.java`**: Spring `@Service` bean using standard `@Transactional` annotations.
