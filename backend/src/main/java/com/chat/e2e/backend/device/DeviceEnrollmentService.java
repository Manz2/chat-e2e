package com.chat.e2e.backend.device;

import com.chat.e2e.backend.api.dto.*;
import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    public void finish(UUID deviceId, DTOs.EnrollmentFinishRequest req) {
        // 0) Basis-Checks
        if (deviceId == null) throw new IllegalArgumentException("deviceId missing");
        if (req == null) throw new IllegalArgumentException("request missing");
        if (isBlank(req.ikPub()) || isBlank(req.kxPub()) || isBlank(req.bindingSig()) || isBlank(req.proof())) {
            throw new IllegalArgumentException("ikPub/kxPub/bindingSig/proof missing");
        }

        // 1) Gerät & Nonce laden/prüfen
        var dev = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("device not found"));

        var nonce = nonces.get(deviceId);
        var exp   = nonceExpiry.get(deviceId);
        if (nonce == null || exp == null) throw new IllegalArgumentException("nonce not found");
        if (Instant.now().isAfter(exp)) throw new IllegalArgumentException("nonce expired");

        // 2) Eingaben dekodieren (SPKI in Base64 erwartet)
        byte[] ikSpki   = b64(req.ikPub());
        byte[] kxSpki   = b64(req.kxPub());
        byte[] bindSig  = b64(req.bindingSig());
        byte[] proofSig = b64(req.proof());

        // Ed25519 PublicKey aus X.509/SPKI
        PublicKey ikPub;
        try {
            ikPub = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(ikSpki));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid ikPub (not Ed25519 SPKI)", e);
        }

        // 3) Binding prüfen: Sign_ed25519( SHA256("bind:" || kxSpki) )
        byte[] bindMsg = sha256(concat("bind:".getBytes(UTF_8), kxSpki));
        if (!verifyEd25519(ikPub, bindMsg, bindSig)) {
            throw new IllegalArgumentException("invalid bindingSig");
        }

        // 4) Proof-of-Possession prüfen:
        // Sign_ed25519( SHA256("enroll:" || deviceId || nonceB64 || ikSpki || kxSpki) )
        byte[] enrollMsg = sha256(concat(
                "enroll:".getBytes(UTF_8),
                deviceId.toString().getBytes(UTF_8),
                nonce.getBytes(UTF_8),   // wir verwenden die B64-Nonce als Bytes
                ikSpki,
                kxSpki
        ));
        if (!verifyEd25519(ikPub, enrollMsg, proofSig)) {
            throw new IllegalArgumentException("invalid proof");
        }

        // 5) Optional: Platform/DeviceName validieren & setzen
        if (!isBlank(req.platform())) {
            var p = req.platform().toLowerCase();
            if (!java.util.Set.of("ios","android","web","desktop").contains(p))
                throw new IllegalArgumentException("invalid platform");
            dev.setPlatform(p);
        }
        if (!isBlank(req.deviceName())) dev.setDeviceName(req.deviceName());

        // 6) Persistieren (öffentliche Schlüssel + Binding)
        dev.setPublicIdentityKey(req.ikPub());   // Base64(SPKI) speichern
        dev.setPublicKxKey(req.kxPub());         // Base64(SPKI) speichern
        dev.setIdentityBindingSig(bindSig);      // raw bytes
        dev.setLastSeenAt(Instant.now());
        deviceRepo.save(dev);

        // 7) Aufräumen (Replay verhindern)
        nonces.remove(deviceId);
        nonceExpiry.remove(deviceId);
    }

    /* ===== Helpers ===== */

    private static byte[] b64(String s) {
        return Base64.getDecoder().decode(s);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static byte[] sha256(byte[] data) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] concat(byte[]... arrs) {
        int len = 0; for (var a : arrs) len += a.length;
        byte[] out = new byte[len];
        int p = 0; for (var a : arrs) { System.arraycopy(a, 0, out, p, a.length); p += a.length; }
        return out;
    }

    private static boolean verifyEd25519(PublicKey ikPub, byte[] msg, byte[] sig) {
        try {
            var verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(ikPub);
            verifier.update(msg);
            return verifier.verify(sig);
        } catch (Exception e) {
            return false;
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
