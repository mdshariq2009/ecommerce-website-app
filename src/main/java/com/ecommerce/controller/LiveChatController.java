package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.model.User;
import com.ecommerce.service.LiveChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/live-chat")
@CrossOrigin(origins = "*")
public class LiveChatController {
    
    @Autowired
    private LiveChatService liveChatService;
    
    /**
     * Auto-start chat for logged-in user
     */
    @PostMapping("/auto-start")
    public ResponseEntity<?> autoStartChat() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal().toString())) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not logged in"));
            }
            
            User user = (User) authentication.getPrincipal();
            
            LiveChatStartRequest request = new LiveChatStartRequest();
            request.setUserId(user.getId());
            request.setUserEmail(user.getEmail());
            request.setUserName(user.getName());
            
            LiveChatConversationResponse response = liveChatService.startChat(request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Get or create chat for logged-in user
     */
    @GetMapping("/conversation")
    public ResponseEntity<?> getConversation() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal().toString())) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not logged in"));
            }
            
            User user = (User) authentication.getPrincipal();
            
            LiveChatConversationResponse response = liveChatService.getConversation(user.getEmail());
            
            if (response == null) {
                LiveChatStartRequest request = new LiveChatStartRequest();
                request.setUserId(user.getId());
                request.setUserEmail(user.getEmail());
                request.setUserName(user.getName());
                
                response = liveChatService.startChat(request);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Send message in chat
     */
    @PostMapping("/send-message")
    public ResponseEntity<?> sendMessage(@RequestBody LiveChatMessageRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not logged in"));
            }
            
            User user = (User) authentication.getPrincipal();
            
            request.setSenderEmail(user.getEmail());
            request.setSenderName(user.getName());
            request.setIsAdmin(false);
            
            LiveChatMessageResponse response = liveChatService.sendMessage(request);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Get all messages in conversation
     */
    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<?> getMessages(@PathVariable UUID conversationId) {
        try {
            List<LiveChatMessageResponse> messages = liveChatService.getMessages(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Close chat
     */
    @PostMapping("/close/{conversationId}")
    public ResponseEntity<?> closeChat(@PathVariable UUID conversationId) {
        try {
            liveChatService.closeChat(conversationId);
            return ResponseEntity.ok(new SuccessResponse("Chat closed successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * End chat - creates new conversation on next chat
     */
    @PutMapping("/end/{conversationId}")
    public ResponseEntity<?> endChat(@PathVariable UUID conversationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(new ErrorResponse("User not logged in"));
            }
            
            User user = (User) authentication.getPrincipal();
            
            liveChatService.endChat(conversationId, user.getEmail());
            return ResponseEntity.ok(new SuccessResponse("Chat ended successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Get all active conversations
     */
    @GetMapping("/admin/conversations")
    public ResponseEntity<?> getAllConversations() {
        try {
            List<LiveChatConversationResponse> conversations = liveChatService.getAllActiveConversations();
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
}

class ErrorResponse {
    private String error;
    
    public ErrorResponse(String error) {
        this.error = error;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}

class SuccessResponse {
    private String message;
    
    public SuccessResponse(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
