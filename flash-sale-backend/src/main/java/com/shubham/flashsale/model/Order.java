package com.shubham.flashsale.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "user_id")
    private Long userId;

    private Integer quantity;

    @Column(name = "total_price")
    private BigDecimal totalPrice ;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @PrePersist
    protected void onCreate() {
        orderTime = LocalDateTime.now();
    }

    // Constructors
    public Order() {}

    public Order(Long productId, Long userId, Integer quantity,
                 BigDecimal totalPrice, OrderStatus status){
        this.productId = productId;
        this.userId = userId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    public Long getProductId() {return productId;}
    public void setProductId(Long productId) {this.productId = productId;}

    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public Integer getQuantity() {return quantity;}
    public void setQuantity(Integer quantity) {this.quantity = quantity;}

    public BigDecimal getTotalPrice(){return totalPrice;}
    public void setTotalPrice(BigDecimal totalPrice){
        this.totalPrice = totalPrice;
    }

    public OrderStatus getStatus() {return status;}
    public void setStatus(OrderStatus status){this.status = status;}

    public LocalDateTime getOrderTime() {return orderTime;}
}
