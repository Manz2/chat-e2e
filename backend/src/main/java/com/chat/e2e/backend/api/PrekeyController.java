// src/main/java/com/chat/e2e/backend/api/PrekeyController.java
package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.*;
import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import com.chat.e2e.backend.keys.PrekeyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/v1/devices")
public class PrekeyController {
    private final UserDeviceRepository deviceRepo;
    private final PrekeyService prekeyService;

    // TODO: Extract to filter/interceptor that validates X-Device-Certificate & X-Device-Signature
    private UserDevice authenticateDevice(UUID deviceId) {
        return deviceRepo.findById(deviceId).orElseThrow(() -> new IllegalArgumentException("device not found"));
    }

    @PutMapping("/{deviceId}/prekeys/signed")
    @Operation(summary="Upload/rotate Signed PreKey")
    public ResponseEntity<?> uploadSpk(@PathVariable UUID deviceId,
                                       @RequestBody DTOs.SpkUploadRequest req) {
        var dev = authenticateDevice(deviceId);
        Instant validUntil = req.validUntil() == null ? null : Instant.parse(req.validUntil());
        prekeyService.uploadSpk(dev, req.keyId(), req.publicKey(), req.signature(), validUntil);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{deviceId}/prekeys/one-time")
    @Operation(summary="Upload One-Time PreKeys")
    public ResponseEntity<?> uploadOpks(@PathVariable UUID deviceId,
                                        @RequestBody DTOs.OpkUploadRequest req) {
        var dev = authenticateDevice(deviceId);
        var items = req.opks().stream()
                .map(o -> new com.chat.e2e.backend.keys.PrekeyService.PrekeyItem(o.keyId(), o.publicKey()))
                .toList();
        int stored = prekeyService.uploadOpks(dev, items);
        return ResponseEntity.ok(Map.of("stored", stored));
    }

    @GetMapping("/{deviceId}/prekeys/one-time/status")
    @Operation(summary="Get One-Time PreKey availability")
    public ResponseEntity<DTOs.OpkStatusResponse> status(@PathVariable UUID deviceId) {
        var dev = authenticateDevice(deviceId);
        return ResponseEntity.ok(new DTOs.OpkStatusResponse(prekeyService.countAvailableOpk(dev)));
    }
}
