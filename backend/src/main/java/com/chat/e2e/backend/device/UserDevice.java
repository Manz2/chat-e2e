package com.chat.e2e.backend.device;

import com.chat.e2e.backend.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "public_kx_key", nullable = false, length = 4096)
    private String publicKxKey;

    @Column(name = "identity_binding_sig", columnDefinition = "bytea")
    private byte[] identityBindingSig;

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
