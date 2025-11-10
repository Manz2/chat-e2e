package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.ConversationService;
import com.chat.e2e.backend.chat.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    // 1) Konversation anlegen
    @PostMapping
    public ResponseEntity<DTOs.CreateConversationResponse> create(
            @RequestBody DTOs.CreateConversationRequest req) {
        return ResponseEntity.ok(conversationService.createConversation(req));
    }

    // 2) Mitgliedsgeräte hinzufügen
    @PostMapping("/{conversationId}/members/devices")
    public ResponseEntity<Void> addMemberDevices(@PathVariable UUID conversationId,
                                                 @RequestBody DTOs.AddMemberDevicesRequest req) {
        conversationService.addMemberDevices(conversationId, req);
        return ResponseEntity.noContent().build();
    }

    // 3) CK verteilen (Control-Message) – sealed per Gerät
    @PostMapping("/{conversationId}/control/ck")
    public ResponseEntity<DTOs.SendMessageResponse> distributeCK(@PathVariable UUID conversationId,
                                                                     @RequestBody DTOs.DistributeCKRequest req) throws JsonProcessingException {
        var resp = messageService.distributeCK(conversationId, req);
        return ResponseEntity.ok(resp);
    }

    // 4) Normale Nachricht senden
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<DTOs.SendMessageResponse> sendMessage(@PathVariable UUID conversationId,
                                                                    @RequestHeader("X-User-Id") UUID senderUserId, // oder aus Auth
                                                                    @RequestHeader("X-Device-Id") UUID senderDeviceId,
                                                                    @RequestBody DTOs.SendMessageRequest req) throws JsonProcessingException {
        var resp = messageService.send(conversationId, senderUserId, senderDeviceId, req);
        return ResponseEntity.ok(resp);
    }
}
