package com.shubham.flashsale.service;

import com.shubham.flashsale.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisStockService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ProductService productService;

    public Long decrementStock(Long productId, Integer quantity) {
        String key = "product:stock:" + productId;

        // 1. Safe Fallback: Only hit DB if Redis crashed/restarted
        Boolean hasKey = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(hasKey)) {
            Product product = productService.getProductById(productId);
            redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(product.getAvailableStock()));
        }

        // 2. Atomic Decrement
        Long remainingStock = redisTemplate.opsForValue().decrement(key, quantity);

        // 3. Null and Oversell Check
        if (remainingStock == null) {
            return -1L; // Failsafe if Redis drops connection
        }

        if (remainingStock < 0) {
            redisTemplate.opsForValue().increment(key, quantity); // Rollback immediately
            return -1L;
        }

        return remainingStock;
    }

    public void incrementStock(Long productId, Integer quantity) {
        redisTemplate.opsForValue().increment("product:stock:" + productId, quantity);
    }
    public void initializeStock(Long productId, Integer stock) {
        String key = "product:stock:" + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
    }
}