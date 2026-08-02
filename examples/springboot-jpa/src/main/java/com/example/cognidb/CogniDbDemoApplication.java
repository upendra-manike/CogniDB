package com.example.cognidb;

import com.example.cognidb.entity.Product;
import com.example.cognidb.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CogniDbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CogniDbDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner runDemo(ProductService productService) {
        return args -> {
            System.out.println("=================================================");
            System.out.println("🚀 CogniDB Spring Boot & JPA Integration Demo");
            System.out.println("=================================================");

            // 1. Create & Insert Record via JPA Entity Manager
            Product laptop = new Product(
                "prod_301",
                "MacBook Pro M3 Max",
                "Electronics",
                3499.99,
                "Ultimate Apple Silicon developer laptop for machine learning and AI databases."
            );
            productService.createProduct(laptop);
            System.out.println("✅ Inserted Product via JPA: " + laptop.getName());

            // 2. Perform Native HNSW Vector Similarity Search inside Spring Data Repository
            List<Product> matches = productService.searchSimilarProducts("Electronics", "machine learning laptop", 1);
            System.out.println("\n🔍 CogniDB Native Vector Search Result:");
            for (Product p : matches) {
                System.out.println("   -> ID: " + p.getId() + " | Name: " + p.getName() + " | Price: $" + p.getPrice());
            }

            // 3. Perform Native In-Engine RAG Query
            String ragAnswer = productService.askDatabaseRag("What is the best laptop for software developers?");
            System.out.println("\n🤖 CogniDB Native AI RAG Response:");
            System.out.println("   " + ragAnswer);

            System.out.println("=================================================");
        };
    }
}
