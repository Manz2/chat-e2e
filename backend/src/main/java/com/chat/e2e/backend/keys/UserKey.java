package com.chat.e2e.backend.keys;

import com.chat.e2e.backend.device.UserDevice;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_key",
        indexes = { @Index(name="idx_user_key_device_type", columnList="device_id,type") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserKey {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="device_id", nullable=false)
    private UserDevice device;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private UserKeyType type;

    @Column(name="key_id", nullable=false)
    private int keyId;
    @Column(name="public_key", nullable=false, length = 8192)
    private String publicKey;

    @Column(name = "signature", columnDefinition = "bytea")
    private byte[] signature;// for SPK

    @Column(name="is_used", nullable=false)
    private boolean used; // for OPK

    @Column(name="valid_until")
    private Instant validUntil;

    @Column(name="claimed_at")
    private Instant claimedAt;

    @Column(name="kem_scheme")
    private String kemScheme;

    @Column(name="created_at", nullable=false) private Instant createdAt;

    @PrePersist void prePersist(){ if(createdAt==null) createdAt = Instant.now(); }
}

