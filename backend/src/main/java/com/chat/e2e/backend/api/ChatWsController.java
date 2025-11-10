package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.MessageService;
import com.chat.e2e.backend.realtime.UserDevicePrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final MessageService messageService;
    private final SimpMessagingTemplate broker;

    @MessageMapping("/messages.send") // Client -> /app/messages.send
    public void send(DTOs.SendWsMessage msg, Principal principal) throws JsonProcessingException {
        var p = (UserDevicePrincipal) principal;

        // reuse deiner HTTP-Logik:
        DTOs.SendMessageRequest req = new DTOs.SendMessageRequest(
                msg.contentType(), msg.epoch(), msg.counter(), msg.ciphertextB64()
        );
        DTOs.SendMessageResponse resp = messageService.send(msg.conversationId(), p.userId(), p.deviceId(), req);

        // Ack nur an Sender (per-User Queue)
        broker.convertAndSendToUser(
                p.userId().toString(),
                "/queue/device",
                new DTOs.SendAckEvent(resp.messageId(), msg.conversationId(), resp.createdAt(), resp.deliveries())
        );

        // Deliveries an Empfänger (falls du userId pro Device brauchst, gib sie aus Service zurück)
        // Beispiel: MessageService könnte dir Liste (deviceId -> userId) liefern:
        // List<Recipient> recipients = resp.recipients();
        // Fürs Beispiel verschicken wir generisch an /topic/device.{id}
        // (Besser: convertAndSendToUser(userId, "/queue/device", ...))
    }

    @MessageMapping("/messages.read")
    public void read(DTOs.ReadWsMessage msg, Principal principal) {
        var p = (UserDevicePrincipal) principal;
        messageService.markRead(p.deviceId(), msg.messageId());

        // Broadcast READ an andere Participant-Devices (hier exemplarisch Topic):
        broker.convertAndSend("/topic/conversation." + msg.conversationId(),
                new DTOs.ReadEvent(msg.conversationId(), msg.messageId(), p.deviceId(), Instant.now()));
    }
}
