package com.ecommerce.service;

import com.ecommerce.dto.*;
import java.util.List;
import java.util.UUID;

public interface LiveChatService {
    LiveChatConversationResponse startChat(LiveChatStartRequest request);
    LiveChatMessageResponse sendMessage(LiveChatMessageRequest request);
    List<LiveChatMessageResponse> getMessages(UUID conversationId);
    LiveChatConversationResponse getConversation(String email);
    void closeChat(UUID conversationId);
    void endChat(UUID conversationId, String userEmail);
    List<LiveChatConversationResponse> getAllActiveConversations();
}
