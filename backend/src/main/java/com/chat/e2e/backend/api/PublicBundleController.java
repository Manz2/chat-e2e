package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.keys.PrekeyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController @RequiredArgsConstructor
@RequestMapping("/v1")
public class PublicBundleController {
    private final PrekeyService prekeyService;

    @GetMapping("/prekey-bundle")
    @Operation(summary = "Fetch PreKey bundle (public)")
    public ResponseEntity<DTOs.PrekeyBundleResponse> bundle(@RequestParam UUID deviceId) {
        return ResponseEntity.ok(prekeyService.buildBundle(deviceId));
    }
}
