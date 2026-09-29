package com.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class LiveChatMessageResponse {
    private UUID id;
    private UUID conversationId;
    private String senderEmail;
    private String senderName;
    private String message;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
    
    public LiveChatMessageResponse() {}
    
    public LiveChatMessageResponse(UUID id, UUID conversationId, String senderEmail, String senderName, String message, Boolean isAdmin, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.message = message;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
