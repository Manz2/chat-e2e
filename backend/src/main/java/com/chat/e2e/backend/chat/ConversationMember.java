package com.chat.e2e.backend.chat;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "conversation_member")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ConversationMember.ConversationMemberId.class)
public class ConversationMember {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role")
    private String role;

    // composite PK helper
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ConversationMemberId implements Serializable {
        private UUID conversationId;
        private UUID userId;
    }
}
