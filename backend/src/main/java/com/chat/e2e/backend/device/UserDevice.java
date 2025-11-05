package com.chat.e2e.backend.device;

import com.chat.e2e.backend.keys.KeyCurve;
import com.chat.e2e.backend.user.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_device")
@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class UserDevice {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "platform")
    private String platform;

    @Column(name = "public_identity_key", nullable = false, length = 4096)
    private String publicIdentityKey;

    @Enumerated(EnumType.STRING)
    @Column(name="key_curve") private KeyCurve keyCurve;

    @Column(name = "pqkem_public_key", columnDefinition = "bytea")
    private byte[] pqkemPublicKey;

    @Column(name = "public_identity_key_sig", columnDefinition = "bytea")
    private byte[] certSignature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cert_payload", columnDefinition = "jsonb")
    private String certPayload;

    @Column(name = "cert_issued_at")
    private Instant certIssuedAt;

    @Column(name = "cert_expires_at")
    private Instant certExpiresAt;

    @Column(name = "cert_alg")
    private String certAlg;

    @Column(name = "cert_serial")
    private String certSerial;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isActive() { return revokedAt == null; }
}
