package com.ecommerce.repository;

import com.ecommerce.model.LiveChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveChatConversationRepository extends JpaRepository<LiveChatConversation, UUID> {
    
    Optional<LiveChatConversation> findByUserEmailAndStatus(String userEmail, String status);
    
    List<LiveChatConversation> findByStatus(String status);
    
    List<LiveChatConversation> findByUserEmail(String userEmail);
    
    List<LiveChatConversation> findAllByOrderByCreatedAtDesc();
}
