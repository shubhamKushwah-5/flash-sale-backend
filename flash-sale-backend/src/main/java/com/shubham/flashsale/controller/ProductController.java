package com.shubham.flashsale.controller;

import com.shubham.flashsale.dto.ProductRequest;
import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.lang.model.element.ModuleElement;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Create product
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = productService.createProduct(request);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    // Get all products
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    // Get product by ID
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // Get available stock for a product
    @GetMapping("/{id}/stock")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        StockResponse response = new StockResponse(
                product.getAvailableStock(),
                product.getTotalStock()
        );
        return ResponseEntity.ok(response);
    }

    // Inner class for stock response
    public static class StockResponse {
        private Integer availableStock;
        private Integer totalStock;

        public StockResponse(Integer availableStock, Integer totalStock) {
            this.availableStock = availableStock;
            this.totalStock = totalStock;
        }

        public Integer getAvailableStock(){ return availableStock;}
        public Integer getTotalStock() {return totalStock;}
    }
}
