package com.shubham.flashsale.service;

import com.shubham.flashsale.dto.ProductRequest;
import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Create product (for setup)
    public Product createProduct(ProductRequest request) {
        Product product = new Product(
                request.getName(),
                request.getPrice(),
                request.getStock()
        );
        return productRepository.save(product);
    }

    // Get all products
    public List<Product> getAllProducts(){
        return productRepository.findAll();
    }

    // Get products by ID (without lock - for display)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id) );
    }

    //Decrement stock with pessimistic lock
    // This is called from OrderService within a transaction
    @Transactional
    public boolean decrementStockPessimistic(Long productId, Integer quantity) {
        //Lock the product row for update
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if enough stock available
        if(product.getAvailableStock() < quantity) {
            return false;
        }

        // Decrement stock
        product.setAvailableStock(product.getAvailableStock() - quantity);
        productRepository.save(product);

        return true;
    }

    // Decrement stock with optimistic lock
    @Transactional
    public boolean decrementStockOptimistic(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check stock
        if(product.getAvailableStock() < quantity) {
            return false;
        }

        // Decrement - if version changed , OptimisticLockException throw
        product.setAvailableStock(product.getAvailableStock() - quantity);
        productRepository.save(product);  // Version check will occur here
        return true;
    }


}
