package com.ecommerce.controller;

import com.ecommerce.dto.OrderCalculation;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.EmailService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.TaxService;
import com.ecommerce.util.CarrierDetector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TaxService taxService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private CarrierDetector carrierDetector;  // ← ADD THIS LINE

    // ========================================
    // CALCULATE ORDER TOTALS
    // ========================================
    
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateOrder(@RequestBody OrderRequest request) {
        try {
            System.out.println("🧮 Calculating order totals...");
            
            double subtotal = 0.0;
            for (OrderRequest.OrderItemDTO item : request.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
                subtotal += product.getPrice() * item.getQuantity();
            }
            
            double shipping = taxService.calculateShipping(subtotal);
            double taxRate = taxService.getTaxRate(request.getShippingState(), request.getShippingZipCode());
            double tax = taxService.calculateTax(subtotal, request.getShippingState(), request.getShippingZipCode());
            double total = subtotal + tax + shipping;
            
            System.out.println("   Subtotal: $" + subtotal);
            System.out.println("   Tax: $" + tax);
            System.out.println("   Shipping: $" + shipping);
            System.out.println("   Total: $" + total);
            
            OrderCalculation calculation = new OrderCalculation(subtotal, tax, taxRate, shipping, total);
            return ResponseEntity.ok(calculation);
            
        } catch (Exception e) {
            System.err.println("❌ Error calculating order: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to calculate order: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
 // ========================================
 // CANCEL RETURN REQUEST ENDPOINT
 // ========================================

 @PostMapping("/{orderId}/cancel-return")
 public ResponseEntity<?> cancelReturnRequest(
         @PathVariable Long orderId,
         Authentication authentication) {
     
     try {
         System.out.println("========================================");
         System.out.println("❌ CANCEL RETURN REQUEST RECEIVED");
         System.out.println("❌ Processing cancel return for Order #" + orderId);
         
         // Find the order
         Order order = orderRepository.findById(orderId)
                 .orElseThrow(() -> new RuntimeException("Order not found"));
         
         // Verify the order belongs to the authenticated user
         String email = authentication.getName();
         if (!order.getUser().getEmail().equals(email)) {
             System.err.println("❌ Unauthorized: Order does not belong to user");
             return ResponseEntity.status(403).body(Map.of(
                 "success", false,
                 "message", "Unauthorized access"
             ));
         }
         
         System.out.println("✅ Order found: " + order.getId());
         System.out.println("👤 Customer: " + order.getUser().getName() + " (" + order.getUser().getEmail() + ")");
         System.out.println("📧 Current order status: " + order.getOrderStatus());
         
         // Update order status back to DELIVERED
         order.setOrderStatus(Order.OrderStatus.DELIVERED);
         order.setReturnRequestDate(null);
         Order savedOrder = orderRepository.save(order);
         orderRepository.flush();
         
         System.out.println("✅ Order status restored to DELIVERED in database");
         System.out.println("✅ Saved order status: " + savedOrder.getOrderStatus());
         
         boolean customerEmailSent = false;
         boolean adminEmailSent = false;
         
         // Send confirmation email to customer
         try {
             System.out.println("========================================");
             System.out.println("📧 SENDING CUSTOMER CANCEL RETURN EMAIL");
             System.out.println("📧 To: " + order.getUser().getEmail());
             
             emailService.sendCancelReturnConfirmationEmail(savedOrder);
             customerEmailSent = true;
             
             System.out.println("✅ Customer cancel return email sent successfully!");
         } catch (Exception emailError) {
             System.err.println("❌ Customer email FAILED!");
             System.err.println("❌ Error: " + emailError.getMessage());
             emailError.printStackTrace();
         }
         
         // Send notification email to admin
         try {
             System.out.println("========================================");
             System.out.println("📧 SENDING ADMIN CANCEL RETURN NOTIFICATION");
             System.out.println("📧 To: mdshariq2009@gmail.com");
             
             emailService.sendAdminCancelReturnNotification(savedOrder);
             adminEmailSent = true;
             
             System.out.println("✅ Admin cancel return notification sent successfully!");
         } catch (Exception emailError) {
             System.err.println("❌ Admin email FAILED!");
             System.err.println("❌ Error: " + emailError.getMessage());
             emailError.printStackTrace();
         }
         
         System.out.println("========================================");
         System.out.println("📊 EMAIL STATUS SUMMARY:");
         System.out.println("   Customer Email: " + (customerEmailSent ? "✅ SENT" : "❌ FAILED"));
         System.out.println("   Admin Email: " + (adminEmailSent ? "✅ SENT" : "❌ FAILED"));
         System.out.println("========================================");
         
         return ResponseEntity.ok(Map.of(
             "success", true,
             "message", "Return cancellation processed successfully",
             "orderId", orderId,
             "emailsSent", customerEmailSent && adminEmailSent,
             "customerEmailSent", customerEmailSent,
             "adminEmailSent", adminEmailSent
         ));
         
     } catch (Exception e) {
         System.err.println("========================================");
         System.err.println("❌ ERROR CANCELLING RETURN REQUEST");
         System.err.println("❌ Error: " + e.getMessage());
         e.printStackTrace();
         System.err.println("========================================");
         return ResponseEntity.status(500).body(Map.of(
             "success", false,
             "message", "Error cancelling return: " + e.getMessage()
         ));
     }
 }

    // ========================================
    // CREATE ORDER
    // ========================================
    
    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            
            System.out.println("========================================");
            System.out.println("🛒 ORDER REQUEST RECEIVED");
            System.out.println("User Email: " + userEmail);
            System.out.println("Payment ID: " + request.getPaymentId());
            System.out.println("Payment Method: " + request.getPaymentMethod());
            System.out.println("Items count: " + (request.getItems() != null ? request.getItems().size() : 0));
            System.out.println("Shipping: " + request.getShippingStreet() + ", " + request.getShippingCity() + ", " + request.getShippingState());
            System.out.println("========================================");
            
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
            
            System.out.println("✅ User found: ID=" + user.getId() + ", Email=" + user.getEmail());
            
            Order order = orderService.createOrder(request, user);
            
            // Force flush to database
            orderRepository.flush();
            
            System.out.println("========================================");
            System.out.println("✅ ORDER CREATED AND SAVED!");
            System.out.println("   Order ID: " + order.getId());
            System.out.println("   User ID: " + order.getUser().getId());
            System.out.println("   Total Amount: $" + order.getTotalAmount());
            System.out.println("   Items: " + order.getItems().size());
            System.out.println("   Payment ID: " + order.getPaymentId());
            System.out.println("========================================");
            
            // Verify it was saved by querying it back
            Order verifyOrder = orderRepository.findById(order.getId()).orElse(null);
            if (verifyOrder != null) {
                System.out.println("✅ VERIFIED: Order exists in database with ID: " + verifyOrder.getId());
            } else {
                System.err.println("❌ WARNING: Order not found in database after save!");
            }
            
            // Send confirmation email (async)
            try {
                emailService.sendOrderConfirmationEmail(order);
                System.out.println("📧 Order confirmation email queued for: " + user.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Failed to queue confirmation email: " + e.getMessage());
            }
            
            // Send notification email to ADMIN
            try {
                emailService.sendAdminOrderNotification(order);
                System.out.println("📧 Admin notification email queued");
            } catch (Exception e) {
                System.err.println("⚠️ Failed to queue admin email: " + e.getMessage());
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR CREATING ORDER");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Type: " + e.getClass().getName());
            System.err.println("========================================");
            e.printStackTrace();
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    @PutMapping("/admin/orders/{orderId}/return-tracking")
    public ResponseEntity<?> updateReturnTracking(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> request) {
        
        try {
            System.out.println("========================================");
            System.out.println("📦 UPDATE RETURN TRACKING REQUEST");
            System.out.println("📦 Order ID: " + orderId);
            System.out.println("📦 Request data: " + request);
            
            String returnTrackingNumber = (String) request.get("returnTrackingNumber");
            String returnStatusString = (String) request.get("returnStatus");
            Boolean includeTracking = (Boolean) request.get("includeTracking");
            
            System.out.println("📦 Return Status: " + returnStatusString);
            System.out.println("📦 Return Tracking: " + returnTrackingNumber);
            System.out.println("📦 Include Tracking in Email: " + includeTracking);
            
            // Update order in database
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Convert String to ReturnStatus enum
            Order.ReturnStatus returnStatus = Order.ReturnStatus.valueOf(returnStatusString);
            order.setReturnStatus(returnStatus);
            
            // If REFUND_ISSUED, also update main order status
            if (returnStatus == Order.ReturnStatus.REFUND_ISSUED) {
                order.setOrderStatus(Order.OrderStatus.RETURNED);
                System.out.println("✅ Order status set to RETURNED (Refund Issued)");
            }
            
            if (returnTrackingNumber != null && !returnTrackingNumber.isEmpty()) {
                order.setReturnTrackingNumber(returnTrackingNumber);
                
                // Detect carrier if tracking number is provided
                String carrier = carrierDetector.detectCarrier(returnTrackingNumber);
                order.setCarrier(carrier);
                System.out.println("📦 Detected Carrier: " + carrier);
            }
            
            orderRepository.save(order);
            orderRepository.flush();
            
            System.out.println("✅ Order updated in database");
            
            // Send email based on status
            if (returnStatus == Order.ReturnStatus.REFUND_ISSUED) {
                System.out.println("========================================");
                System.out.println("💰 SENDING REFUND CONFIRMATION EMAIL");
                
                // Send refund confirmation email
                emailService.sendRefundConfirmationEmail(
                    order.getUser().getEmail(),
                    order.getUser().getName(),
                    orderId,
                    order.getTotalAmount()
                );
                
                System.out.println("✅ Refund confirmation email sent successfully!");
                
            } else if (includeTracking != null && includeTracking) {
                System.out.println("========================================");
                System.out.println("📧 SENDING EMAIL WITH TRACKING DETAILS");
                
                // Send email WITH tracking details (only for LABEL_SENT status)
                emailService.sendReturnLabelWithTracking(
                    order.getUser().getEmail(),
                    order.getUser().getName(),
                    orderId,
                    returnTrackingNumber,
                    returnStatusString,
                    order.getCarrier()
                );
                
                System.out.println("✅ Email WITH tracking sent successfully!");
            } else {
                System.out.println("========================================");
                System.out.println("📧 SENDING EMAIL WITHOUT TRACKING DETAILS");
                
                // Send email WITHOUT tracking details (for all other statuses)
                emailService.sendReturnStatusUpdate(
                    order.getUser().getEmail(),
                    order.getUser().getName(),
                    orderId,
                    returnStatusString
                );
                
                System.out.println("✅ Email WITHOUT tracking sent successfully!");
            }
            
            System.out.println("========================================");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Return tracking updated successfully");
            response.put("returnStatus", returnStatusString);
            response.put("returnTrackingNumber", returnTrackingNumber);
            response.put("carrier", order.getCarrier());
            response.put("refundAmount", order.getTotalAmount());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("========================================");
            System.err.println("❌ INVALID RETURN STATUS");
            System.err.println("❌ Error: " + e.getMessage());
            System.err.println("========================================");
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid return status: " + e.getMessage()));
                    
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR UPDATING RETURN TRACKING");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    

    // ========================================
    // GET USER ORDERS
    // ========================================
    
    @GetMapping
    public ResponseEntity<?> getUserOrders() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            
            System.out.println("========================================");
            System.out.println("📦 Fetching orders for user: " + userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
            
            System.out.println("✅ Found " + orders.size() + " orders for user: " + userEmail);
            System.out.println("========================================");
            
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching user orders: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch orders: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ========================================
    // GET ORDER BY ID
    // ========================================
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!order.getUser().getId().equals(user.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized access to order");
                return ResponseEntity.status(403).body(error);
            }
            
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }

    // ========================================
    // RETURN REQUEST ENDPOINTS
    // ========================================
    
    @PostMapping("/{orderId}/return-request")
    public ResponseEntity<?> processReturnRequest(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> returnData,
            Authentication authentication) {
        
        return handleReturnRequest(orderId, returnData, authentication);
    }
    
    @PostMapping("/{orderId}/return")
    public ResponseEntity<?> processReturn(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> returnData,
            Authentication authentication) {
        
        return handleReturnRequest(orderId, returnData, authentication);
    }
    
    // Common method to handle return requests
    private ResponseEntity<?> handleReturnRequest(
            Long orderId,
            Map<String, Object> returnData,
            Authentication authentication) {
        
        try {
            System.out.println("========================================");
            System.out.println("🔄 RETURN REQUEST RECEIVED");
            System.out.println("🔄 Processing return request for Order #" + orderId);
            System.out.println("📦 Return data received: " + returnData);
            
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            // Verify the order belongs to the authenticated user
            String email = authentication.getName();
            if (!order.getUser().getEmail().equals(email)) {
                System.err.println("❌ Unauthorized: Order does not belong to user");
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Unauthorized access"
                ));
            }
            
            System.out.println("✅ Order found: " + order.getId());
            System.out.println("👤 Customer: " + order.getUser().getName() + " (" + order.getUser().getEmail() + ")");
            System.out.println("📧 Current order status: " + order.getOrderStatus());
            
            // Update order status to RETURNED
            order.setOrderStatus(Order.OrderStatus.RETURNED);
            order.setReturnRequestDate(java.time.LocalDateTime.now());
            Order savedOrder = orderRepository.save(order);
            orderRepository.flush();
            
            System.out.println("✅ Order status updated to RETURNED in database");
            System.out.println("✅ Saved order status: " + savedOrder.getOrderStatus());
            
            boolean customerEmailSent = false;
            boolean adminEmailSent = false;
            
            // Send confirmation email to customer
            try {
                System.out.println("========================================");
                System.out.println("📧 SENDING CUSTOMER EMAIL");
                System.out.println("📧 To: " + order.getUser().getEmail());
                System.out.println("📧 Customer Name: " + order.getUser().getName());
                
                emailService.sendReturnConfirmationEmail(savedOrder, returnData);
                customerEmailSent = true;
                
                System.out.println("✅ Customer return confirmation email sent successfully!");
            } catch (Exception emailError) {
                System.err.println("❌ Customer email FAILED!");
                System.err.println("❌ Error: " + emailError.getMessage());
                emailError.printStackTrace();
            }
            
            // Send notification email to admin
            try {
                System.out.println("========================================");
                System.out.println("📧 SENDING ADMIN EMAIL");
                System.out.println("📧 To: mdshariq2009@gmail.com");
                
                emailService.sendAdminReturnNotification(savedOrder, returnData);
                adminEmailSent = true;
                
                System.out.println("✅ Admin return notification email sent successfully!");
            } catch (Exception emailError) {
                System.err.println("❌ Admin email FAILED!");
                System.err.println("❌ Error: " + emailError.getMessage());
                emailError.printStackTrace();
            }
            
            System.out.println("========================================");
            System.out.println("📊 EMAIL STATUS SUMMARY:");
            System.out.println("   Customer Email: " + (customerEmailSent ? "✅ SENT" : "❌ FAILED"));
            System.out.println("   Admin Email: " + (adminEmailSent ? "✅ SENT" : "❌ FAILED"));
            System.out.println("========================================");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Return request processed successfully",
                "orderId", orderId,
                "emailsSent", customerEmailSent && adminEmailSent,
                "customerEmailSent", customerEmailSent,
                "adminEmailSent", adminEmailSent
            ));
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR PROCESSING RETURN REQUEST");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error processing return: " + e.getMessage()
            ));
        }
    }
}