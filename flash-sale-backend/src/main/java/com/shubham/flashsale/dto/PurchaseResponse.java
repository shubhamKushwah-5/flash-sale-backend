package com.shubham.flashsale.dto;

import com.shubham.flashsale.model.OrderStatus;

public class PurchaseResponse {

    private boolean success;
    private String message;
    private Long orderId;
    private OrderStatus status;

    // Static factory methods
    public static PurchaseResponse success(Long orderId) {
        PurchaseResponse response = new PurchaseResponse();
        response.success = true;
        response.message = "Purchase successfull";
        response.orderId = orderId;
        response.status = OrderStatus.SUCCESS;
        return response;
    }

    public static PurchaseResponse failure(String message) {
        PurchaseResponse response = new PurchaseResponse();
        response.success = false;
        response.message = message;
        response.status = OrderStatus.FAILED;
        return response;
    }

    // Getters and Setters
    public boolean isSuccess() { return success;}
    public void setSuccess(boolean success) {this.success = success;}

    public String getMessage() {return message;}
    public void setMessage(String message){ this.message = message;}

    public Long getOrderId() {return orderId;}
    public void setOrderId(Long orderId) {this.orderId = orderId;}

    public OrderStatus getStatus() {return status;}
    public void setStatus(OrderStatus status ) {this.status = status;}
}
