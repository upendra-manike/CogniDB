package com.example.cognidb.service;

import com.example.cognidb.entity.Product;
import com.example.cognidb.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getProductsByCategoryAndMaxPrice(String category, Double maxPrice) {
        return productRepository.findByCategoryAndPriceLessThan(category, maxPrice);
    }

    public List<Product> searchSimilarProducts(String category, String queryTerm, int limit) {
        return productRepository.searchByVectorSimilarity(category, queryTerm, limit);
    }

    public String askDatabaseRag(String userPrompt) {
        return productRepository.generateAIRagResponse(userPrompt);
    }
}
