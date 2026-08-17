package com.shubham.flashsale.service;

import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.repository.OrderRepository;
import com.shubham.flashsale.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestResetService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisStockService redisStockService;

    @Transactional
    public void resetEntireSystem() {
        System.out.println("--- STARTING SYSTEM RESET ---");

        // 1. Wipe ALL orders
        orderRepository.deleteAllInBatch();
        System.out.println("SUCCESS: Wiped all orders from database.");

        // 2. Reset stock and version directly in MySQL
        productRepository.resetAllStockAndVersion();
        System.out.println("SUCCESS: Forced MySQL to reset stock and version.");

        // 3. Fetch products to sync Redis
        List<Product> allProducts = productRepository.findAll();

        for (Product product : allProducts) {
            // Failsafe: if totalStock is null for some reason, default to 500
            Integer stockToSet = (product.getTotalStock() != null) ? product.getTotalStock() : 500;

            System.out.println("SYNCING REDIS -> Product ID: " + product.getId() + " | Stock: " + stockToSet);
            redisStockService.initializeStock(product.getId(), stockToSet);
        }

        System.out.println("--- SYSTEM RESET COMPLETE ---");
    }
}
