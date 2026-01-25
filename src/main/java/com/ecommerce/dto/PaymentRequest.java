package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public class PaymentRequest {
    
    @NotNull(message = "Amount is required")
    private Double amount;
    
    public PaymentRequest() {
    }
    
    public PaymentRequest(Double amount) {
        this.amount = amount;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
