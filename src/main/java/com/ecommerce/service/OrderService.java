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

import java.util.List;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TaxService taxService;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Autowired
    public OrderService(OrderRepository orderRepository, 
                       ProductRepository productRepository,
                       TaxService taxService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.taxService = taxService;
    }
    
    @Transactional
    public Order createOrder(OrderRequest request, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setShippingStreet(request.getShippingStreet());
        order.setShippingCity(request.getShippingCity());
        order.setShippingState(request.getShippingState());
        order.setShippingZipCode(request.getShippingZipCode());
        order.setShippingCountry(request.getShippingCountry());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentId(request.getPaymentId());
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        
        if (!"cod".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        }
        
        double subtotal = 0.0;
        
        for (OrderRequest.OrderItemDTO itemDTO : request.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            
            if (product.getStock() < itemDTO.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(product.getPrice());
            
            order.getItems().add(orderItem);
            
            subtotal += product.getPrice() * itemDTO.getQuantity();
            
            product.setStock(product.getStock() - itemDTO.getQuantity());
            productRepository.save(product);
        }
        
        // Calculate shipping
        double shipping = taxService.calculateShipping(subtotal);
        
        // Calculate tax
        double tax = taxService.calculateTax(subtotal, request.getShippingState(), request.getShippingZipCode());
        
        // Calculate total
        double total = subtotal + tax + shipping;
        
        order.setSubtotal(subtotal != 0.0 ? subtotal : 0.0);
        order.setTax(tax != 0.0 ? tax : 0.0);
        order.setShipping(shipping != 0.0 ? shipping : 0.0);
        order.setTotalAmount(total != 0.0 ? total : 0.0);
        
        Order savedOrder = orderRepository.save(order);
        
        // Send order confirmation email
        if (emailService != null) {
            try {
                emailService.sendOrderConfirmationEmail(savedOrder);
                System.out.println("📧 Sending order confirmation email...");
            } catch (Exception e) {
                System.err.println("⚠️ Email service unavailable: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️ Email service not configured - skipping email notification");
        }
        
        return savedOrder;
    }

    
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (orderStatus != null) {
            order.setOrderStatus(orderStatus);
            System.out.println("✅ Updated order status to: " + orderStatus);
        }
        
        if (paymentStatus != null) {
            order.setPaymentStatus(paymentStatus);
            System.out.println("✅ Updated payment status to: " + paymentStatus);
        }
        
        if (trackingNumber != null && !trackingNumber.trim().isEmpty()) {
            order.setTrackingNumber(trackingNumber.trim());
            System.out.println("✅ Updated tracking number to: " + trackingNumber);
        }
        
        Order updatedOrder = orderRepository.save(order);
        System.out.println("✅ Order saved successfully with ID: " + updatedOrder.getId());
        
        // Send status update email
        if (emailService != null) {
            try {
                emailService.sendOrderStatusUpdateEmail(updatedOrder);
                System.out.println("📧 Sending status update email...");
            } catch (Exception e) {
                System.err.println("⚠️ Email service unavailable: " + e.getMessage());
            }
        }
        
        return updatedOrder;
    }


}
