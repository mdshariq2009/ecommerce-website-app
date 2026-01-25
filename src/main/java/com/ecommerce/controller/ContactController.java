package com.ecommerce.controller;

import com.ecommerce.dto.ContactFormDTO;
import com.ecommerce.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final EmailService emailService;

    @Autowired
    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitContactForm(@RequestBody ContactFormDTO contactForm) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Received contact form submission from: {} ({})", 
                contactForm.getName(), contactForm.getEmail());
            
            // Validate required fields
            if (contactForm.getName() == null || contactForm.getName().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Name is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (contactForm.getEmail() == null || contactForm.getEmail().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (contactForm.getSubject() == null || contactForm.getSubject().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Subject is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (contactForm.getMessage() == null || contactForm.getMessage().trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Message is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Send email
            emailService.sendContactFormEmail(contactForm);
            
            response.put("status", "success");
            response.put("message", "Your message has been sent successfully! We'll get back to you within 24 hours.");
            
            log.info("Contact form email sent successfully for: {}", contactForm.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing contact form submission", e);
            
            response.put("status", "error");
            response.put("message", "Failed to send message. Please try again later or contact us directly at mdshariq2009@gmail.com");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Contact API is working!");
    }
}