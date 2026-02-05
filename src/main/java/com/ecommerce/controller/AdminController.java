package com.ecommerce.controller;

import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.EmailService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ShippingLabelService;
import com.ecommerce.util.CarrierDetector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;  // Changed to final
    private final ShippingLabelService shippingLabelService;  // Changed to final
    
    @Autowired
    public AdminController(OrderService orderService, 
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          ProductRepository productRepository,
                          EmailService emailService,  // Added to constructor
                          ShippingLabelService shippingLabelService) {  // Added to constructor
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;  // Initialize
        this.shippingLabelService = shippingLabelService;  // Initialize
    }
    
    // ========================================
    // RETURNED ORDERS ENDPOINTS
    // ========================================
    
    // Get all returned orders
    @GetMapping("/returned-orders")
    public ResponseEntity<?> getReturnedOrders() {
        try {
            System.out.println("📦 Fetching returned orders for admin");
            
            List<Order> returnedOrders = orderRepository.findByOrderStatusOrderByReturnRequestDateDesc(Order.OrderStatus.RETURNED);
            
            System.out.println("✅ Found " + returnedOrders.size() + " returned orders");
            return ResponseEntity.ok(returnedOrders);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching returned orders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Preview shipping label in browser
    @GetMapping("/orders/{orderId}/preview-label")
    public ResponseEntity<?> previewShippingLabel(@PathVariable Long orderId) {
        try {
            System.out.println("========================================");
            System.out.println("👁️ Generating shipping label preview for Order #" + orderId);
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (order.getOrderStatus() != Order.OrderStatus.RETURNED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order is not in RETURNED status"));
            }
            
            String labelBase64 = shippingLabelService.generateReturnShippingLabelBase64(order);
            
            System.out.println("✅ Shipping label preview generated successfully");
            System.out.println("========================================");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "labelBase64", labelBase64,
                "orderId", orderId,
                "customerEmail", order.getUser().getEmail()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error generating shipping label preview: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Generate shipping label for download
    @GetMapping("/orders/{orderId}/generate-label")
    public ResponseEntity<?> generateShippingLabel(@PathVariable Long orderId) {
        try {
            System.out.println("========================================");
            System.out.println("📦 Generating shipping label for Order #" + orderId);
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (order.getOrderStatus() != Order.OrderStatus.RETURNED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order is not in RETURNED status"));
            }
            
            byte[] labelPdf = shippingLabelService.generateReturnShippingLabel(order);
            
            System.out.println("✅ Shipping label generated successfully");
            System.out.println("========================================");
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=UPS_Return_Label_Order_" + orderId + ".pdf")
                    .body(labelPdf);
            
        } catch (Exception e) {
            System.err.println("❌ Error generating shipping label: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Send shipping label via email
    @PostMapping("/orders/{orderId}/send-label")
    public ResponseEntity<?> sendShippingLabel(@PathVariable Long orderId) {
        try {
            System.out.println("========================================");
            System.out.println("📧 Sending shipping label for Order #" + orderId);
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (order.getOrderStatus() != Order.OrderStatus.RETURNED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order is not in RETURNED status"));
            }
            
            byte[] labelPdf = shippingLabelService.generateReturnShippingLabel(order);
            emailService.sendShippingLabelEmail(order, labelPdf);
            
            System.out.println("✅ Shipping label email sent to: " + order.getUser().getEmail());
            System.out.println("========================================");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Shipping label sent to " + order.getUser().getEmail()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error sending shipping label: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========================================
    // EXISTING ENDPOINTS
    // ========================================
    
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }
    
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        System.out.println("🗑️ Delete request for order #" + id);
        
        try {
            orderRepository.deleteById(id);
            System.out.println("✅ Order #" + id + " deleted successfully");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.err.println("❌ Failed to delete order: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PatchMapping("/orders/{id}")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String trackingNumber) {
        
        Order.OrderStatus newOrderStatus = orderStatus != null ? Order.OrderStatus.valueOf(orderStatus) : null;
        Order.PaymentStatus newPaymentStatus = paymentStatus != null ? Order.PaymentStatus.valueOf(paymentStatus) : null;
        
        Order order = orderService.updateOrderStatus(id, newOrderStatus, newPaymentStatus, trackingNumber);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalOrders = orderRepository.count();
            long totalUsers = userRepository.count();
            long totalProducts = productRepository.count();
            
            Double totalRevenue = orderRepository.findAll().stream()
                    .filter(order -> order.getPaymentStatus() == Order.PaymentStatus.COMPLETED)
                    .mapToDouble(Order::getTotalAmount)
                    .sum();
            
            stats.put("totalOrders", totalOrders);
            stats.put("totalUsers", totalUsers);
            stats.put("totalProducts", totalProducts);
            stats.put("totalRevenue", totalRevenue);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            stats.put("totalOrders", 0);
            stats.put("totalUsers", 0);
            stats.put("totalProducts", 0);
            stats.put("totalRevenue", 0.0);
            return ResponseEntity.ok(stats);
        }
    }
    
    @PutMapping("/orders/{orderId}/tracking")
    public ResponseEntity<?> updateTracking(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        
        String trackingNumber = request.get("trackingNumber");
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setTrackingNumber(trackingNumber);
        
        // Auto-detect carrier
        String carrier = CarrierDetector.detectCarrier(trackingNumber);
        order.setCarrier(carrier);
        
        System.out.println("📦 Tracking updated - Number: " + trackingNumber + ", Carrier: " + carrier);
        
        orderRepository.save(order);
        
        return ResponseEntity.ok(order);
    }
    
 // Send custom uploaded label via email
    @PostMapping("/orders/{orderId}/send-custom-label")
    public ResponseEntity<?> sendCustomShippingLabel(
            @PathVariable Long orderId,
            @RequestParam("labelFile") MultipartFile labelFile) {
        try {
            System.out.println("========================================");
            System.out.println("📧 Sending custom shipping label for Order #" + orderId);
            System.out.println("📎 File name: " + labelFile.getOriginalFilename());
            System.out.println("📎 File size: " + labelFile.getSize() + " bytes");
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (order.getOrderStatus() != Order.OrderStatus.RETURNED) {
                return ResponseEntity.badRequest().body(Map.of("error", "Order is not in RETURNED status"));
            }
            
            // Validate file
            if (labelFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            
            if (!"application/pdf".equals(labelFile.getContentType())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type. Please upload a PDF"));
            }
            
            // Validate file size (max 5MB)
            if (labelFile.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File too large. Maximum size is 5MB"));
            }
            
            byte[] labelPdf = labelFile.getBytes();
            emailService.sendShippingLabelEmail(order, labelPdf);
            
            System.out.println("✅ Custom shipping label email sent to: " + order.getUser().getEmail());
            System.out.println("========================================");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Custom shipping label sent to " + order.getUser().getEmail()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error sending custom shipping label: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}