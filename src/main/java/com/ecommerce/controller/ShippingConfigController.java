package com.ecommerce.controller;

import com.ecommerce.model.ShippingConfig;
import com.ecommerce.service.TaxService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shipping")
@CrossOrigin(origins = "*")
public class ShippingConfigController {
    
    @Autowired
    private TaxService taxService;
    
    @GetMapping
    public ResponseEntity<ShippingConfig> getShippingConfig() {
        // Public endpoint - anyone can view shipping config
        return ResponseEntity.ok(taxService.getShippingConfig());
    }
    
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShippingConfig> updateShippingConfig(
            @RequestParam Double shippingCost,
            @RequestParam Double freeShippingThreshold) {
        
        System.out.println("📦 Updating shipping config:");
        System.out.println("   Shipping Cost: $" + shippingCost);
        System.out.println("   Free Shipping Threshold: $" + freeShippingThreshold);
        
        ShippingConfig config = taxService.updateShippingConfig(shippingCost, freeShippingThreshold);
        
        System.out.println("✅ Shipping config updated successfully");
        
        return ResponseEntity.ok(config);
    }
    @Repository
    public interface ShippingConfigRepository extends JpaRepository<ShippingConfig, Long> {
        
        Optional<ShippingConfig> findFirstByOrderByIdDesc();
        
    }
}
