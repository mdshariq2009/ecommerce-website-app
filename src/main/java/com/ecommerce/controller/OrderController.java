package com.ecommerce.controller;

import com.ecommerce.dto.OrderCalculation;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.model.Order;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.TaxService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final TaxService taxService;
    private final ProductRepository productRepository;
    
    @Autowired
    public OrderController(OrderService orderService, 
                          TaxService taxService,
                          ProductRepository productRepository) {
        this.orderService = orderService;
        this.taxService = taxService;
        this.productRepository = productRepository;
    }
    
    @PostMapping("/calculate")
    public ResponseEntity<OrderCalculation> calculateOrder(@RequestBody OrderRequest request) {
        // Calculate subtotal
        double subtotal = 0.0;
        for (OrderRequest.OrderItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            subtotal += product.getPrice() * item.getQuantity();
        }
        
        // Calculate shipping
        double shipping = taxService.calculateShipping(subtotal);
        
        // Calculate tax
        double taxRate = taxService.getTaxRate(request.getShippingState(), request.getShippingZipCode());
        double tax = taxService.calculateTax(subtotal, request.getShippingState(), request.getShippingZipCode());
        
        // Calculate total
        double total = subtotal + tax + shipping;
        
        OrderCalculation calculation = new OrderCalculation(subtotal, tax, taxRate, shipping, total);
        return ResponseEntity.ok(calculation);
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal User user) {
        Order order = orderService.createOrder(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getUserOrders(user));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getOrderById(id, user));
    }
}
