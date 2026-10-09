package com.edstem.interviewprep.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

// Indexes back the common filters/sorts; a leading-wildcard name search (LIKE '%x%') can't use a B-tree
// index, which is the main thing to change (full-text index) if the catalog grows very large.
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category_price", columnList = "category, price"),
        @Index(name = "idx_products_price", columnList = "price"),
        @Index(name = "idx_products_created_at", columnList = "createdAt")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    // BigDecimal, never double, for money: no binary rounding errors.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private double rating;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Product() {
        // for JPA
    }

    public Product(String name, String category, BigDecimal price, int stock, double rating) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.rating = rating;
    }

    public void update(String name, String category, BigDecimal price, int stock, double rating) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public double getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
