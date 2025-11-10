package com.chat.e2e.backend.device;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceEnrollmentServiceTest {

    private AppUserRepository userRepo;
    private UserDeviceRepository deviceRepo;
    private DeviceEnrollmentService service;

    @BeforeEach
    void setup() {
        userRepo = mock(AppUserRepository.class);
        deviceRepo = mock(UserDeviceRepository.class);
        service = new DeviceEnrollmentService(userRepo, deviceRepo);

        // save(...) vergibt eine ID falls null und gibt das Objekt zurück
        when(deviceRepo.save(any(UserDevice.class))).thenAnswer(inv -> {
            var d = inv.getArgument(0, UserDevice.class);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
    }

    @Test
    void start_shouldCreateDeviceAndReturnNonce() {
        when(userRepo.findByHandle("alice"))
                .thenReturn(Optional.of(AppUser.builder().handle("alice").build()));

        var resp = service.start(new DTOs.EnrollmentStartRequest("alice"));

        assertThat(resp.deviceId()).isNotNull();
        assertThat(resp.nonce()).isNotBlank();
        assertThat(resp.expiresAt()).isAfter(Instant.now());
        verify(deviceRepo).save(any(UserDevice.class));
    }

    @Test
    void finish_shouldStoreDeviceKeys() throws Exception {
        // 1) User für start()
        when(userRepo.findByHandle("bob"))
                .thenReturn(Optional.of(AppUser.builder().handle("bob").build()));

        // 2) start(): Device + Nonce erzeugen
        var startResp = service.start(new DTOs.EnrollmentStartRequest("bob"));
        UUID deviceId = startResp.deviceId();
        String nonceB64 = startResp.nonce(); // vom Service erzeugt (Base64-String)

        // 3) findById(...) muss das gleiche Device liefern
        var storedDevice = UserDevice.builder().id(deviceId).build();
        when(deviceRepo.findById(deviceId)).thenReturn(Optional.of(storedDevice));

        // --- Krypto vorbereiten ---

        // Ed25519 (Identität)
        var edKpg = java.security.KeyPairGenerator.getInstance("Ed25519");
        KeyPair ed = edKpg.generateKeyPair();
        byte[] ikSpki = ed.getPublic().getEncoded(); // X.509 SPKI
        String ikB64 = java.util.Base64.getEncoder().encodeToString(ikSpki);

        // X25519 (KX)
        var xKpg = java.security.KeyPairGenerator.getInstance("X25519");
        KeyPair xk = xKpg.generateKeyPair();
        byte[] kxSpki = xk.getPublic().getEncoded(); // X.509 SPKI
        String kxB64 = java.util.Base64.getEncoder().encodeToString(kxSpki);

        // Binding-Signatur: Ed25519.sign( SHA256("bind:" || kxSpki) )
        byte[] bindMsg = sha256(concat("bind:".getBytes(java.nio.charset.StandardCharsets.UTF_8), kxSpki));
        String bindingSigB64 = base64(edSign(ed.getPrivate(), bindMsg));

        // Proof-of-Possession:
        // Ed25519.sign( SHA256("enroll:" || deviceId || nonceB64 || ikSpki || kxSpki) )
        byte[] enrollMsg = sha256(concat(
                "enroll:".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                deviceId.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                nonceB64.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                ikSpki,
                kxSpki
        ));
        String proofB64 = base64(edSign(ed.getPrivate(), enrollMsg));

        var finishReq = new DTOs.EnrollmentFinishRequest(
                ikB64,          // ikPub (Base64/SPKI)
                kxB64,          // kxPub (Base64/SPKI)
                bindingSigB64,  // bindingSig (Base64)
                proofB64,       // proof (Base64)
                "android",      // optional
                "Pixel 9"       // optional
        );

        // 4) finish() aufrufen
        service.finish(deviceId, finishReq);

        // 5) Persistenz geprüft
        verify(deviceRepo, atLeastOnce()).save(any(UserDevice.class));
        assertThat(storedDevice.getPublicIdentityKey()).isEqualTo(ikB64);
        assertThat(storedDevice.getPublicKxKey()).isEqualTo(kxB64);
        assertThat(storedDevice.getIdentityBindingSig())
                .isEqualTo(java.util.Base64.getDecoder().decode(bindingSigB64));
        assertThat(storedDevice.getDeviceName()).isEqualTo("Pixel 9");
        assertThat(storedDevice.getPlatform()).isEqualTo("android");
        assertThat(storedDevice.getLastSeenAt()).isNotNull();
    }

    /* ===== lokale Test-Helper ===== */
    private static byte[] concat(byte[]... arrs) {
        int len = 0; for (var a : arrs) len += a.length;
        byte[] out = new byte[len];
        int p = 0; for (var a : arrs) { System.arraycopy(a, 0, out, p, a.length); p += a.length; }
        return out;
    }
    private static byte[] sha256(byte[] data) throws Exception {
        var md = java.security.MessageDigest.getInstance("SHA-256");
        return md.digest(data);
    }
    private static byte[] edSign(java.security.PrivateKey sk, byte[] msg) throws Exception {
        var sig = java.security.Signature.getInstance("Ed25519");
        sig.initSign(sk);
        sig.update(msg);
        return sig.sign();
    }
    private static String base64(byte[] b) {
        return java.util.Base64.getEncoder().encodeToString(b);
    }

}
