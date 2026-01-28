package com.ecommerce.service;

import com.ecommerce.model.ShippingConfig;
import com.ecommerce.repository.ShippingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TaxService {
    
    @Autowired
    private ShippingConfigRepository shippingConfigRepository;
    
    // Default values if not configured
    private static final double DEFAULT_SHIPPING_COST = 10.0;
    private static final double DEFAULT_FREE_SHIPPING_THRESHOLD = 50.0;
    
    // Tax rates by state
    private static final Map<String, Double> STATE_TAX_RATES = new HashMap<>();
    private static final Map<String, Double> ZIP_TAX_RATES = new HashMap<>();
    
    static {
        // State tax rates (%)
        STATE_TAX_RATES.put("CA", 7.25);
        STATE_TAX_RATES.put("NY", 8.52);
        STATE_TAX_RATES.put("TX", 6.25);
        STATE_TAX_RATES.put("FL", 6.0);
        STATE_TAX_RATES.put("IL", 6.25);
        STATE_TAX_RATES.put("PA", 6.0);
        STATE_TAX_RATES.put("OH", 5.75);
        STATE_TAX_RATES.put("WA", 6.5);
        STATE_TAX_RATES.put("MA", 6.25);
        STATE_TAX_RATES.put("NJ", 6.625);
        STATE_TAX_RATES.put("CO", 8.0); // Colorado
        STATE_TAX_RATES.put("Noida", 5.0); //
        STATE_TAX_RATES.put("Delhi", 6.0);
        STATE_TAX_RATES.put("New Delhi", 6.0);
        STATE_TAX_RATES.put("Uttar Pradesh", 6.5);
        STATE_TAX_RATES.put("UP", 6.5);
        
        // Zip code specific rates
        ZIP_TAX_RATES.put("100", 8.875); // NYC
        ZIP_TAX_RATES.put("900", 9.5);   // LA
        ZIP_TAX_RATES.put("941", 9.25);  // SF
        ZIP_TAX_RATES.put("606", 10.25); // Chicago
        ZIP_TAX_RATES.put("801", 8.0); // Parker area
        ZIP_TAX_RATES.put("201", 5.0); // Noida
        ZIP_TAX_RATES.put("110", 6.0); // Delhi
        ZIP_TAX_RATES.put("244", 6.5); //UP
    }
    
    public double calculateTax(double amount, String state, String zipCode) {
        double taxRate = getTaxRate(state, zipCode);
        return amount * (taxRate / 100);
    }
    
    public double getTaxRate(String state, String zipCode) {
        if (zipCode != null && zipCode.length() >= 3) {
            String zipPrefix = zipCode.substring(0, 3);
            if (ZIP_TAX_RATES.containsKey(zipPrefix)) {
                return ZIP_TAX_RATES.get(zipPrefix);
            }
        }
        
        if (state != null && STATE_TAX_RATES.containsKey(state.toUpperCase())) {
            return STATE_TAX_RATES.get(state.toUpperCase());
        }
        
        return 7.0;
    }
    
    public double calculateShipping(double subtotal) {
        ShippingConfig config = getShippingConfig();
        
        if (subtotal >= config.getFreeShippingThreshold()) {
            return 0.0;
        }
        
        return config.getShippingCost();
    }
    
    public ShippingConfig getShippingConfig() {
        return shippingConfigRepository.findFirstByOrderByIdDesc()
                .orElse(new ShippingConfig(DEFAULT_SHIPPING_COST, DEFAULT_FREE_SHIPPING_THRESHOLD));
    }
    
    public ShippingConfig updateShippingConfig(Double shippingCost, Double freeShippingThreshold) {
        ShippingConfig config = shippingConfigRepository.findFirstByOrderByIdDesc()
                .orElse(new ShippingConfig());
        
        config.setShippingCost(shippingCost);
        config.setFreeShippingThreshold(freeShippingThreshold);
        config.setUpdatedAt(java.time.LocalDateTime.now());
        
        return shippingConfigRepository.save(config);
    }
}
