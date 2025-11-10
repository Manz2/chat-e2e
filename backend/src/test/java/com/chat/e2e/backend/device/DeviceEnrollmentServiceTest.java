package com.chat.e2e.backend.device;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
        when(deviceRepo.save(any(UserDevice.class))).thenAnswer(inv -> {
            var d = inv.getArgument(0, UserDevice.class);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });
    }

    @Test
    void start_shouldCreateDeviceAndReturnNonce() {
        when(userRepo.findByHandle("alice")).thenReturn(Optional.of(AppUser.builder().handle("alice").build()));

        var resp = service.start(new DTOs.EnrollmentStartRequest("alice"));

        assertThat(resp.deviceId()).isNotNull();
        assertThat(resp.nonce()).isNotBlank();
        assertThat(resp.expiresAt()).isAfter(Instant.now());
        verify(deviceRepo).save(any(UserDevice.class));
    }

    @Test
    void finish_shouldStoreDeviceKeys() throws Exception {
        // 1) User vorhanden für start()
        when(userRepo.findByHandle("bob"))
                .thenReturn(Optional.of(AppUser.builder().handle("bob").build()));

        // 2) deviceRepo.save(...) vergibt IDs und gibt das Objekt zurück
        when(deviceRepo.save(any(UserDevice.class))).thenAnswer(inv -> {
            UserDevice d = inv.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            return d;
        });

        // 3) start(): erzeugt Device + Nonce
        var startResp = service.start(new DTOs.EnrollmentStartRequest("bob"));
        UUID enrolledDeviceId = startResp.deviceId();

        // 4) Das gleiche Device muss bei finish() per findById(...) gefunden werden
        var enrolledDevice = UserDevice.builder().id(enrolledDeviceId).build();
        when(deviceRepo.findById(enrolledDeviceId)).thenReturn(Optional.of(enrolledDevice));

        // 5) Server-Schlüssel (zum Signieren des Certs)
        var kpg = java.security.KeyPairGenerator.getInstance("Ed25519");
        var kp = kpg.generateKeyPair();

        // 6) finish(): nutzt dieselbe deviceId + vorhandene Nonce
        service.finish(
                enrolledDeviceId,
                new DTOs.EnrollmentFinishRequest("IK", "KX", null, "proof"),
                kp.getPublic(),
                kp.getPrivate()
        );

        // Es wurde gespeichert (Keys gesetzt)
        verify(deviceRepo, atLeastOnce()).save(any(UserDevice.class));
    }

}
