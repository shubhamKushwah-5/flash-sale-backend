package com.shubham.flashsale.repository;

import com.shubham.flashsale.model.Product;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {

    //Pessimistic locking - locks row for update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(Long id);

    // NOrmal read without lock (for optimistic locking approach)
    Optional<Product> findById(Long id);

    // NEW: Instantly resets available stock to total stock and version to 0 for ALL products
//    @Modifying(clearAutomatically = true, flushAutomatically = true)
//    @Query("UPDATE Product p SET p.availableStock = p.totalStock, p.version = 0")
//    void resetAllStockAndVersion();

    // Use nativeQuery = true to force MySQL to execute this directly, bypassing Hibernate entirely
    @Modifying
    @Query(value = "UPDATE flash_sale_products SET available_stock = total_stock, version = 0", nativeQuery = true)
    void resetAllStockAndVersion();



}
