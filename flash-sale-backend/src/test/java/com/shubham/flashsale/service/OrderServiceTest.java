package com.shubham.flashsale.service;

import com.shubham.flashsale.dto.PurchaseRequest;
import com.shubham.flashsale.dto.PurchaseResponse;
import com.shubham.flashsale.model.Order;
import com.shubham.flashsale.model.OrderStatus;
import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.repository.OrderRepository;
import com.shubham.flashsale.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test product before each test
        testProduct = new Product(
                "Test Concert Ticket",
                new BigDecimal("1000"),
                10 // 10 available
        );
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testPurchaseWithPessimisticLock_Success() {
        // Given
        PurchaseRequest request = new PurchaseRequest(
                testProduct.getId(),
                1001l, // userId
                2           // quantity
        );

        // When
        PurchaseResponse response = orderService.purchaseWithPessimisticLock(request);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getOrderId());
        assertEquals(OrderStatus.SUCCESS , response.getStatus());

        // Verify stock decremented
        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(8, updated.getAvailableStock());

        // Verify order created
        Order order = orderRepository.findById(response.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.SUCCESS, order.getStatus());
        assertEquals(2, order.getQuantity());
    }

    @Test
    void testPurchaseWithPessimisticLock_InsufficientStock() {
        // Given - try to buy more than available
        PurchaseRequest request = new PurchaseRequest(
                testProduct.getId(),
                1002L,
                15  // More than 10 available
        );

        // When
        PurchaseResponse response = orderService.purchaseWithPessimisticLock(request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Insufficient stock", response.getMessage());
        assertEquals(OrderStatus.FAILED, response.getStatus());

        // Verify stock unchanged
        Product unchanged = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(10, unchanged.getAvailableStock());

        // Verify failed order was recorded
        List<Order> failedOrders = orderRepository.findByStatus(OrderStatus.FAILED);
        assertFalse(failedOrders.isEmpty());
    }

    @Test
    void testGetUserOrders() {
        // Given - create multiple orders for same user
        Long userId = 2001L;

        PurchaseRequest req1 = new PurchaseRequest(testProduct.getId(), userId, 2);
        PurchaseRequest req2 = new PurchaseRequest(testProduct.getId(), userId, 3);

        orderService.purchaseWithPessimisticLock(req1);
        orderService.purchaseWithPessimisticLock(req2);

        // When
        List<Order> userOrders = orderService.getUserOrders(userId);

        // Then
        assertEquals(2, userOrders.size());
        assertTrue(userOrders.stream().allMatch(o -> o.getUserId().equals(userId)));
    }

    @Test
    void testGetSuccessfulOrderCount() {
        // Given - create successful and failed orders
        orderService.purchaseWithPessimisticLock(
                new PurchaseRequest(testProduct.getId(), 3001L, 2)
        );  // Success

        orderService.purchaseWithPessimisticLock(
                new PurchaseRequest(testProduct.getId(), 3002L, 3)
        );  // Success

        orderService.purchaseWithPessimisticLock(
                new PurchaseRequest(testProduct.getId(), 3003L, 20)
        );  // Fail - insufficient stock

        // When
        long successCount = orderService.getSuccessfulOrderCount(testProduct.getId());

        // Then
        assertEquals(2, successCount);
    }
}
