package com.ecommerce.service.impl;

import com.ecommerce.dto.*;
import com.ecommerce.model.LiveChatConversation;
import com.ecommerce.model.LiveChatMessage;
import com.ecommerce.repository.LiveChatConversationRepository;
import com.ecommerce.repository.LiveChatMessageRepository;
import com.ecommerce.service.LiveChatService;
import com.ecommerce.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LiveChatServiceImpl implements LiveChatService {
    
    @Autowired
    private LiveChatConversationRepository conversationRepository;
    
    @Autowired
    private LiveChatMessageRepository messageRepository;
    
    @Autowired
    private EmailService emailService;
    
    private static final String ADMIN_EMAIL = "mdshariq2009@gmail.com";
    
    @Override
    public LiveChatConversationResponse startChat(LiveChatStartRequest request) {
        var existingConversation = conversationRepository
            .findByUserEmailAndStatus(request.getUserEmail(), "active");
        
        LiveChatConversation conversation;
        
        if (existingConversation.isPresent()) {
            conversation = existingConversation.get();
        } else {
            conversation = new LiveChatConversation();
            conversation.setUserId(convertToUUID(request.getUserId()));
            conversation.setUserEmail(request.getUserEmail());
            conversation.setUserName(request.getUserName());
            conversation.setAdminId(ADMIN_EMAIL);
            conversation.setStatus("active");
            
            conversation = conversationRepository.save(conversation);
            
            // Send email notification to admin
            sendAdminNotificationEmail(request.getUserName(), request.getUserEmail());
        }
        
        return mapToResponse(conversation);
    }
    
    @Override
    public LiveChatMessageResponse sendMessage(LiveChatMessageRequest request) {
        LiveChatMessage message = new LiveChatMessage();
        message.setConversationId(request.getConversationId());
        message.setSenderEmail(request.getSenderEmail());
        message.setSenderName(request.getSenderName());
        message.setMessage(request.getMessage());
        message.setIsAdmin(request.getIsAdmin() != null && request.getIsAdmin());
        
        message = messageRepository.save(message);
        
        var conversation = conversationRepository.findById(request.getConversationId())
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.setLastMessage(request.getMessage());
        conversation.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        // Send email notification
        if (request.getIsAdmin() != null && request.getIsAdmin()) {
            emailService.sendCustomerLiveChatReply(request.getSenderEmail(), request.getSenderName(), request.getMessage());
        } else {
            emailService.sendAdminLiveChatNotification(request.getSenderName(), request.getSenderEmail());
        }
        
        return mapMessageToResponse(message);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LiveChatMessageResponse> getMessages(UUID conversationId) {
        var messages = messageRepository.findByConversationIdAndIsAdminFalse(conversationId);
        messages.forEach(msg -> msg.setReadStatus(true));
        messageRepository.saveAll(messages);
        
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
            .stream()
            .map(this::mapMessageToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public LiveChatConversationResponse getConversation(String email) {
        return conversationRepository.findByUserEmailAndStatus(email, "active")
            .map(this::mapToResponse)
            .orElse(null);
    }
    
    @Override
    public void closeChat(UUID conversationId) {
        var conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.setStatus("closed");
        conversationRepository.save(conversation);
    }

    @Override
    public void endChat(UUID conversationId, String userEmail) {
        var conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        // Verify user owns this conversation
        if (!conversation.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized: You can only end your own conversations");
        }
        
        conversation.setStatus("closed");
        conversationRepository.save(conversation);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LiveChatConversationResponse> getAllActiveConversations() {
        return conversationRepository.findByStatus("active")
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private void sendAdminNotificationEmail(String userName, String userEmail) {
        emailService.sendAdminLiveChatNotification(userName, userEmail);
    }
    
    private UUID convertToUUID(Long userId) {
        if (userId == null || userId == 0) {
            return UUID.randomUUID();
        }
        return UUID.nameUUIDFromBytes(userId.toString().getBytes());
    }
    
    private LiveChatConversationResponse mapToResponse(LiveChatConversation conversation) {
        LiveChatConversationResponse response = new LiveChatConversationResponse();
        response.setId(conversation.getId());
        response.setUserEmail(conversation.getUserEmail());
        response.setUserName(conversation.getUserName());
        response.setStatus(conversation.getStatus());
        response.setLastMessage(conversation.getLastMessage());
        response.setLastMessageTime(conversation.getLastMessageTime());
        response.setCreatedAt(conversation.getCreatedAt());
        return response;
    }
    
    private LiveChatMessageResponse mapMessageToResponse(LiveChatMessage message) {
        LiveChatMessageResponse response = new LiveChatMessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setSenderEmail(message.getSenderEmail());
        response.setSenderName(message.getSenderName());
        response.setMessage(message.getMessage());
        response.setIsAdmin(message.getIsAdmin());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
