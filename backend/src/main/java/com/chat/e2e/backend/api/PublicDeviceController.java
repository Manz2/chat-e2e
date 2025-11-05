package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.UserDeviceRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/v1/users")
public class PublicDeviceController {
    private final UserDeviceRepository deviceRepo;

    @GetMapping("/{handle}/devices")
    @Operation(summary="List public devices of a user")
    public ResponseEntity<List<DTOs.DeviceSummary>> list(@PathVariable String handle,
                                                         @RequestParam(defaultValue="false", name="include_revoked") boolean includeRevoked) {
        var devices = deviceRepo.findByUserHandle(handle, includeRevoked).stream()
                .map(d -> new DTOs.DeviceSummary(d.getId(), d.getDeviceName(), d.getPlatform(), d.getCreatedAt(), d.getLastSeenAt(), d.getRevokedAt()!=null))
                .toList();
        return ResponseEntity.ok(devices);
    }
}
