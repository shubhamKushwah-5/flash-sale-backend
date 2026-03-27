package com.shubham.flashsale.repository;

import com.shubham.flashsale.model.Order;
import com.shubham.flashsale.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    long countByProductIdAndStatus(Long productId, OrderStatus status);
}
