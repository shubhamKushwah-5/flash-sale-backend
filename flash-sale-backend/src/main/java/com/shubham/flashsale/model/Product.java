package com.shubham.flashsale.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal price;

    @Column(name = "total_stock")
    private Integer totalStock;

    @Column(name = "available_stock")
    private Integer availableStock;

    @Version // for optimistic locking
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructor

    public Product() {}

    public Product(String name, BigDecimal price, Integer stock) {
        this.name = name;
        this.price = price;
        this.totalStock = stock;
        this.availableStock = stock;
    }

    // Getters and Setters

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public BigDecimal getPrice() {return price;}
    public void setPrice(BigDecimal price) {this.price = price;}

    public Integer getTotalStock() {return totalStock;}
    public void setTotalStock(Integer totalStock) {this.totalStock = totalStock;}

    public Integer getAvailableStock() {return availableStock;}
    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Long getVersion() {return version;}
    public void setVersion(Long version) {this.version = version;}

    public LocalDateTime getCreatedAt() {return createdAt;}


}
