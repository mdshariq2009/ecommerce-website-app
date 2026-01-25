package com.ecommerce.dto;

public class OrderCalculation {
    
    private Double subtotal;
    private Double tax;
    private Double taxRate;
    private Double shipping;
    private Double total;
    
    // Constructors
    public OrderCalculation() {
    }
    
    public OrderCalculation(Double subtotal, Double tax, Double taxRate, Double shipping, Double total) {
        this.subtotal = subtotal;
        this.tax = tax;
        this.taxRate = taxRate;
        this.shipping = shipping;
        this.total = total;
    }
    
    // Getters and Setters
    public Double getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }
    
    public Double getTax() {
        return tax;
    }
    
    public void setTax(Double tax) {
        this.tax = tax;
    }
    
    public Double getTaxRate() {
        return taxRate;
    }
    
    public void setTaxRate(Double taxRate) {
        this.taxRate = taxRate;
    }
    
    public Double getShipping() {
        return shipping;
    }
    
    public void setShipping(Double shipping) {
        this.shipping = shipping;
    }
    
    public Double getTotal() {
        return total;
    }
    
    public void setTotal(Double total) {
        this.total = total;
    }
}
