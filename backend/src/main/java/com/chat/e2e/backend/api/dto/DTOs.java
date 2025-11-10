package com.chat.e2e.backend.api.dto;

import java.time.Instant;
import java.util.*;

public class DTOs {
    public record DeviceSummary(UUID deviceId, String deviceName, String platform, Instant createdAt, Instant lastSeenAt, boolean revoked) {}

    public record EnrollmentStartRequest(String userHandle) {}
    public record EnrollmentStartResponse(UUID deviceId, String nonce, Instant expiresAt) {}

    public record EnrollmentFinishRequest(
            String ikPub,        // Base64 (X.509 SPKI)
            String kxPub,        // Base64 (X.509 SPKI)
            String bindingSig,   // Base64 (Ed25519 Sign(kxPub-bound))
            String proof,        // Base64 (Ed25519 Sign(enroll-msg))
            String platform,     // optional: 'ios','android','web','desktop'
            String deviceName    // optional
    ) {}

    public record RegisterUserRequest(String handle, String displayName, String password) {}

    public record CreateConversationRequest(boolean isGroup, Set<String> memberHandles) {}
    public record CreateConversationResponse(UUID conversationId, Instant createdAt) {}

    public record AddMemberDevicesRequest(String handle, Set<UUID> deviceIds) {} // Geräte müssen dem User gehören

    public record DistributeCKRequest(
            int epoch,
            Map<UUID,String> sealedForDevice, // deviceId -> base64(ciphertext)
            String sigFromDevice,             // Ed25519 signierte Control-Header (Base64), optional serverseitig ungeprüft
            UUID fromDeviceId
    ) {}

    public record SendMessageRequest(
            String contentType,    // z.B. "text/plain"
            int epoch,
            long counter,
            String ciphertextB64   // AEAD-Ciphertext (gleich für alle Zielgeräte)
    ) {}

    public record SendMessageResponse(UUID messageId, Instant createdAt, int deliveries) {}

    public record BootstrapResponse(
            UUID userId,
            String handle,
            List<ConversationBrief> conversations,
            List<UserDeviceBrief> devices
    ) {}

    public record ConversationBrief(UUID conversationId, boolean isGroup, Instant createdAt,
                                    List<MemberBrief> members) {}
    public record MemberBrief(UUID userId, String handle) {}
    public record UserDeviceBrief(UUID deviceId, String platform, Instant revokedAt, Instant lastSeenAt) {}

    // --- neu: Inbox Pull ---
    public record InboxResponse(List<DeliveryDTO> items, String nextCursor) {}

    public record DeliveryDTO(
            UUID deliveryId,
            UUID messageId,
            UUID conversationId,
            String contentType,
            String msgHeaderJson,   // z.B. {"epoch":7,"counter":1001}
            String ciphertextB64,   // base64(bytea)
            Instant createdAt
    ) {}

    // --- neu: Acks ---
    public record AckRequest(UUID deviceId, List<UUID> deliveryIds) {}

    // --- neu: Read Receipts ---
    public record ReadRequest(UUID deviceId, UUID messageId) {}

    public record ReadWsMessage(UUID conversationId, UUID messageId) {}
    public record DeliveredEvent(UUID messageId, UUID conversationId, UUID recipientDeviceId, Instant createdAt) {}

    public record JwtClaims(UUID userId, UUID deviceId) {}

    // Client -> Server
    public record SendWsMessage(
            UUID conversationId,
            String contentType,
            int epoch,
            long counter,
            String ciphertextB64
    ) {}

    // Server -> Client (nur an Sender als Bestätigung)
    public record SendAckEvent(
            UUID messageId,
            UUID conversationId,
            Instant createdAt,
            int deliveries
    ) {}
    // Read-Event (Server -> andere)
    public record ReadEvent(UUID conversationId, UUID messageId, UUID byDeviceId, Instant at) {}

}
