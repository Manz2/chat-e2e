package com.chat.e2e.backend.device;

import com.chat.e2e.backend.api.dto.*;
import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class DeviceEnrollmentService {
    private final AppUserRepository userRepo;
    private final UserDeviceRepository deviceRepo;

    // Simple in-memory nonce store demo; in prod: DB or cache
    private final java.util.Map<UUID, String> nonces = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<UUID, Instant> nonceExpiry = new java.util.concurrent.ConcurrentHashMap<>();

    public DTOs.EnrollmentStartResponse start(DTOs.EnrollmentStartRequest req) {
        AppUser user = userRepo.findByHandle(req.userHandle())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        UserDevice d = deviceRepo.save(UserDevice.builder().user(user).platform("unknown").build());
        String nonce = Base64.getEncoder().encodeToString(java.util.UUID.randomUUID().toString().getBytes());
        nonces.put(d.getId(), nonce);
        nonceExpiry.put(d.getId(), Instant.now().plusSeconds(300));
        return new DTOs.EnrollmentStartResponse(d.getId(), nonce, nonceExpiry.get(d.getId()));
    }

    @Transactional
    public DTOs.EnrollmentFinishResponse finish(UUID deviceId, DTOs.EnrollmentFinishRequest req, PublicKey serverSigPublic, PrivateKey serverSigPrivate) {
        UserDevice dev = deviceRepo.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("device not found"));
        String nonce = Optional.ofNullable(nonces.get(deviceId)).orElseThrow(() -> new IllegalArgumentException("nonce not found"));
        if (Instant.now().isAfter(nonceExpiry.get(deviceId))) throw new IllegalArgumentException("nonce expired");

        // TODO: Verify proof-of-possession using ikPub (client signed SHA256(nonce||ikPub||deviceId))
        // (hier nur Platzhalter)
        if (req.proof() == null || req.proof().isBlank()) throw new IllegalArgumentException("invalid proof");

        // Build cert payload (JSON string minimal)
        String payload = """
          {"ver":1,"device_id":"%s","ik_pub":"%s","key_curve":"%s","issued_at":"%s"}
          """.formatted(deviceId, req.ikPub(), req.keyCurve(), Instant.now().toString());

        // Sign with server's Ed25519 (or what you configured)
        try {
            var sig = Signature.getInstance("Ed25519");
            sig.initSign(serverSigPrivate);
            sig.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(sig.sign());

            dev.setPublicIdentityKey(req.ikPub());
            dev.setKeyCurve(req.keyCurve());
            dev.setPqkemPublicKey(req.pqkemPub() == null ? null : Base64.getDecoder().decode(req.pqkemPub()));
            dev.setCertPayload(payload);
            dev.setCertSignature(Base64.getDecoder().decode(signature));
            dev.setCertIssuedAt(Instant.now());
            dev.setCertExpiresAt(Instant.now().plusSeconds(31536000)); // 1y
            dev.setCertAlg("Ed25519");
            deviceRepo.save(dev);

            nonces.remove(deviceId); nonceExpiry.remove(deviceId);
            return new DTOs.EnrollmentFinishResponse(new DTOs.DeviceCertificate(payload, signature, "ed25519:root-2025"), dev.getCertExpiresAt());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void revoke(String handle, UUID deviceId) {
        UserDevice dev = deviceRepo.findByIdAndUser_Handle(deviceId, handle)
                .orElseThrow(() -> new IllegalArgumentException("device not found"));
        dev.setRevokedAt(Instant.now());
        deviceRepo.save(dev);
    }
}
