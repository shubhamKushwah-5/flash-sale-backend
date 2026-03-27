package com.shubham.flashsale.controller;

import com.shubham.flashsale.dto.PurchaseRequest;
import com.shubham.flashsale.dto.PurchaseResponse;
import com.shubham.flashsale.model.Order;
import com.shubham.flashsale.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Purchase with pessimistic locking
     * use this for high -contention scenarios (flash sales)
     */
    @PostMapping("/purchase-pessimistic")
    public ResponseEntity<PurchaseResponse> purchasePessimistic(
            @Valid @RequestBody PurchaseRequest request) {

        PurchaseResponse response = orderService.purchaseWithPessimisticLock(request);

        if(response.isSuccess()) {
            return ResponseEntity.ok(response);
        }else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Purchase with optimistic locking
     * using this for normal - contention scenarios like regular sales
     */
    @PostMapping("/purchase-optimistic")
    public ResponseEntity<PurchaseResponse> purchaseOptimistic (
            @Valid @RequestBody PurchaseRequest request) {

        PurchaseResponse response = orderService.purchaseWithOptimisticLock(request);

        if(response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user's order history
     */
    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    /**
     * Get successfull order count for analytics
     */
    @GetMapping("/product/{productId}/success-count")
    public ResponseEntity<Long> getSuccessCount(@PathVariable Long productId) {
        long count = orderService.getSuccessfulOrderCount(productId);
        return ResponseEntity.ok(count);
    }


}
