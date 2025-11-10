package com.chat.e2e.backend.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_delivery")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class MessageDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "recipient_device_id", nullable = false)
    private UUID recipientDeviceId;

    @JdbcTypeCode(SqlTypes.VARBINARY) // oder SqlTypes.BINARY
    @Column(name = "ciphertext", nullable = false, columnDefinition = "bytea")
    private byte[] ciphertext;

    @Column(name = "msg_header", columnDefinition = "jsonb")
    private String msgHeader;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;
}
