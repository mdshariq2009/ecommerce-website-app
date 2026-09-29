package com.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class LiveChatConversationResponse {
    private UUID id;
    private String userEmail;
    private String userName;
    private String status;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
    
    public LiveChatConversationResponse() {}
    
    public LiveChatConversationResponse(UUID id, String userEmail, String userName, String status, String lastMessage, LocalDateTime lastMessageTime, LocalDateTime createdAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.userName = userName;
        this.status = status;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.createdAt = createdAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
