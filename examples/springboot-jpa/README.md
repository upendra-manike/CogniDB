# 🍃 SyntricDB Spring Boot & JPA Integration Example

This example demonstrates how to integrate **SyntricDB** into a Spring Boot 3 application using standard **Spring Data JPA**, `@Entity`, `@Repository`, `@Transactional`, and native SyntricDB `@Query` vector similarity methods.

---

## 🛠️ How to Run

1. Make sure SyntricDB is running locally:
   ```bash
   syntricdb start
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

- **`Product.java`**: Standard JPA `@Entity` mapped to SyntricDB unified table.
- **`ProductRepository.java`**: Spring Data JPA repository extending `JpaRepository` with native SyntricDB `@Query` annotations for `embedding SIMILAR TO` and `AI_RAG(...)`.
- **`ProductService.java`**: Spring `@Service` bean using standard `@Transactional` annotations.
