package com.ecommerce.dto;

import java.util.UUID;

public class LiveChatMessageRequest {
    private UUID conversationId;
    private String message;
    private String senderEmail;
    private String senderName;
    private Boolean isAdmin;
    
    public LiveChatMessageRequest() {}
    
    public LiveChatMessageRequest(UUID conversationId, String message, String senderEmail, String senderName, Boolean isAdmin) {
        this.conversationId = conversationId;
        this.message = message;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.isAdmin = isAdmin;
    }
    
    public UUID getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
