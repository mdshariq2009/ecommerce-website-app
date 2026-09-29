package com.ecommerce.repository;

import com.ecommerce.model.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, UUID> {
    
    List<LiveChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    
    List<LiveChatMessage> findByConversationIdAndIsAdminFalse(UUID conversationId);
}
