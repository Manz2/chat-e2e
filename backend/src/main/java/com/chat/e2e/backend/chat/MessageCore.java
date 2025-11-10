package com.chat.e2e.backend.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_core")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MessageCore {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    // JSONB – je nach Dialekt (Postgres) als Text persistiert
    @Column(name = "header", columnDefinition = "jsonb")
    private String header;
}
