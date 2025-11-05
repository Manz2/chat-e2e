package com.chat.e2e.backend.keys;

import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static com.chat.e2e.backend.keys.KeyCurve.x448;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PrekeyServiceTest {

    private UserDeviceRepository deviceRepo;
    private UserKeyRepository keyRepo;
    private PrekeyService service;

    @BeforeEach
    void setup() {
        deviceRepo = mock(UserDeviceRepository.class);
        keyRepo = mock(UserKeyRepository.class);
        service = new PrekeyService(deviceRepo, keyRepo);
    }

    @Test
    void uploadSpk_deletesOldAndSavesNew() {
        var device = UserDevice.builder().id(UUID.randomUUID()).build();
        when(keyRepo.findSpks(device)).thenReturn(List.of(UserKey.builder().id(UUID.randomUUID()).build()));

        // Verwende gültige Base64 Strings
        String spkPubB64 = Base64.getEncoder().encodeToString("SPK_PUB".getBytes());
        String spkSigB64 = Base64.getEncoder().encodeToString("SPK_SIG".getBytes());

        service.uploadSpk(device, 23, spkPubB64, spkSigB64, Instant.parse("2026-01-01T00:00:00Z"));

        verify(keyRepo, times(1)).delete(any(UserKey.class));
        verify(keyRepo).save(argThat(k ->
                k.getType() == UserKeyType.signed_prekey &&
                        k.getKeyId() == 23 &&
                        new String(Base64.getDecoder().decode(spkPubB64)).equals("SPK_PUB")
        ));
    }


    @Test
    void uploadOpks_storesAll() {
        var device = UserDevice.builder().id(UUID.randomUUID()).build();
        var items = List.of(new PrekeyService.PrekeyItem(1001,"OPK1"), new PrekeyService.PrekeyItem(1002,"OPK2"));

        int stored = service.uploadOpks(device, items);

        assertThat(stored).isEqualTo(2);
        verify(keyRepo, times(2)).save(any(UserKey.class));
    }

    @Test
    void buildBundle_claimsOpkAtomicallyAndReturnsBundle() {
        var deviceId = UUID.randomUUID();
        var device = UserDevice.builder()
                .id(deviceId)
                .publicIdentityKey("IK")
                .keyCurve(x448)
                .build();
        when(deviceRepo.findById(deviceId)).thenReturn(java.util.Optional.of(device));

        var spk = UserKey.builder()
                .type(UserKeyType.signed_prekey)
                .keyId(23)
                .publicKey("SPK_PUB")
                .signature("sig".getBytes())
                .build();
        when(keyRepo.findSpks(device)).thenReturn(List.of(spk));

        // simulate native query returning one OPK row: (id, key_id, public_key)
        when(keyRepo.claimOneOpk(deviceId))
                .thenReturn(Collections.singletonList(new Object[]{UUID.randomUUID(), 1001, "OPK_PUB"}));




        var bundle = service.buildBundle(deviceId);

        assertThat(bundle.ikPub()).isEqualTo("IK");
        assertThat(bundle.spk().keyId()).isEqualTo(23);
        assertThat(bundle.opk().keyId()).isEqualTo(1001);
    }
}
