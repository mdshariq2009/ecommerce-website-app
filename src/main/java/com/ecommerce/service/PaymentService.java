package com.ecommerce.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {
    
    @Value("${stripe.api.key}")
    private String stripeApiKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        System.out.println("✅ Stripe initialized with API key: " + stripeApiKey.substring(0, 20) + "...");
    }
    
    public Map<String, String> createPaymentIntent(Double amount) throws StripeException {
        System.out.println("💳 Creating payment intent for amount: $" + amount);
        
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) (amount * 100)) // Convert to cents
                    .setCurrency("usd")
                    .addPaymentMethodType("card")
                    .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            System.out.println("✅ Payment intent created: " + paymentIntent.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            
            return response;
        } catch (StripeException e) {
            System.err.println("❌ Stripe error: " + e.getMessage());
            throw e;
        }
    }
}
