package com.shubham.flashsale.service;

import com.shubham.flashsale.dto.PurchaseRequest;
import com.shubham.flashsale.dto.PurchaseResponse;
import com.shubham.flashsale.model.Order;
import com.shubham.flashsale.model.OrderStatus;
import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.repository.OrderRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.aspectj.weaver.ast.Or;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.shubham.flashsale.service.RedisStockService;

import java.math.BigDecimal;
import java.util.List;


@Service
public class OrderService {

   @Autowired
   private OrderTransactionService orderTransactionService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisStockService redisStockService;

    /**
     * Purchase with PESSIMISTIC locking
     *
     *
     * Transaction ensures atomicity of stock check + decrement + order creation
     */

    @Transactional
    public PurchaseResponse purchaseWithPessimisticLock(PurchaseRequest request) {
        try {
            //Attemp to decrement stock (this locks the product row
            boolean stockDecremented = productService.decrementStockPessimistic(
                    request.getProductId(),
                    request.getQuantity()
            );

            if(!stockDecremented) {
                // NOt enough stock - order failed
                Order failedOrder = createOrder(request, OrderStatus.FAILED);
                orderRepository.save(failedOrder);
                return PurchaseResponse.failure("Insufficient stock");
            }

            // Stock decremented successfully - create successful order
            Order successOrder = createOrder(request, OrderStatus.SUCCESS);
            Order saved = orderRepository.save(successOrder);

            return PurchaseResponse.success(saved.getId());

        } catch (Exception e) {
            Order failedOrder = createOrder(request, OrderStatus.FAILED);
            orderRepository.save(failedOrder);
            return PurchaseResponse.failure("Purchase failed: " + e.getMessage());
        }
    }

    /**
     * Purchase with PESSIMISTIC locking
     *
     *
     * Transaction ensures atomicity of stock check + decrement + order creation
     */
    @Transactional
    public PurchaseResponse purchaseWithOptimisticLock(PurchaseRequest request) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries ) {
            try {
                // Attempt to decrement stock
                boolean stockDecremented = productService.decrementStockOptimistic(
                        request.getProductId(),
                        request.getQuantity()
                );

                if(!stockDecremented) {
                    Order failedOrder = createOrder(request, OrderStatus.FAILED);
                    orderRepository.save(failedOrder);
                    return PurchaseResponse.failure("Insufficient stock");
                }

                // Success
                Order successOrder = createOrder(request, OrderStatus.SUCCESS);
                Order saved = orderRepository.save(successOrder);
                return PurchaseResponse.success(saved.getId());

            } catch(OptimisticLockException e) {
                // version conflict - retry
                attempt++;
                if(attempt >= maxRetries) {
                    Order failedOrder = createOrder(request, OrderStatus.FAILED);
                    orderRepository.save(failedOrder);
                    return PurchaseResponse.failure("Too many concurrent attempts, please try later");
                }
                //wait a bit before retry
                try{
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }  catch (Exception e) {
                Order failedOrder = createOrder(request, OrderStatus.FAILED);
                orderRepository.save(failedOrder);
                return PurchaseResponse.failure("Purchase failed: " + e.getMessage());
            }
        }

        return PurchaseResponse.failure("Purchase failed after retries");
    }


    // purchase with REDIS

   // no  @Transactional as we do not lock db connection for redis
    public PurchaseResponse purchaseWithRedis(PurchaseRequest request) {
       try {
           Long remainingStock = redisStockService.decrementStock(
                   request.getProductId(),
                   request.getQuantity()
           );

           if (remainingStock < 0) {
               // Safely routes to the newly separated proxy
               orderTransactionService.saveFailedOrder(request);
               return PurchaseResponse.failure("Insufficient stock");
           }

           // Safely routes to the newly separated proxy
           return orderTransactionService.commitToDatabase(request);

       } catch (Exception e) {
           redisStockService.incrementStock(request.getProductId(), request.getQuantity());
           orderTransactionService.saveFailedOrder(request);
           return PurchaseResponse.failure("Purchase failed: " + e.getMessage());
       }
    }



    //Get user's order history
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    //get successful order for a product for analytics
    public long getSuccessfulOrderCount(Long productId) {
        return orderRepository.countByProductIdAndStatus(productId,OrderStatus.SUCCESS);
    }

    //helper method to create order enttity
    private Order createOrder(PurchaseRequest request, OrderStatus status) {
        // Get product to calculate total price
        Product product = productService.getProductById(request.getProductId());
        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        return new Order (
                request.getProductId(),
                request.getUserId(),
                request.getQuantity(),
                totalPrice,status
        );
    }

}
