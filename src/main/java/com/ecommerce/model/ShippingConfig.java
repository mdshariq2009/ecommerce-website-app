package com.ecommerce.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_config")
public class ShippingConfig implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Double shippingCost;
    
    @Column(nullable = false)
    private Double freeShippingThreshold;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public ShippingConfig() {
    }
    
    public ShippingConfig(Double shippingCost, Double freeShippingThreshold) {
        this.shippingCost = shippingCost;
        this.freeShippingThreshold = freeShippingThreshold;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Double getShippingCost() {
        return shippingCost;
    }
    
    public void setShippingCost(Double shippingCost) {
        this.shippingCost = shippingCost;
    }
    
    public Double getFreeShippingThreshold() {
        return freeShippingThreshold;
    }
    
    public void setFreeShippingThreshold(Double freeShippingThreshold) {
        this.freeShippingThreshold = freeShippingThreshold;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
