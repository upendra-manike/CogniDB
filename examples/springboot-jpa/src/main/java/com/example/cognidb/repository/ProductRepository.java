package com.example.cognidb.repository;

import com.example.cognidb.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // 1. Standard JPA Derived Query Method
    List<Product> findByCategoryAndPriceLessThan(String category, Double price);

    // 2. Native CogniDB Hybrid SQL + Vector Similarity Query
    @Query(value = "SELECT * FROM products WHERE category = :category AND embedding SIMILAR TO :searchTerm TOP :limit", nativeQuery = true)
    List<Product> searchByVectorSimilarity(@Param("category") String category, 
                                           @Param("searchTerm") String searchTerm, 
                                           @Param("limit") int limit);

    // 3. Native In-Engine AI RAG Query
    @Query(value = "SELECT AI_RAG(:prompt)", nativeQuery = true)
    String generateAIRagResponse(@Param("prompt") String prompt);
}
