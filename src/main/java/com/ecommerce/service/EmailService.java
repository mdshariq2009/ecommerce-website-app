package com.ecommerce.service;

import com.ecommerce.dto.ContactFormDTO;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.util.CarrierDetector;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.name}")
    private String fromName;
    
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Order Confirmation - Order #" + order.getId());
            
            String emailContent = buildOrderConfirmationEmail(order);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Order confirmation email sent to: " + order.getUser().getEmail());
            
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error sending email: " + e.getMessage());
        }
    }
    
    
    
    private String buildOrderConfirmationEmail(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        double subtotal = 0;
        
        for (OrderItem item : order.getItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            subtotal += itemTotal;
            
            itemsHtml.append(String.format(
                "<tr>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td>" +
                "</tr>",
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                itemTotal
            ));
        }
        
        // Get values from order or calculate
        double orderSubtotal = order.getSubtotal() != null ? order.getSubtotal() : subtotal;
        double orderTax = order.getTax() != null ? order.getTax() : 0.0;
        double orderShipping = order.getShipping() != null ? order.getShipping() : 0.0;
        double orderTotal = order.getTotalAmount();
        
        // Add subtotal, shipping, tax rows
        itemsHtml.append(String.format(
            "<tr style='background:#f8f9fa;'>" +
            "  <td colspan='3' style='padding:12px;text-align:right;'>Subtotal:</td>" +
            "  <td style='padding:12px;text-align:right;font-weight:bold;'>$%.2f</td>" +
            "</tr>",
            orderSubtotal
        ));
        
        itemsHtml.append(String.format(
            "<tr style='background:#f8f9fa;'>" +
            "  <td colspan='3' style='padding:12px;text-align:right;'>Shipping:</td>" +
            "  <td style='padding:12px;text-align:right;font-weight:bold;color:%s;'>%s</td>" +
            "</tr>",
            orderShipping == 0 ? "#27ae60" : "#555",
            orderShipping == 0 ? "FREE" : String.format("$%.2f", orderShipping)
        ));
        
        itemsHtml.append(String.format(
            "<tr style='background:#f8f9fa;'>" +
            "  <td colspan='3' style='padding:12px;text-align:right;'>Tax:</td>" +
            "  <td style='padding:12px;text-align:right;font-weight:bold;'>$%.2f</td>" +
            "</tr>",
            orderTax
        ));
        
        itemsHtml.append(String.format(
            "<tr style='background:#27ae60;color:white;'>" +
            "  <td colspan='3' style='padding:15px;text-align:right;font-size:18px;'><strong>Total Amount:</strong></td>" +
            "  <td style='padding:15px;text-align:right;font-size:18px;'><strong>$%.2f</strong></td>" +
            "</tr>",
            orderTotal
        ));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        String orderDate = order.getCreatedAt().format(formatter);
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>🛒 Order Confirmed!</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>Thank you for your purchase</p>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <h2 style='color:#2c3e50;margin-top:0;'>Hello %s,</h2>" +
            "      <p style='color:#555;font-size:16px;line-height:1.6;'>Your order has been successfully placed and is being processed. We'll send you another email when your order ships.</p>" +
            "      " +
            "      <!-- Order Info Box -->" +
            "      <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #667eea;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#2c3e50;'>Order Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order Date:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order Status:</strong></td><td style='text-align:right;'><span style='background:#f39c12;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>%s</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Payment Status:</strong></td><td style='text-align:right;'><span style='background:#27ae60;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>%s</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Payment Method:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Order Items -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>Order Details</h3>" +
            "      <table style='width:100%%;border-collapse:collapse;'>" +
            "        <thead>" +
            "          <tr style='background:#34495e;color:white;'>" +
            "            <th style='padding:12px;text-align:left;'>Product</th>" +
            "            <th style='padding:12px;text-align:center;'>Qty</th>" +
            "            <th style='padding:12px;text-align:right;'>Price</th>" +
            "            <th style='padding:12px;text-align:right;'>Total</th>" +
            "          </tr>" +
            "        </thead>" +
            "        <tbody>" +
            "          %s" +
            "        </tbody>" +
            "      </table>" +
            "      " +
            "      <!-- Shipping Address -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>Shipping Address</h3>" +
            "      <div style='background:#f8f9fa;padding:20px;border-radius:8px;'>" +
            "        <p style='margin:5px 0;color:#555;'>%s</p>" +
            "        <p style='margin:5px 0;color:#555;'>%s, %s %s</p>" +
            "        <p style='margin:5px 0;color:#555;'>%s</p>" +
            "      </div>" +
            "      " +
            "      <!-- Call to Action -->" +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/orders' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>Track Your Order</a>" +
            "      </div>" +
            "      " +
            "      <p style='color:#7f8c8d;font-size:14px;margin-top:30px;'>If you have any questions about your order, please contact our customer support.</p>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Thank you for shopping with us!</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2025 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            order.getUser().getName(),
            order.getId(),
            orderDate,
            order.getOrderStatus(),
            order.getPaymentStatus(),
            order.getPaymentMethod().toUpperCase(),
            itemsHtml.toString(),
            order.getShippingStreet(),
            order.getShippingCity(),
            order.getShippingState(),
            order.getShippingZipCode(),
            order.getShippingCountry()
        );
    }
    
    @Async
    public void sendAdminOrderNotification(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Send to admin email
            helper.setFrom(fromEmail, fromName);
            helper.setTo("mdshariq2009@gmail.com");  // Admin email
            helper.setSubject("New Order Received - Order #" + order.getId());
            helper.setReplyTo(order.getUser().getEmail());  // Reply goes to customer
            
            String emailContent = buildAdminOrderNotificationEmail(order);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Admin notification email sent for Order #" + order.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send admin notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildAdminOrderNotificationEmail(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        double subtotal = 0;
        
        for (OrderItem item : order.getItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            subtotal += itemTotal;
            
            itemsHtml.append(String.format(
                "<tr>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td>" +
                "</tr>",
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                itemTotal
            ));
        }
        
        double orderSubtotal = order.getSubtotal() != null ? order.getSubtotal() : subtotal;
        double orderTax = order.getTax() != null ? order.getTax() : 0.0;
        double orderShipping = order.getShipping() != null ? order.getShipping() : 0.0;
        double orderTotal = order.getTotalAmount();
        
        // Add totals rows
        itemsHtml.append(String.format(
            "<tr style='background:#f8f9fa;'>" +
            "  <td colspan='3' style='padding:12px;text-align:right;font-weight:600;'>Subtotal:</td>" +
            "  <td style='padding:12px;text-align:right;font-weight:bold;'>$%.2f</td>" +
            "</tr>",
            orderSubtotal
        ));
        
        itemsHtml.append(String.format(
            "<tr style='background:#f8f9fa;'>" +
            "  <td colspan='3' style='padding:12px;text-align:right;font-weight:600;'>Tax:</td>" +
            "  <td style='padding:12px;text-align:right;font-weight:bold;'>$%.2f</td>" +
            "</tr>",
            orderTax
        ));
        
        itemsHtml.append(String.format(
            "<tr style='background:#f8f9fa;'>" +
            "  <td colspan='3' style='padding:12px;text-align:right;font-weight:600;'>Shipping:</td>" +
            "  <td style='padding:12px;text-align:right;font-weight:bold;color:%s;'>%s</td>" +
            "</tr>",
            orderShipping == 0 ? "#27ae60" : "#555",
            orderShipping == 0 ? "FREE" : String.format("$%.2f", orderShipping)
        ));
        
        itemsHtml.append(String.format(
            "<tr style='background:#667eea;color:white;'>" +
            "  <td colspan='3' style='padding:15px;text-align:right;font-size:18px;'><strong>TOTAL:</strong></td>" +
            "  <td style='padding:15px;text-align:right;font-size:18px;'><strong>$%.2f</strong></td>" +
            "</tr>",
            orderTotal
        ));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        String orderDate = order.getCreatedAt().format(formatter);
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>🔔 New Order Alert!</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>A customer just placed an order</p>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <div style='background:#fee2e2;padding:20px;border-radius:8px;margin-bottom:25px;border-left:4px solid #ef4444;'>" +
            "        <h3 style='margin:0 0 10px 0;color:#991b1b;'>⚡ Action Required</h3>" +
            "        <p style='margin:0;color:#991b1b;'>A new order has been placed and requires your attention.</p>" +
            "      </div>" +
            "      " +
            "      <!-- Order Info -->" +
            "      <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #667eea;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#2c3e50;'>📦 Order Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order Date:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order Status:</strong></td><td style='text-align:right;'><span style='background:#f39c12;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>%s</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Payment Status:</strong></td><td style='text-align:right;'><span style='background:#27ae60;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>%s</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Payment Method:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Customer Info -->" +
            "      <div style='background:#dbeafe;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #3b82f6;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#1e40af;'>👤 Customer Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Name:</strong></td><td style='text-align:right;color:#1f2937;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Email:</strong></td><td style='text-align:right;color:#1f2937;'><a href='mailto:%s' style='color:#3b82f6;text-decoration:none;'>%s</a></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Phone:</strong></td><td style='text-align:right;color:#1f2937;'>%s</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Order Items -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📋 Order Details</h3>" +
            "      <table style='width:100%%;border-collapse:collapse;'>" +
            "        <thead>" +
            "          <tr style='background:#34495e;color:white;'>" +
            "            <th style='padding:12px;text-align:left;'>Product</th>" +
            "            <th style='padding:12px;text-align:center;'>Qty</th>" +
            "            <th style='padding:12px;text-align:right;'>Price</th>" +
            "            <th style='padding:12px;text-align:right;'>Total</th>" +
            "          </tr>" +
            "        </thead>" +
            "        <tbody>" +
            "          %s" +
            "        </tbody>" +
            "      </table>" +
            "      " +
            "      <!-- Shipping Address -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📍 Shipping Address</h3>" +
            "      <div style='background:#f8f9fa;padding:20px;border-radius:8px;'>" +
            "        <p style='margin:5px 0;color:#555;'>%s</p>" +
            "        <p style='margin:5px 0;color:#555;'>%s, %s %s</p>" +
            "        <p style='margin:5px 0;color:#555;'>%s</p>" +
            "      </div>" +
            "      " +
            "      <!-- Admin Actions -->" +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/admin' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;margin-right:10px;'>View in Admin Dashboard</a>" +
            "        <a href='mailto:%s' style='display:inline-block;background:#27ae60;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>Contact Customer</a>" +
            "      </div>" +
            "      " +
            "      <div style='background:#fff3cd;border-left:4px solid #ffc107;padding:15px;border-radius:4px;margin-top:20px;'>" +
            "        <p style='margin:0;color:#856404;'><strong>⚡ Quick Actions:</strong></p>" +
            "        <p style='margin:5px 0 0 0;color:#856404;font-size:14px;'>Log into the admin dashboard to process this order, update status, and add tracking information.</p>" +
            "      </div>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Admin Notification - E-Commerce Store</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            order.getId(),
            orderDate,
            order.getOrderStatus(),
            order.getPaymentStatus(),
            order.getPaymentMethod().toUpperCase(),
            order.getUser().getName(),
            order.getUser().getEmail(),
            order.getUser().getEmail(),
            order.getUser().getPhoneNumber() != null ? order.getUser().getPhoneNumber() : "N/A",
            itemsHtml.toString(),
            order.getShippingStreet(),
            order.getShippingCity(),
            order.getShippingState(),
            order.getShippingZipCode(),
            order.getShippingCountry(),
            order.getUser().getEmail()
        );
    }

    
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to E-Commerce Store! 🎉");
            
            String emailContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
                "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
                "    <div style='background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);color:white;padding:40px 20px;text-align:center;'>" +
                "      <h1 style='margin:0;font-size:32px;'>Welcome! 🎉</h1>" +
                "    </div>" +
                "    <div style='padding:30px;'>" +
                "      <h2 style='color:#2c3e50;'>Hello %s,</h2>" +
                "      <p style='color:#555;font-size:16px;line-height:1.6;'>Thank you for joining E-Commerce Store!</p>" +
                "      <p style='color:#555;font-size:16px;line-height:1.6;'>You can now browse our products and start shopping.</p>" +
                "      <div style='text-align:center;margin:30px 0;'>" +
                "        <a href='http://localhost:8080/web/products' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>Start Shopping</a>" +
                "      </div>" +
                "    </div>" +
                "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
                "      <p style='margin:0;color:#95a5a6;'>© 2025 E-Commerce Store</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>",
                userName
            );
            
            helper.setText(emailContent, true);
            mailSender.send(message);
            
            System.out.println("✅ Welcome email sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send welcome email: " + e.getMessage());
        }
    }
    
    @Async
    public void sendOrderStatusUpdateEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Order Status Update - Order #" + order.getId());
            
            String statusColor = "#f59e0b";
            String statusIcon = "📦";
            
            switch (order.getOrderStatus()) {
                case SHIPPED:
                    statusColor = "#3b82f6";
                    statusIcon = "🚚";
                    break;
                case DELIVERED:
                    statusColor = "#10b981";
                    statusIcon = "✅";
                    break;
                case CANCELLED:
                    statusColor = "#ef4444";
                    statusIcon = "❌";
                    break;
                default:
                    statusColor = "#f59e0b";
                    statusIcon = "📦";
            }
            
            // Tracking section
            String trackingSection = "";
            if (order.getTrackingNumber() != null && !order.getTrackingNumber().isEmpty() && 
                (order.getOrderStatus() == Order.OrderStatus.SHIPPED || order.getOrderStatus() == Order.OrderStatus.DELIVERED)) {
                
                // Get carrier (auto-detected or default)
                String carrier = order.getCarrier() != null ? order.getCarrier() : 
                                 CarrierDetector.detectCarrier(order.getTrackingNumber());
                
                String trackingUrl = CarrierDetector.getTrackingUrl(order.getTrackingNumber(), carrier);
                
                trackingSection = String.format(
                    "<div style='background:#dbeafe;padding:20px;border-radius:12px;margin:20px 0;border-left:4px solid #3b82f6;'>" +
                    "  <h3 style='margin:0 0 10px 0;color:#1e40af;'>📦 Tracking Information</h3>" +
                    "  <p style='margin:5px 0;color:#1e40af;'><strong>Carrier:</strong> %s</p>" +
                    "  <p style='margin:5px 0;color:#1e40af;'><strong>Tracking Number:</strong></p>" +
                    "  <p style='font-family:monospace;font-size:1.3em;font-weight:bold;color:#3b82f6;margin:10px 0;'>%s</p>" +
                    "  <a href='%s' style='display:inline-block;background:#3b82f6;color:white;padding:10px 20px;text-decoration:none;border-radius:8px;margin-top:10px;'>Track Your Package →</a>" +
                    "</div>",
                    carrier,
                    order.getTrackingNumber(),
                    trackingUrl
                );
            }
            
            String emailContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
                "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
                "    <div style='background:%s;color:white;padding:40px 20px;text-align:center;'>" +
                "      <h1 style='margin:0;font-size:32px;'>%s Order Update</h1>" +
                "    </div>" +
                "    <div style='padding:30px;'>" +
                "      <h2 style='color:#1f2937;'>Hello %s,</h2>" +
                "      <p style='color:#555;font-size:16px;line-height:1.6;'>Your order status has been updated!</p>" +
                "      " +
                "      <div style='background:#f9fafb;padding:20px;border-radius:12px;margin:25px 0;border-left:4px solid %s;'>" +
                "        <h3 style='margin:0 0 15px 0;color:#1f2937;'>Order Status</h3>" +
                "        <table style='width:100%%;'>" +
                "          <tr><td style='padding:5px 0;color:#555;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
                "          <tr><td style='padding:5px 0;color:#555;'><strong>Status:</strong></td><td style='text-align:right;'><span style='background:%s;color:white;padding:4px 12px;border-radius:4px;font-weight:bold;'>%s</span></td></tr>" +
                "          <tr><td style='padding:5px 0;color:#555;'><strong>Payment:</strong></td><td style='text-align:right;'>%s</td></tr>" +
                "        </table>" +
                "      </div>" +
                "      %s" +
                "      <div style='text-align:center;margin:30px 0;'>" +
                "        <a href='http://localhost:8080/web/orders' style='display:inline-block;background:#3b82f6;color:white;padding:12px 30px;text-decoration:none;border-radius:8px;font-weight:bold;'>View Order Details</a>" +
                "      </div>" +
                "    </div>" +
                "    <div style='background:#1f2937;color:white;padding:20px;text-align:center;'>" +
                "      <p style='margin:0;color:#9ca3af;'>© 2025 E-Commerce Store</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>",
                statusColor,
                statusIcon,
                order.getUser().getName(),
                statusColor,
                order.getId(),
                statusColor,
                order.getOrderStatus(),
                order.getPaymentStatus(),
                trackingSection
            );
            
            helper.setText(emailContent, true);
            mailSender.send(message);
            
            System.out.println("✅ Order status update email sent to: " + order.getUser().getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send status update email: " + e.getMessage());
        }
    }

    // ========== NEW: RETURN REQUEST EMAILS ==========
    
   
    public void sendReturnConfirmationEmail(Order order, Map<String, Object> returnData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Return Request Confirmed - Order #" + order.getId());
            
            String emailContent = buildReturnConfirmationEmail(order, returnData);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Return confirmation email sent to: " + order.getUser().getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send return confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildReturnConfirmationEmail(Order order, Map<String, Object> returnData) {
        StringBuilder itemsHtml = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) returnData.get("items");
        
        if (items != null) {
            for (Map<String, Object> item : items) {
                String productName = (String) item.get("productName");
                Integer quantity = (Integer) item.get("quantity");
                Double price = ((Number) item.get("price")).doubleValue();
                double itemTotal = price * quantity;
                
                itemsHtml.append(String.format(
                    "<tr>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td>" +
                    "</tr>",
                    productName, quantity, price, itemTotal
                ));
            }
        }
        
        String customerName = (String) returnData.get("customerName");
        Double orderAmount = ((Number) returnData.get("orderAmount")).doubleValue();
        String returnRequestDate = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>🔄 Return Request Confirmed</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>We've received your return request</p>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <h2 style='color:#2c3e50;margin-top:0;'>Hello %s,</h2>" +
            "      <p style='color:#555;font-size:16px;line-height:1.6;'>Your return request for Order #%d has been successfully submitted and confirmed.</p>" +
            "      " +
            "      <!-- Return Info Box -->" +
            "      <div style='background:#fef3c7;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #f59e0b;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#92400e;'>🔄 Return Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Return Request Date:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Return Status:</strong></td><td style='text-align:right;'><span style='background:#f59e0b;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>PENDING PICKUP</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Refund Amount:</strong></td><td style='text-align:right;font-weight:bold;font-size:18px;color:#10b981;'>$%.2f</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- What's Next -->" +
            "      <div style='background:#dbeafe;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #3b82f6;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#1e40af;'>📞 What Happens Next?</h3>" +
            "        <div style='color:#1e40af;line-height:1.8;'>" +
            "          <p style='margin:8px 0;'>✓ Our customer service team will contact you within <strong>24-48 hours</strong></p>" +
            "          <p style='margin:8px 0;'>✓ We will arrange a convenient pickup time with you</p>" +
            "          <p style='margin:8px 0;'>✓ Please keep the item in its <strong>original packaging</strong></p>" +
            "          <p style='margin:8px 0;'>✓ Refund will be processed within 5-7 business days after inspection</p>" +
            "        </div>" +
            "      </div>" +
            "      " +
            "      <!-- Items Being Returned -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📦 Items Being Returned</h3>" +
            "      <table style='width:100%%;border-collapse:collapse;'>" +
            "        <thead>" +
            "          <tr style='background:#34495e;color:white;'>" +
            "            <th style='padding:12px;text-align:left;'>Product</th>" +
            "            <th style='padding:12px;text-align:center;'>Qty</th>" +
            "            <th style='padding:12px;text-align:right;'>Price</th>" +
            "            <th style='padding:12px;text-align:right;'>Total</th>" +
            "          </tr>" +
            "        </thead>" +
            "        <tbody>" +
            "          %s" +
            "        </tbody>" +
            "      </table>" +
            "      " +
            "      <!-- Important Notice -->" +
            "      <div style='background:#fff3cd;border-left:4px solid #ffc107;padding:15px;border-radius:4px;margin-top:25px;'>" +
            "        <p style='margin:0;color:#856404;'><strong>⚠️ Important:</strong></p>" +
            "        <p style='margin:5px 0 0 0;color:#856404;font-size:14px;'>Please ensure all items are in their original condition with tags attached. Items must be unused and in resalable condition to qualify for a full refund.</p>" +
            "      </div>" +
            "      " +
            "      <!-- Call to Action -->" +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/orders' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>View Order Details</a>" +
            "      </div>" +
            "      " +
            "      <p style='color:#7f8c8d;font-size:14px;margin-top:30px;'>If you have any questions about your return, please contact our customer support at <a href='mailto:support@ecommerce.com' style='color:#3498db;'>support@ecommerce.com</a></p>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Thank you for shopping with us!</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            customerName,
            order.getId(),
            order.getId(),
            returnRequestDate,
            orderAmount,
            itemsHtml.toString()
        );
    }

    @Async
    public void sendAdminReturnNotification(Order order, Map<String, Object> returnData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo("mdshariq2009@gmail.com");  // Admin email
            helper.setSubject("🔄 New Return Request - Order #" + order.getId());
            helper.setReplyTo((String) returnData.get("customerEmail"));
            
            String emailContent = buildAdminReturnNotificationEmail(order, returnData);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Admin return notification email sent for Order #" + order.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send admin return notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildAdminReturnNotificationEmail(Order order, Map<String, Object> returnData) {
        StringBuilder itemsHtml = new StringBuilder();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) returnData.get("items");
        
        int itemCount = 0;
        if (items != null) {
            itemCount = items.size();
            for (Map<String, Object> item : items) {
                String productName = (String) item.get("productName");
                Integer quantity = item.get("quantity") instanceof Integer ? 
                    (Integer) item.get("quantity") : 
                    Integer.parseInt(item.get("quantity").toString());
                Double price = item.get("price") instanceof Double ? 
                    (Double) item.get("price") : 
                    Double.parseDouble(item.get("price").toString());
                double itemTotal = price * quantity;
                
                itemsHtml.append(String.format(
                    "<tr>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td>" +
                    "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td>" +
                    "</tr>",
                    productName, quantity, price, itemTotal
                ));
            }
        }
        
        String customerName = (String) returnData.get("customerName");
        String customerEmail = (String) returnData.get("customerEmail");
        Double orderAmount = returnData.get("orderAmount") instanceof Double ? 
            (Double) returnData.get("orderAmount") : 
            Double.parseDouble(returnData.get("orderAmount").toString());
        
        String shippingStreet = (String) returnData.get("shippingStreet");
        String shippingCity = (String) returnData.get("shippingCity");
        String shippingState = (String) returnData.get("shippingState");
        String shippingZipCode = (String) returnData.get("shippingZipCode");
        String shippingCountry = (String) returnData.get("shippingCountry");
        
        String returnRequestDate = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>🔄 Return Request Alert!</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>A customer has requested a return</p>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <div style='background:#fee2e2;padding:20px;border-radius:8px;margin-bottom:25px;border-left:4px solid #ef4444;'>" +
            "        <h3 style='margin:0 0 10px 0;color:#991b1b;'>⚡ Action Required - Contact Customer Within 24-48 Hours</h3>" +
            "        <p style='margin:0;color:#991b1b;'>A return request has been submitted and requires your immediate attention.</p>" +
            "      </div>" +
            "      " +
            "      <!-- Return Info -->" +
            "      <div style='background:#fef3c7;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #f59e0b;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#92400e;'>🔄 Return Request Details</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Return Request Date:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#92400e;'><strong>Refund Amount:</strong></td><td style='text-align:right;font-weight:bold;font-size:18px;color:#ef4444;'>$%.2f</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Customer Info -->" +
            "      <div style='background:#dbeafe;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #3b82f6;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#1e40af;'>👤 Customer Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Name:</strong></td><td style='text-align:right;color:#1f2937;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Email:</strong></td><td style='text-align:right;color:#1f2937;'><a href='mailto:%s' style='color:#3b82f6;text-decoration:none;'>%s</a></td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Items Being Returned -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📦 Items Being Returned (%d items)</h3>" +
            "      <table style='width:100%%;border-collapse:collapse;'>" +
            "        <thead>" +
            "          <tr style='background:#34495e;color:white;'>" +
            "            <th style='padding:12px;text-align:left;'>Product</th>" +
            "            <th style='padding:12px;text-align:center;'>Qty</th>" +
            "            <th style='padding:12px;text-align:right;'>Price</th>" +
            "            <th style='padding:12px;text-align:right;'>Total</th>" +
            "          </tr>" +
            "        </thead>" +
            "        <tbody>" +
            "          %s" +
            "        </tbody>" +
            "      </table>" +
            "      " +
            "      <!-- Pickup Address -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📍 Pickup Address</h3>" +
            "      <div style='background:#f8f9fa;padding:20px;border-radius:8px;'>" +
            "        <p style='margin:5px 0;color:#555;'><strong>%s</strong></p>" +
            "        <p style='margin:5px 0;color:#555;'>%s, %s %s</p>" +
            "        <p style='margin:5px 0;color:#555;'>%s</p>" +
            "      </div>" +
            "      " +
            "      <!-- Action Items -->" +
            "      <div style='background:#e0e7ff;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #667eea;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#4338ca;'>✅ Required Actions</h3>" +
            "        <div style='color:#4338ca;line-height:1.8;'>" +
            "          <p style='margin:8px 0;'>1️⃣ Contact customer within 24-48 hours</p>" +
            "          <p style='margin:8px 0;'>2️⃣ Schedule a pickup time</p>" +
            "          <p style='margin:8px 0;'>3️⃣ Arrange return shipping/pickup</p>" +
            "          <p style='margin:8px 0;'>4️⃣ Inspect item upon receipt</p>" +
            "          <p style='margin:8px 0;'>5️⃣ Process refund after inspection</p>" +
            "        </div>" +
            "      </div>" +
            "      " +
            "      <!-- Admin Actions -->" +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/admin' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;margin-right:10px;'>View in Admin Dashboard</a>" +
            "        <a href='mailto:%s' style='display:inline-block;background:#f59e0b;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>Contact Customer</a>" +
            "      </div>" +
            "      " +
            "      <div style='background:#fee2e2;border-left:4px solid #ef4444;padding:15px;border-radius:4px;margin-top:20px;'>" +
            "        <p style='margin:0;color:#991b1b;'><strong>🚨 Priority Action:</strong></p>" +
            "        <p style='margin:5px 0 0 0;color:#991b1b;font-size:14px;'>This return request requires immediate attention. Please contact the customer as soon as possible to arrange pickup.</p>" +
            "      </div>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Admin Notification - E-Commerce Store</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            order.getId(),           // Order ID
            returnRequestDate,       // Return request date
            orderAmount,             // Refund amount
            customerName,            // Customer name
            customerEmail,           // Customer email (first)
            customerEmail,           // Customer email (second)
            itemCount,               // Number of items
            itemsHtml.toString(),    // Items HTML
            shippingStreet,          // Street
            shippingCity,            // City
            shippingState,           // State
            shippingZipCode,         // Zip
            shippingCountry,         // Country
            customerEmail            // Customer email for mailto
        );
    }
    
 // ========== CANCEL RETURN EMAIL NOTIFICATIONS ==========

    @Async
    public void sendCancelReturnConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Return Cancelled - Order #" + order.getId());
            
            String emailContent = buildCancelReturnConfirmationEmail(order);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Cancel return confirmation email sent to: " + order.getUser().getEmail());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send cancel return confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildCancelReturnConfirmationEmail(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        
        for (OrderItem item : order.getItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            
            itemsHtml.append(String.format(
                "<tr>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td>" +
                "</tr>",
                item.getProductName(), item.getQuantity(), item.getPrice(), itemTotal
            ));
        }
        
        String cancelDate = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #10b981 0%%, #059669 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>✅ Return Cancelled</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>Your return request has been cancelled</p>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <h2 style='color:#2c3e50;margin-top:0;'>Hello %s,</h2>" +
            "      <p style='color:#555;font-size:16px;line-height:1.6;'>Your return request for Order #%d has been successfully cancelled.</p>" +
            "      " +
            "      <!-- Status Update Box -->" +
            "      <div style='background:#d1fae5;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #10b981;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#065f46;'>📦 Order Status Updated</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#065f46;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#065f46;'><strong>Cancellation Date:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#065f46;'><strong>Current Status:</strong></td><td style='text-align:right;'><span style='background:#10b981;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>DELIVERED</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#065f46;'><strong>Order Total:</strong></td><td style='text-align:right;font-weight:bold;font-size:18px;color:#10b981;'>$%.2f</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- What This Means -->" +
            "      <div style='background:#dbeafe;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #3b82f6;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#1e40af;'>ℹ️ What This Means</h3>" +
            "        <div style='color:#1e40af;line-height:1.8;'>" +
            "          <p style='margin:8px 0;'>✓ Your return request has been cancelled</p>" +
            "          <p style='margin:8px 0;'>✓ Your order status is now <strong>DELIVERED</strong></p>" +
            "          <p style='margin:8px 0;'>✓ No pickup will be scheduled</p>" +
            "          <p style='margin:8px 0;'>✓ You can keep your items</p>" +
            "          <p style='margin:8px 0;'>✓ You can submit a new return request if needed (within 30 days of delivery)</p>" +
            "        </div>" +
            "      </div>" +
            "      " +
            "      <!-- Order Items -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📦 Order Items</h3>" +
            "      <table style='width:100%%;border-collapse:collapse;'>" +
            "        <thead>" +
            "          <tr style='background:#34495e;color:white;'>" +
            "            <th style='padding:12px;text-align:left;'>Product</th>" +
            "            <th style='padding:12px;text-align:center;'>Qty</th>" +
            "            <th style='padding:12px;text-align:right;'>Price</th>" +
            "            <th style='padding:12px;text-align:right;'>Total</th>" +
            "          </tr>" +
            "        </thead>" +
            "        <tbody>" +
            "          %s" +
            "        </tbody>" +
            "      </table>" +
            "      " +
            "      <!-- Call to Action -->" +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/orders' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>View Order Details</a>" +
            "      </div>" +
            "      " +
            "      <p style='color:#7f8c8d;font-size:14px;margin-top:30px;'>If you have any questions, please contact our customer support at <a href='mailto:support@ecommerce.com' style='color:#3498db;'>support@ecommerce.com</a></p>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Thank you for shopping with us!</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            order.getUser().getName(),
            order.getId(),
            order.getId(),
            cancelDate,
            order.getTotalAmount(),
            itemsHtml.toString()
        );
    }

    @Async
    public void sendAdminCancelReturnNotification(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo("mdshariq2009@gmail.com");
            helper.setSubject("Return Cancelled - Order #" + order.getId());
            helper.setReplyTo(order.getUser().getEmail());
            
            String emailContent = buildAdminCancelReturnNotificationEmail(order);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Admin cancel return notification email sent for Order #" + order.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send admin cancel return notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildAdminCancelReturnNotificationEmail(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        
        for (OrderItem item : order.getItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            
            itemsHtml.append(String.format(
                "<tr>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td>" +
                "  <td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td>" +
                "</tr>",
                item.getProductName(), item.getQuantity(), item.getPrice(), itemTotal
            ));
        }
        
        String cancelDate = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #6b7280 0%%, #4b5563 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>ℹ️ Return Cancelled</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>Customer cancelled their return request</p>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <div style='background:#e0e7ff;padding:20px;border-radius:8px;margin-bottom:25px;border-left:4px solid #667eea;'>" +
            "        <h3 style='margin:0 0 10px 0;color:#4338ca;'>ℹ️ Return Request Cancelled</h3>" +
            "        <p style='margin:0;color:#4338ca;'>The customer has cancelled their return request. No further action is required.</p>" +
            "      </div>" +
            "      " +
            "      <!-- Order Info -->" +
            "      <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #667eea;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#2c3e50;'>📦 Order Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order ID:</strong></td><td style='text-align:right;'>#%d</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Cancellation Date:</strong></td><td style='text-align:right;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Current Status:</strong></td><td style='text-align:right;'><span style='background:#10b981;color:white;padding:4px 12px;border-radius:4px;font-size:12px;font-weight:bold;'>DELIVERED</span></td></tr>" +
            "          <tr><td style='padding:5px 0;color:#555;'><strong>Order Total:</strong></td><td style='text-align:right;font-weight:bold;font-size:18px;color:#10b981;'>$%.2f</td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Customer Info -->" +
            "      <div style='background:#dbeafe;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #3b82f6;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#1e40af;'>👤 Customer Information</h3>" +
            "        <table style='width:100%%;'>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Name:</strong></td><td style='text-align:right;color:#1f2937;'>%s</td></tr>" +
            "          <tr><td style='padding:5px 0;color:#1e40af;'><strong>Email:</strong></td><td style='text-align:right;color:#1f2937;'><a href='mailto:%s' style='color:#3b82f6;text-decoration:none;'>%s</a></td></tr>" +
            "        </table>" +
            "      </div>" +
            "      " +
            "      <!-- Order Items -->" +
            "      <h3 style='color:#2c3e50;margin:30px 0 15px 0;'>📦 Order Items</h3>" +
            "      <table style='width:100%%;border-collapse:collapse;'>" +
            "        <thead>" +
            "          <tr style='background:#34495e;color:white;'>" +
            "            <th style='padding:12px;text-align:left;'>Product</th>" +
            "            <th style='padding:12px;text-align:center;'>Qty</th>" +
            "            <th style='padding:12px;text-align:right;'>Price</th>" +
            "            <th style='padding:12px;text-align:right;'>Total</th>" +
            "          </tr>" +
            "        </thead>" +
            "        <tbody>" +
            "          %s" +
            "        </tbody>" +
            "      </table>" +
            "      " +
            "      <!-- Info Notice -->" +
            "      <div style='background:#fef3c7;border-left:4px solid #f59e0b;padding:15px;border-radius:4px;margin-top:25px;'>" +
            "        <p style='margin:0;color:#92400e;'><strong>ℹ️ Note:</strong></p>" +
            "        <p style='margin:5px 0 0 0;color:#92400e;font-size:14px;'>No pickup will be scheduled. The order remains in DELIVERED status. The customer can submit a new return request if needed within 30 days of delivery.</p>" +
            "      </div>" +
            "      " +
            "      <!-- Call to Action -->" +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/admin' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>View in Admin Dashboard</a>" +
            "      </div>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Admin Notification - E-Commerce Store</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            order.getId(),
            cancelDate,
            order.getTotalAmount(),
            order.getUser().getName(),
            order.getUser().getEmail(),
            order.getUser().getEmail(),
            itemsHtml.toString()
        );
    }
    
    @Async
    public void sendContactFormEmail(ContactFormDTO contactForm) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Send to your email (mdshariq2009@gmail.com)
            helper.setFrom(fromEmail, fromName);
            helper.setTo("mdshariq2009@gmail.com");
            helper.setSubject("New Contact Form Submission - " + contactForm.getSubject());
            helper.setReplyTo(contactForm.getEmail());
            
            String emailContent = buildContactFormEmail(contactForm);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            System.out.println("✅ Contact form email sent to: mdshariq2009@gmail.com");
            
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send contact form email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error sending contact form email: " + e.getMessage());
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }

    private String buildContactFormEmail(ContactFormDTO contactForm) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
        
        String subjectLabel = getContactSubjectLabel(contactForm.getSubject());
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "  <meta charset='UTF-8'>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f9fafb;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.1);'>" +
            "    <!-- Header -->" +
            "    <div style='background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);color:white;padding:30px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:24px;font-weight:800;'>🛒 E-Commerce Store</h1>" +
            "      <p style='margin:10px 0 0 0;opacity:0.9;font-size:14px;'>New Contact Form Submission</p>" +
            "      <span style='display:inline-block;background:#10b981;color:white;padding:4px 12px;border-radius:12px;font-size:11px;font-weight:bold;text-transform:uppercase;margin-top:10px;'>New Message</span>" +
            "    </div>" +
            "    " +
            "    <!-- Content -->" +
            "    <div style='padding:30px;'>" +
            "      <!-- Customer Info -->" +
            "      <div style='margin-bottom:20px;padding:15px;background:#f9fafb;border-radius:8px;border-left:4px solid #667eea;'>" +
            "        <div style='font-weight:bold;color:#667eea;font-size:12px;text-transform:uppercase;margin-bottom:5px;letter-spacing:0.5px;'>👤 Full Name</div>" +
            "        <div style='color:#1f2937;font-size:16px;'>%s</div>" +
            "      </div>" +
            "      " +
            "      <div style='margin-bottom:20px;padding:15px;background:#f9fafb;border-radius:8px;border-left:4px solid #667eea;'>" +
            "        <div style='font-weight:bold;color:#667eea;font-size:12px;text-transform:uppercase;margin-bottom:5px;letter-spacing:0.5px;'>📧 Email Address</div>" +
            "        <div style='color:#1f2937;font-size:16px;'><a href='mailto:%s' style='color:#667eea;text-decoration:none;'>%s</a></div>" +
            "      </div>" +
            "      " +
            "      <div style='margin-bottom:20px;padding:15px;background:#f9fafb;border-radius:8px;border-left:4px solid #667eea;'>" +
            "        <div style='font-weight:bold;color:#667eea;font-size:12px;text-transform:uppercase;margin-bottom:5px;letter-spacing:0.5px;'>📱 Phone Number</div>" +
            "        <div style='color:#1f2937;font-size:16px;'>%s</div>" +
            "      </div>" +
            "      " +
            "      <div style='margin-bottom:20px;padding:15px;background:#f9fafb;border-radius:8px;border-left:4px solid #667eea;'>" +
            "        <div style='font-weight:bold;color:#667eea;font-size:12px;text-transform:uppercase;margin-bottom:5px;letter-spacing:0.5px;'>📋 Subject</div>" +
            "        <div style='color:#1f2937;font-size:16px;'>%s</div>" +
            "      </div>" +
            "      " +
            "      <!-- Message -->" +
            "      <div style='background:#f9fafb;padding:20px;border-radius:8px;border:1px solid #e5e7eb;margin-top:20px;'>" +
            "        <div style='font-weight:bold;color:#667eea;font-size:12px;text-transform:uppercase;margin-bottom:10px;letter-spacing:0.5px;'>💬 Message</div>" +
            "        <div style='color:#1f2937;font-size:16px;line-height:1.8;white-space:pre-wrap;'>%s</div>" +
            "      </div>" +
            "      " +
            "      <!-- Timestamp -->" +
            "      <div style='color:#6b7280;font-size:12px;text-align:right;margin-top:20px;padding-top:15px;border-top:1px solid #e5e7eb;'>" +
            "        ⏰ Submitted on: %s" +
            "      </div>" +
            "      " +
            "      <!-- Quick Reply Button -->" +
            "      <div style='text-align:center;margin:30px 0 10px 0;'>" +
            "        <a href='mailto:%s?subject=Re: %s' style='display:inline-block;background:#3b82f6;color:white;padding:12px 30px;text-decoration:none;border-radius:8px;font-weight:bold;'>Reply to Customer →</a>" +
            "      </div>" +
            "    </div>" +
            "    " +
            "    <!-- Footer -->" +
            "    <div style='background:#1f2937;color:white;padding:20px;text-align:center;font-size:12px;'>" +
            "      <p style='margin:0;'><strong>E-Commerce Store</strong></p>" +
            "      <p style='margin:5px 0;color:#9ca3af;'>This email was sent from your contact form.</p>" +
            "      <p style='margin:5px 0;color:#9ca3af;'>Reply directly to <a href='mailto:%s' style='color:#667eea;text-decoration:none;'>%s</a> to respond to the customer.</p>" +
            "      <p style='margin:15px 0 0 0;padding-top:15px;border-top:1px solid #374151;color:#6b7280;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            contactForm.getName(),
            contactForm.getEmail(),
            contactForm.getEmail(),
            contactForm.getPhone() != null && !contactForm.getPhone().isEmpty() 
                ? contactForm.getPhone() : "Not provided",
            subjectLabel,
            contactForm.getMessage().replace("\n", "<br>"),
            timestamp,
            contactForm.getEmail(),
            contactForm.getSubject(),
            contactForm.getEmail(),
            contactForm.getEmail()
        );
    }

    private String getContactSubjectLabel(String subject) {
        return switch (subject.toLowerCase()) {
            case "order" -> "📦 Order Inquiry";
            case "product" -> "🛍️ Product Question";
            case "shipping" -> "🚚 Shipping & Delivery";
            case "return" -> "🔄 Returns & Refunds";
            case "technical" -> "🔧 Technical Support";
            default -> "📨 " + subject;
        };
    }
    
    @Async
    public void sendShippingLabelEmail(Order order, byte[] labelPdf) {
        try {
            System.out.println("========================================");
            System.out.println("📧 PREPARING TO SEND SHIPPING LABEL EMAIL");
            System.out.println("📧 To: " + order.getUser().getEmail());
            System.out.println("📎 Label PDF size: " + labelPdf.length + " bytes");
            System.out.println("========================================");
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("Return Shipping Label - Order #" + order.getId());
            
            // Email body
            String emailContent = buildShippingLabelEmail(order);
            helper.setText(emailContent, true);
            
            // Attach the PROVIDED PDF (not generating a new one)
            String filename = "Return_Shipping_Label_Order_" + order.getId() + ".pdf";
            helper.addAttachment(filename, new ByteArrayResource(labelPdf));
            
            System.out.println("📎 Attachment added: " + filename);
            System.out.println("📤 Sending email...");
            
            mailSender.send(message);
            
            System.out.println("✅ Shipping label email sent successfully to: " + order.getUser().getEmail());
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ FAILED TO SEND SHIPPING LABEL EMAIL");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
        }
    }

    private String buildShippingLabelEmail(Order order) {
        String currentDate = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
        
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'>" +
            "  <div style='max-width:600px;margin:20px auto;background:white;'>" +
            "    <div style='background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);color:white;padding:40px 20px;text-align:center;'>" +
            "      <h1 style='margin:0;font-size:32px;'>📦 Return Shipping Label</h1>" +
            "      <p style='margin:10px 0 0 0;font-size:16px;'>Your return label is ready!</p>" +
            "    </div>" +
            "    " +
            "    <div style='padding:30px;'>" +
            "      <h2 style='color:#2c3e50;margin-top:0;'>Hello %s,</h2>" +
            "      <p style='color:#555;font-size:16px;line-height:1.6;'>Your return shipping label for Order #%d is attached to this email.</p>" +
            "      " +
            "      <div style='background:#dbeafe;padding:20px;border-radius:8px;margin:25px 0;border-left:4px solid #3b82f6;'>" +
            "        <h3 style='margin:0 0 15px 0;color:#1e40af;'>📋 How to Use Your Return Label</h3>" +
            "        <div style='color:#1e40af;line-height:1.8;'>" +
            "          <p style='margin:8px 0;'><strong>1.</strong> Print the attached shipping label</p>" +
            "          <p style='margin:8px 0;'><strong>2.</strong> Pack your items securely in the original packaging</p>" +
            "          <p style='margin:8px 0;'><strong>3.</strong> Attach the label to the outside of the package</p>" +
            "          <p style='margin:8px 0;'><strong>4.</strong> Drop off at any USPS location or schedule a pickup</p>" +
            "          <p style='margin:8px 0;'><strong>5.</strong> Keep your receipt until refund is processed</p>" +
            "        </div>" +
            "      </div>" +
            "      " +
            "      <div style='background:#fef3c7;border-left:4px solid #f59e0b;padding:15px;border-radius:4px;'>" +
            "        <p style='margin:0;color:#92400e;'><strong>⚠️ Important:</strong></p>" +
            "        <p style='margin:5px 0 0 0;color:#92400e;font-size:14px;'>Please ship your return within 7 days. Refunds will be processed within 5-7 business days after we receive and inspect the item.</p>" +
            "      </div>" +
            "      " +
            "      <div style='text-align:center;margin:30px 0;'>" +
            "        <a href='http://localhost:8080/web/orders' style='display:inline-block;background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-weight:bold;'>View Order Status</a>" +
            "      </div>" +
            "      " +
            "      <p style='color:#7f8c8d;font-size:14px;margin-top:30px;'>Questions? Contact us at support@ecommerce.com</p>" +
            "    </div>" +
            "    " +
            "    <div style='background:#2c3e50;color:white;padding:20px;text-align:center;'>" +
            "      <p style='margin:0;font-size:16px;font-weight:bold;'>Thank you - E-Commerce Store</p>" +
            "      <p style='margin:10px 0 0 0;color:#95a5a6;font-size:14px;'>© 2026 E-Commerce Store. All rights reserved.</p>" +
            "    </div>" +
            "  </div>" +
            "</body>" +
            "</html>",
            order.getUser().getName(),
            order.getId()
        );
    }
    

}