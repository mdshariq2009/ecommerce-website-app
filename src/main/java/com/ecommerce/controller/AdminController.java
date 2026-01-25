package com.ecommerce.controller;

import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.CarrierDetector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    
    @Autowired
    public AdminController(OrderService orderService, 
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          ProductRepository productRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }
    
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
}
