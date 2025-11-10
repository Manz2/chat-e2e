package com.chat.e2e.backend.chat;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "conversation_member_device")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ConversationMemberDevice.ConversationMemberDeviceId.class)
public class ConversationMemberDevice {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ConversationMemberDeviceId implements Serializable {
        private UUID conversationId;
        private UUID deviceId;
    }
}
