package com.shubham.flashsale.service;

import com.shubham.flashsale.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RedisStockService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private DefaultRedisScript<Long> decrementScript;

    @PostConstruct
    public void init() {
        // Lua script: strictly atomic
        String lua =
                "if redis.call('exists', KEYS[1]) == 0 then return -1 end " +
                        "local stock = tonumber(redis.call('get', KEYS[1])) " +
                        "local qty = tonumber(ARGV[1]) " +
                        "if stock >= qty then return redis.call('decrby', KEYS[1], qty) " +
                        "else return -2 end";

        this.decrementScript = new DefaultRedisScript<>(lua, Long.class);
    }

    public Long decrementStock(Long productId, Integer quantity) {
        String key = "product:stock:" + productId;

        // Execute the script atomically in Redis
        return redisTemplate.execute(
                decrementScript,
                Collections.singletonList(key),
                String.valueOf(quantity)
        );
    }

    public void incrementStock(Long productId, Integer quantity) {
        redisTemplate.opsForValue().increment("product:stock:" + productId, quantity);
    }

    public void initializeStock(Long productId, Integer stock) {
        String key = "product:stock:" + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
    }
}