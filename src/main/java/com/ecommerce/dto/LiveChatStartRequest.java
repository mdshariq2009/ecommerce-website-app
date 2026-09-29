package com.ecommerce.dto;

public class LiveChatStartRequest {
    private String userEmail;
    private String userName;
    private Long userId;
    
    public LiveChatStartRequest() {}
    
    public LiveChatStartRequest(String userEmail, String userName, Long userId) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.userId = userId;
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
