// src/main/java/com/chat/e2e/backend/api/DeviceEnrollmentController.java
package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.*;
import com.chat.e2e.backend.device.DeviceEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
@RequestMapping("/v1/devices")
public class DeviceEnrollmentController {
    private final DeviceEnrollmentService service;

    // In real life inject server keys via bean/config
    private final KeyPair serverSigKeyPair = gen();

    @PostMapping("/enroll/start")
    @Operation(summary="Start device enrollment (get nonce + deviceId)")
    public ResponseEntity<DTOs.EnrollmentStartResponse> start(@RequestBody DTOs.EnrollmentStartRequest req) {
        return ResponseEntity.ok(service.start(req));
    }

    @PostMapping("/{deviceId}/enroll/finish")
    @Operation(summary="Finish device enrollment (issue device certificate)")
    public ResponseEntity<DTOs.EnrollmentFinishResponse> finish(@PathVariable UUID deviceId,
                                                                @RequestBody DTOs.EnrollmentFinishRequest req) {
        return ResponseEntity.ok(service.finish(deviceId, req, serverSigKeyPair.getPublic(), serverSigKeyPair.getPrivate()));
    }

    @PostMapping("/{deviceId}/revoke")
    @Operation(summary="Revoke a device (owner only)")
    public ResponseEntity<Void> revoke(@RequestHeader("X-User-Handle") String handle, // placeholder auth
                                       @PathVariable UUID deviceId) {
        // TODO: replace with proper user auth
        service.revoke(handle, deviceId);
        return ResponseEntity.noContent().build();
    }

    private static KeyPair gen() {
        try { var kpg = KeyPairGenerator.getInstance("Ed25519"); return kpg.generateKeyPair(); }
        catch(Exception e){ throw new RuntimeException(e); }
    }
}
