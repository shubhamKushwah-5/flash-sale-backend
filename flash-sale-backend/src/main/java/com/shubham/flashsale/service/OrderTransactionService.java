package com.shubham.flashsale.service;

import com.shubham.flashsale.dto.PurchaseRequest;
import com.shubham.flashsale.dto.PurchaseResponse;
import com.shubham.flashsale.model.Order;
import com.shubham.flashsale.model.OrderStatus;
import com.shubham.flashsale.model.Product;
import com.shubham.flashsale.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderTransactionService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Transactional
    public PurchaseResponse commitToDatabase(PurchaseRequest request) {
        Product product = productService.getProductById(request.getProductId());

        product.setAvailableStock(product.getAvailableStock() - request.getQuantity());
        productService.saveProduct(product);

        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Order successOrder = new Order();
        successOrder.setProductId(request.getProductId());
        successOrder.setUserId(request.getUserId());
        successOrder.setQuantity(request.getQuantity());
        successOrder.setTotalPrice(totalPrice);
        successOrder.setStatus(OrderStatus.SUCCESS);

        Order saved = orderRepository.save(successOrder);
        return PurchaseResponse.success(saved.getId());
    }

    @Transactional
    public void saveFailedOrder(PurchaseRequest request) {
        Order failedOrder = new Order();
        failedOrder.setProductId(request.getProductId());
        failedOrder.setUserId(request.getUserId());
        failedOrder.setQuantity(request.getQuantity());
        failedOrder.setTotalPrice(BigDecimal.ZERO);
        failedOrder.setStatus(OrderStatus.FAILED);
        orderRepository.save(failedOrder);
    }


}
