package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final ConversationService conversationService;

    @GetMapping("/bootstrap")
    public ResponseEntity<DTOs.BootstrapResponse> bootstrap(Principal principal) {
        // principal.getName() → userHandle oder userId (je nach Security-Setup)
        return ResponseEntity.ok(conversationService.bootstrap(principal));
    }
}
