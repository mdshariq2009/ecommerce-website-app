package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private TaxService taxService;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Transactional
    public Order createOrder(OrderRequest request, User user) {
        System.out.println("========================================");
        System.out.println("🛒 OrderService.createOrder() STARTED");
        System.out.println("User: " + user.getEmail() + " (ID: " + user.getId() + ")");
        System.out.println("Payment ID: " + request.getPaymentId());
        System.out.println("========================================");
        
        try {
            Order order = new Order();
            order.setUser(user);
            order.setPaymentId(request.getPaymentId());
            order.setPaymentMethod(request.getPaymentMethod());
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setOrderStatus(Order.OrderStatus.PENDING);
            
            // Set shipping address
            order.setShippingStreet(request.getShippingStreet());
            order.setShippingCity(request.getShippingCity());
            order.setShippingState(request.getShippingState());
            order.setShippingZipCode(request.getShippingZipCode());
            order.setShippingCountry(request.getShippingCountry());
            order.setCreatedAt(LocalDateTime.now());
            order.setCarrier("USPS");
            
            System.out.println("📍 Shipping: " + request.getShippingStreet() + ", " + request.getShippingCity());
            
            // Process items
            double subtotal = 0.0;
            List<OrderItem> orderItems = new ArrayList<>();
            
            System.out.println("📦 Processing " + request.getItems().size() + " items:");
            
            for (OrderRequest.OrderItemDTO itemDTO : request.getItems()) {
                Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemDTO.getProductId()));
                
                if (product.getStock() < itemDTO.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setProductName(product.getName());
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setPrice(product.getPrice());
                
                orderItems.add(orderItem);
                subtotal += product.getPrice() * itemDTO.getQuantity();
                
                // Update stock
                product.setStock(product.getStock() - itemDTO.getQuantity());
                productRepository.save(product);
                
                System.out.println("   ✅ " + product.getName() + " x" + itemDTO.getQuantity() + " @ $" + product.getPrice());
            }
            
            order.setItems(orderItems);
            
            // Calculate totals
            double shipping = taxService.calculateShipping(subtotal);
            double tax = taxService.calculateTax(subtotal, request.getShippingState(), request.getShippingZipCode());
            double total = subtotal + tax + shipping;
            
            order.setSubtotal(subtotal);
            order.setTax(tax);
            order.setShipping(shipping);
            order.setTotalAmount(total);
            
            System.out.println("💰 Order Totals:");
            System.out.println("   Subtotal: $" + subtotal);
            System.out.println("   Tax: $" + tax);
            System.out.println("   Shipping: $" + shipping);
            System.out.println("   TOTAL: $" + total);
            
            System.out.println("💾 Saving order to PostgreSQL database...");
            
            Order savedOrder = orderRepository.save(order);
            
            System.out.println("========================================");
            System.out.println("✅ ORDER SAVED SUCCESSFULLY!");
            System.out.println("   Order ID: " + savedOrder.getId());
            System.out.println("   User ID: " + savedOrder.getUser().getId());
            System.out.println("   Total Amount: $" + savedOrder.getTotalAmount());
            System.out.println("   Items Count: " + savedOrder.getItems().size());
            System.out.println("   Created At: " + savedOrder.getCreatedAt());
            System.out.println("========================================");
            
            return savedOrder;
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ ERROR IN OrderService.createOrder()");
            System.err.println("   Error Type: " + e.getClass().getName());
            System.err.println("   Error Message: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    public List<Order> getUserOrders(User user) {
        System.out.println("📦 Getting orders for user: " + user.getEmail());
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        System.out.println("✅ Found " + orders.size() + " orders");
        return orders;
    }
    
    public Order getOrderById(Long id, User user) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getUser().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("Access denied");
        }
        
        return order;
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Transactional
    public Order updateOrderStatus(Long id, Order.OrderStatus orderStatus, Order.PaymentStatus paymentStatus, String trackingNumber) {
        System.out.println("========================================");
        System.out.println("🔄 Updating Order #" + id);
        System.out.println("========================================");
        
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (orderStatus != null) {
            order.setOrderStatus(orderStatus);
            System.out.println("✅ Order Status: " + orderStatus);
        }
        
        if (paymentStatus != null) {
            order.setPaymentStatus(paymentStatus);
            System.out.println("✅ Payment Status: " + paymentStatus);
        }
        
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setTrackingNumber(trackingNumber.trim());
            System.out.println("✅ Tracking Number: " + trackingNumber);
        }
        
        Order updatedOrder = orderRepository.save(order);
        
        System.out.println("========================================");
        System.out.println("✅ Order #" + id + " updated successfully");
        System.out.println("========================================");
        
        // Send status update email
        if (emailService != null) {
            try {
                emailService.sendOrderStatusUpdateEmail(updatedOrder);
                System.out.println("📧 Status update email queued");
            } catch (Exception e) {
                System.err.println("⚠️ Failed to send status email: " + e.getMessage());
            }
        }
        
        return updatedOrder;
    }
    
    public java.util.Map<String, Object> calculateOrderTotals(User user, OrderRequest request) {
        double subtotal = 0.0;
        
        for (OrderRequest.OrderItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            subtotal += product.getPrice() * item.getQuantity();
        }
        
        double shipping = taxService.calculateShipping(subtotal);
        double taxRate = taxService.getTaxRate(request.getShippingState(), request.getShippingZipCode());
        double tax = taxService.calculateTax(subtotal, request.getShippingState(), request.getShippingZipCode());
        double total = subtotal + tax + shipping;
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("subtotal", subtotal);
        result.put("tax", tax);
        result.put("taxRate", taxRate);
        result.put("shipping", shipping);
        result.put("total", total);
        
        return result;
    }
}