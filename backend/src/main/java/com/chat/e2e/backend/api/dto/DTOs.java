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
}
