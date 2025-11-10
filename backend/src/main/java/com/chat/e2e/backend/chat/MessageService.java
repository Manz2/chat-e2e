package com.chat.e2e.backend.chat;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;
    private final ConversationMemberDeviceRepository memberDeviceRepo;
    private final MessageCoreRepository messageCoreRepo;
    private final MessageDeliveryRepository messageDeliveryRepo;
    private final UserDeviceRepository deviceRepo;
    private final ObjectMapper mapper = new ObjectMapper();


    /** Normale Nachricht: identischer Ciphertext für alle Zielgeräte */
    @Transactional
    public DTOs.SendMessageResponse send(UUID conversationId,
                                             UUID senderUserId,
                                             UUID senderDeviceId,
                                             DTOs.SendMessageRequest req) throws JsonProcessingException {


        // 0) Checks
        var conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("conversation not found"));

        if (!memberRepo.existsByConversationIdAndUserId(conversationId, senderUserId))
            throw new IllegalArgumentException("sender not a member");

        var senderDevice = deviceRepo.findById(senderDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("sender device not found"));

        // 1) message_core anlegen
        var header = new java.util.HashMap<String,Object>();
        header.put("type", "text");
        header.put("epoch", req.epoch());
        header.put("counter", req.counter());
        header.put("content_type", req.contentType());

        var headerJson = mapper.writeValueAsString(header);


        var core = MessageCore.builder()
                .conversationId(conversationId)
                .senderId(senderUserId)
                .createdAt(Instant.now())
                .contentType(req.contentType())
                .header(headerJson)
                .build();
        core = messageCoreRepo.save(core);

        // 2) Zielgeräte bestimmen (alle Geräte aller Mitglieder, i. d. R. ohne gesperrte/abgemeldete)
        var recipientDeviceIds = memberDeviceRepo.findAllDeviceIdsByConversationId(conversationId)
                .stream()
                // optional: Sendergerät ausschließen (Echo vermeiden)
                .filter(id -> !id.equals(senderDeviceId))
                .collect(Collectors.toSet());

        // 3) Per-Device-Delivery erzeugen (identischer Ciphertext)
        var msgHeader = new java.util.HashMap<String,Object>();
        msgHeader.put("epoch", req.epoch());
        msgHeader.put("counter", req.counter());

        byte[] ciphertext = java.util.Base64.getDecoder().decode(req.ciphertextB64());

        int deliveries = 0;
        for (UUID devId : recipientDeviceIds) {
            // Option: Gerät ist revoked? dann auslassen
            UserDevice d = deviceRepo.findById(devId).orElse(null);
            if (d == null || d.getRevokedAt() != null) continue;

            var msgHeaderJson = mapper.writeValueAsString(msgHeader);


            var delivery = MessageDelivery.builder()
                    .messageId(core.getId())
                    .recipientDeviceId(devId)
                    .ciphertext(ciphertext)
                    .msgHeader(msgHeaderJson)
                    .build();
            messageDeliveryRepo.save(delivery);
            deliveries++;
        }

        return new DTOs.SendMessageResponse(core.getId(), core.getCreatedAt(), deliveries);
    }

    /** CK-Verteilung: sealed CK pro Gerät, daher Map<deviceId, ciphertextB64> */
    @Transactional
    public DTOs.SendMessageResponse distributeCK(UUID conversationId, DTOs.DistributeCKRequest req) throws JsonProcessingException {
        var conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("conversation not found"));

        // Header als Control-Message
        var header = new java.util.HashMap<String,Object>();
        header.put("type", "ck_distribute");
        header.put("epoch", req.epoch());
        header.put("from_device", req.fromDeviceId() == null ? null : req.fromDeviceId().toString());
        header.put("sig", req.sigFromDevice()); // optional, server prüft nicht zwingend

        var headerJson = mapper.writeValueAsString(header);


        var core = MessageCore.builder()
                .conversationId(conversationId)
                .senderId(null) // optional: System/Control; oder Owner-User setzen
                .createdAt(Instant.now())
                .contentType("control/ck_distribute")
                .header(headerJson)
                .build();
        core = messageCoreRepo.save(core);

        int deliveries = 0;
        for (var e : req.sealedForDevice().entrySet()) {
            UUID devId = e.getKey();
            byte[] sealed = java.util.Base64.getDecoder().decode(e.getValue());

            var msgHeaderJson = mapper.writeValueAsString(Map.of("epoch", req.epoch(), "counter", 0));

            // Option: prüfen, ob Gerät zur Konversation gehört
            if (!memberDeviceRepo.existsByConversationIdAndDeviceId(conversationId, devId)) continue;

            var delivery = MessageDelivery.builder()
                    .messageId(core.getId())
                    .recipientDeviceId(devId)
                    .ciphertext(sealed)
                    .msgHeader(msgHeaderJson)
                    .build();
            messageDeliveryRepo.save(delivery);
            deliveries++;
        }
        return new DTOs.SendMessageResponse(core.getId(), core.getCreatedAt(), deliveries);
    }
    @Transactional(readOnly = true)
    public List<DTOs.DeliveryDTO> fetchInbox(UUID deviceId, String sinceCursor, int limit) {
        var cursor = Cursor.decode(sinceCursor); // createdAt + messageId oder null
        var page = org.springframework.data.domain.PageRequest.of(0, limit);
        List<Object[]> rows = messageDeliveryRepo.findNextForDevice(deviceId, cursor.createdAt, cursor.messageId, page);

        List<DTOs.DeliveryDTO> out = new ArrayList<>();
        for (Object[] r : rows) {
            // mapping: d.id, m.id, m.conversationId, m.contentType, m.header(json), d.ciphertext(bytea), m.createdAt
            UUID deliveryId = (UUID) r[0];
            UUID messageId  = (UUID) r[1];
            UUID convId     = (UUID) r[2];
            String ctype    = (String) r[3];
            String header   = (String) r[4];
            byte[] ct       = (byte[]) r[5];
            Instant created = (Instant) r[6];

            out.add(new DTOs.DeliveryDTO(
                    deliveryId, messageId, convId, ctype, header,
                    Base64.getEncoder().encodeToString(ct), created
            ));
        }
        return out;
    }

    public String computeNextCursor(List<DTOs.DeliveryDTO> items, String prev) {
        if (items == null || items.isEmpty()) return prev; // oder null
        DTOs.DeliveryDTO last = items.get(items.size()-1);
        return Cursor.encode(last.createdAt(), last.messageId());
    }

    @Transactional
    public void ack(UUID deviceId, List<UUID> deliveryIds) {
        if (deliveryIds == null || deliveryIds.isEmpty()) return;
        messageDeliveryRepo.bulkSetDeliveredAt(deviceId, deliveryIds, Instant.now());
    }

    @Transactional
    public void markRead(UUID deviceId, UUID messageId) {
        messageDeliveryRepo.updateRead(deviceId, messageId, Instant.now());
        // optional: publish READ event
    }

    // --- kleiner Cursor-Helper ---
    static class Cursor {
        final Instant createdAt;
        final UUID messageId;
        Cursor(Instant c, UUID id){ this.createdAt=c; this.messageId=id; }
        static Cursor decode(String s){
            if (s==null || s.isBlank()) return new Cursor(null, null);
            // simple format: "<epochSecond>:<uuid>"
            try {
                String[] p = s.split(":");
                return new Cursor(Instant.ofEpochSecond(Long.parseLong(p[0])), UUID.fromString(p[1]));
            } catch(Exception e){ return new Cursor(null, null); }
        }
        static String encode(Instant ts, UUID id){
            return ts==null||id==null ? null : (ts.getEpochSecond() + ":" + id);
        }
    }
}
