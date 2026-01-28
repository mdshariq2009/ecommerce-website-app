package com.ecommerce.service;

import com.ecommerce.dto.ContactFormDTO;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.util.CarrierDetector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${SPRING_MAIL_PASSWORD}") // Your Resend API Key
    private String resendApiKey;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.name}")
    private String fromName;

    // --- CORE API SENDER (Replaces JavaMailSender) ---

    private void sendViaApi(String to, String subject, String htmlContent, String replyTo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", String.format("%s <%s>", fromName, fromEmail));
            body.put("to", to);
            body.put("subject", subject);
            body.put("html", htmlContent);
            if (replyTo != null) {
                body.put("reply_to", replyTo);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(RESEND_API_URL, entity, String.class);
            System.out.println("✅ Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Resend API Error: " + e.getMessage());
        }
    }

    // --- PUBLIC ASYNC METHODS ---

    @Async
    public void sendOrderConfirmationEmail(Order order) {
        String emailContent = buildOrderConfirmationEmail(order);
        sendViaApi(order.getUser().getEmail(), "Order Confirmation - Order #" + order.getId(), emailContent, null);
    }

    @Async
    public void sendAdminOrderNotification(Order order) {
        String emailContent = buildAdminOrderNotificationEmail(order);
        sendViaApi("mdshariq2009@gmail.com", "New Order Received - Order #" + order.getId(), emailContent, order.getUser().getEmail());
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        String emailContent = buildWelcomeEmailHtml(userName);
        sendViaApi(toEmail, "Welcome to E-Commerce Store! 🎉", emailContent, null);
    }

    @Async
    public void sendOrderStatusUpdateEmail(Order order) {
        String emailContent = buildOrderStatusUpdateHtml(order);
        sendViaApi(order.getUser().getEmail(), "Order Status Update - Order #" + order.getId(), emailContent, null);
    }

    @Async
    public void sendContactFormEmail(ContactFormDTO contactForm) {
        String emailContent = buildContactFormEmail(contactForm);
        sendViaApi("mdshariq2009@gmail.com", "New Contact Form Submission - " + contactForm.getSubject(), emailContent, contactForm.getEmail());
    }

    // --- HTML BUILDERS (Your original detailed styling) ---

    private String buildOrderConfirmationEmail(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        double subtotal = 0;
        for (OrderItem item : order.getItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            subtotal += itemTotal;
            itemsHtml.append(String.format(
                "<tr><td style='padding:12px;border-bottom:1px solid #ecf0f1;'>%s</td><td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:center;'>%d</td><td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;'>$%.2f</td><td style='padding:12px;border-bottom:1px solid #ecf0f1;text-align:right;font-weight:bold;'>$%.2f</td></tr>",
                item.getProductName(), item.getQuantity(), item.getPrice(), itemTotal
            ));
        }

        double orderSubtotal = order.getSubtotal() != null ? order.getSubtotal() : subtotal;
        double orderTax = order.getTax() != null ? order.getTax() : 0.0;
        double orderShipping = order.getShipping() != null ? order.getShipping() : 0.0;
        double orderTotal = order.getTotalAmount();

        itemsHtml.append(String.format("<tr style='background:#f8f9fa;'><td colspan='3' style='padding:12px;text-align:right;'>Subtotal:</td><td style='padding:12px;text-align:right;font-weight:bold;'>$%.2f</td></tr>", orderSubtotal));
        itemsHtml.append(String.format("<tr style='background:#f8f9fa;'><td colspan='3' style='padding:12px;text-align:right;'>Shipping:</td><td style='padding:12px;text-align:right;font-weight:bold;color:%s;'>%s</td></tr>", orderShipping == 0 ? "#27ae60" : "#555", orderShipping == 0 ? "FREE" : String.format("$%.2f", orderShipping)));
        itemsHtml.append(String.format("<tr style='background:#f8f9fa;'><td colspan='3' style='padding:12px;text-align:right;'>Tax:</td><td style='padding:12px;text-align:right;font-weight:bold;'>$%.2f</td></tr>", orderTax));
        itemsHtml.append(String.format("<tr style='background:#27ae60;color:white;'><td colspan='3' style='padding:15px;text-align:right;font-size:18px;'><strong>Total Amount:</strong></td><td style='padding:15px;text-align:right;font-size:18px;'><strong>$%.2f</strong></td></tr>", orderTotal));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        String orderDate = order.getCreatedAt().format(formatter);

        return String.format(
            "<!DOCTYPE html><html><body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f5f5f5;'><div style='max-width:600px;margin:20px auto;background:white;'><div style='background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);color:white;padding:40px 20px;text-align:center;'><h1 style='margin:0;font-size:32px;'>🛒 Order Confirmed!</h1><p style='margin:10px 0 0 0;font-size:16px;'>Thank you for your purchase</p></div><div style='padding:30px;'><h2 style='color:#2c3e50;margin-top:0;'>Hello %s,</h2><p style='color:#555;font-size:16px;line-height:1.6;'>Your order has been successfully placed. Order ID: #%d, Date: %s</p><table style='width:100%%;border-collapse:collapse;'><thead><tr style='background:#34495e;color:white;'><th style='padding:12px;text-align:left;'>Product</th><th style='padding:12px;text-align:center;'>Qty</th><th style='padding:12px;text-align:right;'>Price</th><th style='padding:12px;text-align:right;'>Total</th></tr></thead><tbody>%s</tbody></table></div></div></body></html>",
            order.getUser().getName(), order.getId(), orderDate, itemsHtml.toString()
        );
    }

    private String buildAdminOrderNotificationEmail(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
        return "<html><body><h1>New Order Alert!</h1><p>Order #" + order.getId() + " was placed on " + order.getCreatedAt().format(formatter) + ".</p></body></html>";
    }

    private String buildWelcomeEmailHtml(String userName) {
        return String.format("<html><body><h1>Welcome, %s!</h1><p>Thanks for joining our store.</p></body></html>", userName);
    }

    private String buildOrderStatusUpdateHtml(Order order) {
        return String.format("<html><body><h1>Order Update</h1><p>Order #%d status: %s</p></body></html>", order.getId(), order.getOrderStatus());
    }

    private String buildContactFormEmail(ContactFormDTO contactForm) {
        return String.format("<html><body><h2>Contact Request</h2><p><strong>From:</strong> %s</p><p><strong>Message:</strong> %s</p></body></html>", contactForm.getName(), contactForm.getMessage());
    }

    private String getContactSubjectLabel(String subject) {
        if (subject == null) return "General Inquiry";
        return subject;
    }
}