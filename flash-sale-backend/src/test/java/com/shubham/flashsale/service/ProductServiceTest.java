package com.shubham.flashsale.service;

import com.shubham.flashsale.dto.ProductRequest;
import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // so Rollback after each test
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testCreateProduct() {
        // Given
        ProductRequest request = new ProductRequest(
                "Test Concert Ticket",
                new BigDecimal("1000"),
                100
        );

        // When
        Product created = productService.createProduct(request);

        // Then
        assertNotNull(created.getId());
        assertEquals("Test Concert Ticket", created.getName());
        assertEquals(new BigDecimal("1000"), created.getPrice());
        assertEquals(100, created.getTotalStock());
        assertEquals(100, created.getAvailableStock());

    }

    @Test
    void testDecrementStockPessimistic_Success() {
        // Giben - create product with 10 stock
        Product product = new Product("Test", new BigDecimal("100"), 10);
        product = productRepository.save(product);

        // When - decrement by 3
        boolean result = productService.decrementStockPessimistic(product.getId(), 3 );

        // Then
        assertTrue(result);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, updated.getAvailableStock());

    }

    @Test
    void testDecrementStockOptimistic_Success() {
        // Given
        Product product = new Product("Test", new BigDecimal("100"), 10);
        product = productRepository.save(product);

        // When
        boolean result = productService.decrementStockOptimistic(product.getId(), 3);

        // Then
        assertTrue(result);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, updated.getAvailableStock());
    }
}
