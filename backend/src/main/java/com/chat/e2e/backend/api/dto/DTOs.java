package com.chat.e2e.backend.api.dto;

import com.chat.e2e.backend.keys.KeyCurve;

import java.time.Instant;
import java.util.*;

public class DTOs {
    public record DeviceSummary(UUID deviceId, String deviceName, String platform, Instant createdAt, Instant lastSeenAt, boolean revoked) {}

    public record EnrollmentStartRequest(String userHandle) {}
    public record EnrollmentStartResponse(UUID deviceId, String nonce, Instant expiresAt) {}

    public record EnrollmentFinishRequest(String ikPub, KeyCurve keyCurve, String pqkemPub, String proof) {}
    public record EnrollmentFinishResponse(DeviceCertificate deviceCertificate, Instant certExpiresAt) {}
    public record DeviceCertificate(String payload, String signature, String kid) {}

    public record SpkUploadRequest(int keyId, String publicKey, String signature, String validUntil) {}
    public record OpkItem(int keyId, String publicKey) {}
    public record OpkUploadRequest(List<OpkItem> opks) {}
    public record OpkStatusResponse(long available) {}

    public record PrekeyBundleResponse(
            String ikPub, KeyCurve keyCurve,
            Spk spk,
            Opk opk,
            String pqkemPub,
            PqPrekey pqkemPrekey,
            DeviceCertificate deviceCertificate
    ) {
        public record Spk(int keyId, String publicKey, String signature) {}
        public record Opk(Integer keyId, String publicKey) {}
        public record PqPrekey(String scheme, Integer keyId, String publicKey) {}
    }

    public record RegisterUserRequest(String handle, String displayName, String password) {}
}
