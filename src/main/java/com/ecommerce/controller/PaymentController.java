package com.ecommerce.controller;

import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestBody Map<String, Object> paymentInfo,
            Principal principal) {
        
        try {
            Stripe.apiKey = stripeSecretKey;
            
            Double amount = (Double) paymentInfo.get("amount");
            long amountInCents = (long) (amount * 100);

            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("=== CREATING PAYMENT INTENT ===");
            System.out.println("💳 User: " + user.getEmail());
            System.out.println("Amount: $" + amount + " (" + amountInCents + " cents)");
            System.out.println("Current Customer ID: " + user.getStripeCustomerId());

            // Create or get Stripe Customer
            String customerId = user.getStripeCustomerId();
            if (customerId == null || customerId.isEmpty()) {
                System.out.println("🔧 Creating new Stripe Customer...");
                
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(user.getName())
                        .setDescription("Customer for " + user.getEmail())
                        .build();
                
                Customer customer = Customer.create(customerParams);
                customerId = customer.getId();
                
                System.out.println("✅ Stripe Customer created: " + customerId);
                
                // Save customer ID to database
                user.setStripeCustomerId(customerId);
                User savedUser = userRepository.save(user);
                
                System.out.println("✅ Customer ID saved to database");
                System.out.println("Verified saved Customer ID: " + savedUser.getStripeCustomerId());
                
            } else {
                System.out.println("✅ Using existing Stripe Customer: " + customerId);
            }

            // Create PaymentIntent WITH Customer
            System.out.println("🔧 Creating PaymentIntent with Customer: " + customerId);
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setCustomer(customerId)  // CRITICAL: Attach to customer
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)  // Allow reuse
                    .setDescription("Order payment for " + user.getEmail())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            System.out.println("✅ PaymentIntent created successfully!");
            System.out.println("PaymentIntent ID: " + paymentIntent.getId());
            System.out.println("Client Secret: " + paymentIntent.getClientSecret().substring(0, 20) + "...");
            System.out.println("Status: " + paymentIntent.getStatus());
            System.out.println("Customer: " + paymentIntent.getCustomer());
            System.out.println("=== PAYMENT INTENT CREATION COMPLETE ===");

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());

            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            System.err.println("❌ Stripe Exception: " + e.getMessage());
            System.err.println("Stripe Error Code: " + e.getCode());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            System.err.println("❌ General Exception: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Debug endpoint - Check Stripe Customer
    @GetMapping("/debug-customer")
    public ResponseEntity<?> debugCustomer(Principal principal) {
        try {
            Stripe.apiKey = stripeSecretKey;
            
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("email", user.getEmail());
            debug.put("stripeCustomerId", user.getStripeCustomerId());
            
            if (user.getStripeCustomerId() != null) {
                try {
                    Customer customer = Customer.retrieve(user.getStripeCustomerId());
                    debug.put("customerExists", true);
                    debug.put("customerEmail", customer.getEmail());
                    debug.put("customerName", customer.getName());
                    debug.put("paymentMethodsCount", customer.getInvoiceSettings().getDefaultPaymentMethod());
                } catch (StripeException e) {
                    debug.put("customerExists", false);
                    debug.put("error", e.getMessage());
                }
            } else {
                debug.put("customerExists", false);
            }
            
            System.out.println("🔍 Debug Customer Info: " + debug);
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/create-setup-intent")
    public ResponseEntity<Map<String, String>> createSetupIntent(Principal principal) {
        
        try {
            Stripe.apiKey = stripeSecretKey;
            
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("🔧 Creating Setup Intent for: " + user.getEmail());

            // Create or get Stripe Customer
            String customerId = user.getStripeCustomerId();
            if (customerId == null || customerId.isEmpty()) {
                System.out.println("🔧 Creating new Stripe Customer...");
                
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(user.getName())
                        .build();
                
                Customer customer = Customer.create(customerParams);
                customerId = customer.getId();
                
                user.setStripeCustomerId(customerId);
                userRepository.save(user);
                
                System.out.println("✅ Stripe Customer created: " + customerId);
            } else {
                System.out.println("✅ Using existing Stripe Customer: " + customerId);
            }

            // Create Setup Intent (for saving cards without payment)
            com.stripe.param.SetupIntentCreateParams params = 
                com.stripe.param.SetupIntentCreateParams.builder()
                        .setCustomer(customerId)
                        .build();

            com.stripe.model.SetupIntent setupIntent = com.stripe.model.SetupIntent.create(params);

            System.out.println("✅ Setup Intent created: " + setupIntent.getId());

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", setupIntent.getClientSecret());

            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            System.err.println("❌ Stripe Exception: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    

    @GetMapping("/get-payment-method-details")
    public ResponseEntity<Map<String, String>> getPaymentMethodDetails(
            @RequestParam String paymentMethodId,
            Principal principal) {
        
        try {
            Stripe.apiKey = stripeSecretKey;
            
            System.out.println("🔍 Retrieving payment method details: " + paymentMethodId);
            
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            
            Map<String, String> details = new HashMap<>();
            details.put("brand", paymentMethod.getCard().getBrand());
            details.put("last4", paymentMethod.getCard().getLast4());
            details.put("expMonth", paymentMethod.getCard().getExpMonth().toString());
            details.put("expYear", paymentMethod.getCard().getExpYear().toString());
            
            System.out.println("✅ Card details - Brand: " + details.get("brand") + ", Last4: " + details.get("last4"));
            
            return ResponseEntity.ok(details);
            
        } catch (StripeException e) {
            System.err.println("❌ Stripe Exception: " + e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    @GetMapping("/stripe-publishable-key")
    public ResponseEntity<Map<String, String>> getStripePublishableKey() {
        Map<String, String> response = new HashMap<>();
        response.put("publishableKey", stripePublishableKey);
        return ResponseEntity.ok(response);
    }

}
