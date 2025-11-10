package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final MessageService messageService;

    @PostMapping("/ack")
    public ResponseEntity<Void> ack(@RequestBody DTOs.AckRequest req, Principal principal){
        // authz.checkDeviceOwnedByPrincipal(req.deviceId(), principal)
        messageService.ack(req.deviceId(), req.deliveryIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read")
    public ResponseEntity<Void> read(@RequestBody DTOs.ReadRequest req, Principal principal){
        // authz.checkDeviceOwnedByPrincipal(req.deviceId(), principal)
        messageService.markRead(req.deviceId(), req.messageId());
        return ResponseEntity.noContent().build();
    }
}
