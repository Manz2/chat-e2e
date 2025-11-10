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
}
