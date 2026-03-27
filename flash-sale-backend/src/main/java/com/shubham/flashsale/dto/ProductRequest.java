package com.shubham.flashsale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;

    //Constructor
    public ProductRequest(){}

    public ProductRequest(String name, BigDecimal price, Integer stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // Getters and Setters
    public String getName(){return name;}
    public void setName(String name ) {this.name = name;}

    public BigDecimal getPrice(){ return price;}
    public void setPrice(BigDecimal price) {this.price = price;}

    public Integer getStock(){return stock;}
    public void setStock(Integer stock) {this.stock = stock;}
}
