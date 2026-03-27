package com.shubham.flashsale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PurchaseRequest {

    @NotNull(message = "Product ID  is required")
    private Long productId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    //Constructor
    public PurchaseRequest(){}

    public PurchaseRequest(Long productId, Long userId, Integer quantity) {
        this.productId = productId;
        this.userId = userId;
        this.quantity = quantity;
    }

    // Getters and setters
    public Long getProductId(){return productId;}
    public void setProductId(Long ProductId){ this.productId = productId;}

    public Long getUserId(){ return userId;}
    public void setUserId(Long userId){ this.userId = userId;}

    public Integer getQuantity() { return quantity;}
    public void setQuantity(Integer quantity){ this.quantity = quantity;}
}
