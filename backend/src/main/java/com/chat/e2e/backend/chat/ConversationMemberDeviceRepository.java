package com.chat.e2e.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;
import java.util.UUID;

public interface ConversationMemberDeviceRepository extends JpaRepository<ConversationMemberDevice, UUID> {
    @Query("select cmd.deviceId from ConversationMemberDevice cmd where cmd.conversationId = :conversationId")
    Set<UUID> findAllDeviceIdsByConversationId(UUID conversationId);

    boolean existsByConversationIdAndDeviceId(UUID conversationId, UUID deviceId);
}
