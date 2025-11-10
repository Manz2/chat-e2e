package com.chat.e2e.backend.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "is_group", nullable = false)
    private boolean isGroup;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
