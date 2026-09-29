package com.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleChatService {
    
    @Value("${google.chat.webhook.url:}")
    private String webhookUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public GoogleChatService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Send message to Google Chat space using webhook
     */
    public void notifyAdminNewMessage(String customerName, String customerEmail, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.out.println("Google Chat webhook URL not configured");
            return;
        }
        
        try {
            String messageText = formatMessage(customerName, customerEmail, message);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", messageText);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(
                objectMapper.writeValueAsString(payload),
                headers
            );
            
            restTemplate.postForObject(webhookUrl, request, String.class);
            System.out.println("Google Chat notification sent successfully");
        } catch (Exception e) {
            System.err.println("Failed to send Google Chat notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Format message for Google Chat
     */
    private String formatMessage(String customerName, String customerEmail, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format(
            "🔔 *NEW CUSTOMER MESSAGE*\n\n" +
            "👤 *From:* %s\n" +
            "📧 *Email:* %s\n" +
            "💬 *Message:* %s\n" +
            "⏰ *Time:* %s",
            customerName,
            customerEmail,
            message,
            timestamp
        );
    }
}
