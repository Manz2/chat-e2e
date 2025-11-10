package com.chat.e2e.backend.chat;

import com.chat.e2e.backend.api.dto.DTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/devices")
@RequiredArgsConstructor
public class DeviceInboxController {

    private final MessageService messageService;

    @GetMapping("/{deviceId}/inbox")
    public ResponseEntity<DTOs.InboxResponse> inbox(@PathVariable UUID deviceId,
                                                    @RequestParam(required = false) String since,
                                                    @RequestParam(defaultValue = "50") int limit,
                                                    Principal principal) {
        // authz.checkDeviceOwnedByPrincipal(deviceId, principal) → TODO in Security-Filter/Service
        List<DTOs.DeliveryDTO> items = messageService.fetchInbox(deviceId, since, limit);
        String next = messageService.computeNextCursor(items, since);
        return ResponseEntity.ok(new DTOs.InboxResponse(items, next));
    }
}
