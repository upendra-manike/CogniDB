package com.example.syntricdb.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;
    
    private String name;
    private String category;
    private Double price;
    
    @Column(length = 1000)
    private String description;

    public Product() {}

    public Product(String id, String name, String category, Double price, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
