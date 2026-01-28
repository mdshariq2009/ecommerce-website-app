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
}