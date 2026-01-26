package com.ecommerce.controller;

import com.ecommerce.model.PaymentMethod;
import com.ecommerce.model.User;
import com.ecommerce.repository.PaymentMethodRepository;
import com.ecommerce.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentMethodAttachParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    @Value("${stripe.api.key:sk_test_default}")
    private String stripeSecretKey;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    // Get user profile
    @GetMapping
    public ResponseEntity<?> getProfile(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("📋 Getting profile for: " + user.getEmail());
            
            // Build response manually
            Map<String, Object> profileResponse = new HashMap<>();
            profileResponse.put("id", user.getId());
            profileResponse.put("name", user.getName());
            profileResponse.put("email", user.getEmail());
            profileResponse.put("role", user.getRole().toString());
            profileResponse.put("createdAt", user.getCreatedAt());
            profileResponse.put("phoneNumber", user.getPhoneNumber());
            
            // Legacy fields for backward compatibility
            profileResponse.put("cardLastFour", user.getCardLastFour());
            profileResponse.put("savedPaymentMethod", user.getSavedPaymentMethod());
            profileResponse.put("billingZip", user.getBillingZip());
            profileResponse.put("stripePaymentMethodId", user.getStripePaymentMethodId());
            profileResponse.put("stripeCustomerId", user.getStripeCustomerId());
            profileResponse.put("defaultPaymentMethodId", user.getDefaultPaymentMethodId());
            
            return ResponseEntity.ok(profileResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error getting profile: " + e.getMessage());
        }
    }

    // Update user profile
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UserProfileDTO profileDTO, Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (profileDTO.getName() != null) {
                user.setName(profileDTO.getName());
            }
            if (profileDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(profileDTO.getPhoneNumber());
            }
            
            userRepository.save(user);
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating profile: " + e.getMessage());
        }
    }

    // Get all payment methods
    @GetMapping("/payment-methods")
    public ResponseEntity<?> getPaymentMethods(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<PaymentMethod> paymentMethods = paymentMethodRepository
                    .findByUserOrderByIsDefaultDescCreatedAtDesc(user);
            
            System.out.println("📋 Found " + paymentMethods.size() + " payment methods for user");
            
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Get default payment method
    @GetMapping("/default-payment-method")
    public ResponseEntity<?> getDefaultPaymentMethod(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Optional<PaymentMethod> defaultPM = paymentMethodRepository
                    .findByUserAndIsDefault(user, true);
            
            if (defaultPM.isPresent()) {
                System.out.println("✅ Default payment method found: " + defaultPM.get().getCardLastFour());
                return ResponseEntity.ok(defaultPM.get());
            } else {
                System.out.println("ℹ️ No default payment method");
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Set default payment method
    @PostMapping("/set-default-payment-method/{paymentMethodId}")
    public ResponseEntity<?> setDefaultPaymentMethod(
            @PathVariable Long paymentMethodId,
            Principal principal) {
        
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Unset all as default
            List<PaymentMethod> allMethods = paymentMethodRepository.findByUser(user);
            allMethods.forEach(pm -> pm.setIsDefault(false));
            paymentMethodRepository.saveAll(allMethods);
            
            // Set selected as default
            PaymentMethod selectedPM = paymentMethodRepository.findById(paymentMethodId)
                    .orElseThrow(() -> new RuntimeException("Payment method not found"));
            
            if (!selectedPM.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body("Unauthorized");
            }
            
            selectedPM.setIsDefault(true);
            paymentMethodRepository.save(selectedPM);
            
            // Update user's default reference and legacy fields
            user.setDefaultPaymentMethodId(selectedPM.getStripePaymentMethodId());
            user.setStripePaymentMethodId(selectedPM.getStripePaymentMethodId());
            user.setSavedPaymentMethod(selectedPM.getCardBrand());
            user.setCardLastFour(selectedPM.getCardLastFour());
            userRepository.save(user);
            
            System.out.println("✅ Set payment method " + paymentMethodId + " as default");
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Save Stripe Payment Method (supports multiple cards)
    @PostMapping("/save-stripe-payment-method")
    public ResponseEntity<?> saveStripePaymentMethod(
            @RequestBody SavePaymentMethodRequest request,
            Principal principal) {
        
        try {
            Stripe.apiKey = stripeSecretKey;
            
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("=== SAVING PAYMENT METHOD ===");
            System.out.println("Payment Method ID: " + request.getPaymentMethodId());
            System.out.println("Is Default: " + request.getIsDefault());
            
            // Attach to Stripe Customer
            if (user.getStripeCustomerId() != null && request.getPaymentMethodId() != null) {
                try {
                    com.stripe.model.PaymentMethod stripePM = com.stripe.model.PaymentMethod
                            .retrieve(request.getPaymentMethodId());
                    
                    if (stripePM.getCustomer() == null) {
                        PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                                .setCustomer(user.getStripeCustomerId())
                                .build();
                        stripePM.attach(attachParams);
                        System.out.println("✅ PaymentMethod attached to Customer");
                    }
                } catch (StripeException e) {
                    System.err.println("❌ Stripe error: " + e.getMessage());
                    return ResponseEntity.badRequest().body("Error: " + e.getMessage());
                }
            }
            
            // Check if payment method already exists
            Optional<PaymentMethod> existing = paymentMethodRepository
                    .findByUserAndStripePaymentMethodId(user, request.getPaymentMethodId());
            
            PaymentMethod paymentMethod;
            
            if (existing.isPresent()) {
                paymentMethod = existing.get();
                System.out.println("Updating existing payment method");
            } else {
                paymentMethod = new PaymentMethod();
                paymentMethod.setUser(user);
                System.out.println("Creating new payment method");
            }
            
            // Set payment method details
            paymentMethod.setStripePaymentMethodId(request.getPaymentMethodId());
            paymentMethod.setCardBrand(request.getCardBrand());
            paymentMethod.setCardLastFour(request.getCardLast4());
            
            // Determine if this should be default
            List<PaymentMethod> userMethods = paymentMethodRepository.findByUser(user);
            boolean shouldBeDefault = request.getIsDefault() != null ? request.getIsDefault() : userMethods.isEmpty();
            
            if (shouldBeDefault) {
                // Unset all others as default
                userMethods.forEach(pm -> pm.setIsDefault(false));
                paymentMethodRepository.saveAll(userMethods);
                
                paymentMethod.setIsDefault(true);
                user.setDefaultPaymentMethodId(request.getPaymentMethodId());
                
                // Update legacy fields for backward compatibility
                user.setStripePaymentMethodId(request.getPaymentMethodId());
                user.setSavedPaymentMethod(request.getCardBrand());
                user.setCardLastFour(request.getCardLast4());
            } else {
                paymentMethod.setIsDefault(false);
            }
            
            paymentMethodRepository.save(paymentMethod);
            userRepository.save(user);
            
            System.out.println("✅ Payment method saved - Default: " + paymentMethod.getIsDefault());
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Delete specific payment method
    @DeleteMapping("/payment-method/{paymentMethodId}")
    public ResponseEntity<?> deleteSpecificPaymentMethod(
            @PathVariable Long paymentMethodId,
            Principal principal) {
        
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            PaymentMethod pm = paymentMethodRepository.findById(paymentMethodId)
                    .orElseThrow(() -> new RuntimeException("Payment method not found"));
            
            if (!pm.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body("Unauthorized");
            }
            
            boolean wasDefault = pm.getIsDefault();
            
            paymentMethodRepository.delete(pm);
            System.out.println("✅ Payment method deleted: " + paymentMethodId);
            
            // If deleted card was default, set another as default
            if (wasDefault) {
                List<PaymentMethod> remaining = paymentMethodRepository.findByUser(user);
                if (!remaining.isEmpty()) {
                    PaymentMethod newDefault = remaining.get(0);
                    newDefault.setIsDefault(true);
                    paymentMethodRepository.save(newDefault);
                    
                    user.setDefaultPaymentMethodId(newDefault.getStripePaymentMethodId());
                    user.setStripePaymentMethodId(newDefault.getStripePaymentMethodId());
                    user.setSavedPaymentMethod(newDefault.getCardBrand());
                    user.setCardLastFour(newDefault.getCardLastFour());
                } else {
                    user.setDefaultPaymentMethodId(null);
                    user.setStripePaymentMethodId(null);
                    user.setSavedPaymentMethod(null);
                    user.setCardLastFour(null);
                }
                userRepository.save(user);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Legacy delete endpoint (deletes all payment methods)
    @DeleteMapping("/payment-method")
    public ResponseEntity<?> deletePaymentMethod(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            System.out.println("🗑️ Deleting all payment methods for: " + user.getEmail());
            
            // Delete all payment methods
            List<PaymentMethod> methods = paymentMethodRepository.findByUser(user);
            paymentMethodRepository.deleteAll(methods);
            
            // Clear legacy fields
            user.setStripePaymentMethodId(null);
            user.setSavedPaymentMethod(null);
            user.setCardLastFour(null);
            user.setBillingZip(null);
            user.setDefaultPaymentMethodId(null);
            
            userRepository.save(user);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

 // Change password
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordDTO request,
            Principal principal) {
        
        try {
            System.out.println("========================================");
            System.out.println("🔐 Password change request for: " + principal.getName());
            System.out.println("========================================");
            
            User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate current password
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Current password is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Verify current password matches
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                System.err.println("❌ Current password incorrect for user: " + principal.getName());
                
                Map<String, String> error = new HashMap<>();
                error.put("error", "Current password is incorrect");
                return ResponseEntity.status(401).body(error);
            }
            
            // Validate new password
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "New password is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.getNewPassword().length() < 6) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "New password must be at least 6 characters long");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Check if new password is same as current
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "New password must be different from current password");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Encode and save new password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            
            System.out.println("========================================");
            System.out.println("✅ Password changed successfully for: " + principal.getName());
            System.out.println("========================================");
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ Error changing password");
            System.err.println("   User: " + principal.getName());
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to change password: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Debug endpoint
    @GetMapping("/debug-payment")
    public ResponseEntity<?> debugPayment(Principal principal) {
        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<PaymentMethod> methods = paymentMethodRepository.findByUser(user);
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("email", user.getEmail());
            debug.put("stripeCustomerId", user.getStripeCustomerId());
            debug.put("defaultPaymentMethodId", user.getDefaultPaymentMethodId());
            debug.put("paymentMethodsCount", methods.size());
            debug.put("paymentMethods", methods);
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // DTO Classes
    public static class UserProfileDTO {
        private String name;
        private String phoneNumber;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }

    public static class SavePaymentMethodRequest {
        private String paymentMethodId;
        private String cardBrand;
        private String cardLast4;
        private String billingZip;
        private Boolean isDefault;
        
        public String getPaymentMethodId() {
            return paymentMethodId;
        }
        
        public void setPaymentMethodId(String paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
        }
        
        public String getCardBrand() {
            return cardBrand;
        }
        
        public void setCardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
        }
        
        public String getCardLast4() {
            return cardLast4;
        }
        
        public void setCardLast4(String cardLast4) {
            this.cardLast4 = cardLast4;
        }
        
        public String getBillingZip() {
            return billingZip;
        }
        
        public void setBillingZip(String billingZip) {
            this.billingZip = billingZip;
        }
        
        public Boolean getIsDefault() {
            return isDefault;
        }
        
        public void setIsDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }
    }

    public static class ChangePasswordDTO {
        private String currentPassword;
        private String newPassword;
        
        public String getCurrentPassword() {
            return currentPassword;
        }
        
        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }
        
        public String getNewPassword() {
            return newPassword;
        }
        
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
