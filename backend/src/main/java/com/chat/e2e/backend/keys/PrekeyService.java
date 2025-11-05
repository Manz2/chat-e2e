package com.chat.e2e.backend.keys;


import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service @RequiredArgsConstructor
public class PrekeyService {
    private final UserDeviceRepository deviceRepo;
    private final UserKeyRepository keyRepo;

    @Transactional
    public void uploadSpk(UserDevice device, int keyId, String publicKey, String signature, java.time.Instant validUntil) {
        // Enforce single SPK: delete old or ensure unique with DB constraint
        var oldSpks = keyRepo.findSpks(device);
        oldSpks.forEach(keyRepo::delete);

        UserKey spk = UserKey.builder()
                .device(device)
                .type(UserKeyType.signed_prekey)
                .keyId(keyId)
                .publicKey(publicKey)
                .signature(signature == null ? null : java.util.Base64.getDecoder().decode(signature))
                .validUntil(validUntil)
                .used(false)
                .build();
        keyRepo.save(spk);
    }

    @Transactional
    public int uploadOpks(UserDevice device, List<PrekeyItem> items) {
        int n = 0;
        for (var it : items) {
            UserKey opk = UserKey.builder()
                    .device(device)
                    .type(UserKeyType.one_time_prekey)
                    .keyId(it.keyId)
                    .publicKey(it.publicKey)
                    .used(false)
                    .build();
            keyRepo.save(opk);
            n++;
        }
        return n;
    }

    public long countAvailableOpk(UserDevice device) {
        return keyRepo.countAvailableOpk(device);
    }

    // PUBLIC bundle assembly + atomic OPK claim
    @Transactional
    public DTOs.PrekeyBundleResponse buildBundle(UUID deviceId) {
        UserDevice d = deviceRepo.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("device not found"));
        var spks = keyRepo.findSpks(d);
        if (spks.isEmpty()) throw new IllegalStateException("no SPK available");
        var spk = spks.get(0);

        Integer opkId = null; String opkPub = null;
        var rows = keyRepo.claimOneOpk(deviceId); // atomic
        if (!rows.isEmpty()) {
            Object[] r = rows.get(0);
            opkId = ((Number) r[1]).intValue();
            opkPub = (String) r[2];
        }

        return new DTOs.PrekeyBundleResponse(
                d.getPublicIdentityKey(),
                d.getKeyCurve(),
                new DTOs.PrekeyBundleResponse.Spk(spk.getKeyId(), spk.getPublicKey(),
                        spk.getSignature() == null ? null : java.util.Base64.getEncoder().encodeToString(spk.getSignature())),
                new DTOs.PrekeyBundleResponse.Opk(opkId, opkPub),
                d.getPqkemPublicKey() == null ? null : java.util.Base64.getEncoder().encodeToString(d.getPqkemPublicKey()),
                null, // PQ prekey optional
                d.getCertPayload() == null ? null :
                        new DTOs.DeviceCertificate(d.getCertPayload(),
                                d.getCertSignature()==null?null:java.util.Base64.getEncoder().encodeToString(d.getCertSignature()),
                                "ed25519:root-2025")
        );
    }

    public record PrekeyItem(int keyId, String publicKey) {}
}

