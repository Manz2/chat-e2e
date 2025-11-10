package com.chat.e2e.backend.chat;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageServiceTest {

    private ConversationRepository conversationRepo;
    private ConversationMemberRepository memberRepo;
    private ConversationMemberDeviceRepository memberDeviceRepo;
    private MessageCoreRepository messageCoreRepo;
    private MessageDeliveryRepository messageDeliveryRepo;
    private UserDeviceRepository deviceRepo;
    private MessageService service;

    @BeforeEach
    void setup() {
        conversationRepo = mock(ConversationRepository.class);
        memberRepo = mock(ConversationMemberRepository.class);
        memberDeviceRepo = mock(ConversationMemberDeviceRepository.class);
        messageCoreRepo = mock(MessageCoreRepository.class);
        messageDeliveryRepo = mock(MessageDeliveryRepository.class);
        deviceRepo = mock(UserDeviceRepository.class);
        service = new MessageService(conversationRepo, memberRepo, memberDeviceRepo, messageCoreRepo, messageDeliveryRepo, deviceRepo);

        when(messageCoreRepo.save(any(MessageCore.class))).thenAnswer(inv -> {
            var m = inv.getArgument(0, MessageCore.class);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            if (m.getCreatedAt() == null) m.setCreatedAt(Instant.now());
            return m;
        });
    }

    @Test
    void send_ok_fansOutToDevices() throws JsonProcessingException {
        var convId = UUID.randomUUID();
        var senderUser = UUID.randomUUID();
        var senderDevice = UUID.randomUUID();

        when(conversationRepo.findById(convId)).thenReturn(Optional.of(Conversation.builder().id(convId).isGroup(true).createdAt(Instant.now()).build()));
        when(memberRepo.existsByConversationIdAndUserId(convId, senderUser)).thenReturn(true);

        var d1 = UUID.randomUUID(); var d2 = UUID.randomUUID(); var d3 = senderDevice; // d3 ist Sender → filtern
        when(memberDeviceRepo.findAllDeviceIdsByConversationId(convId)).thenReturn(Set.of(d1, d2, d3));

        when(deviceRepo.findById(d1)).thenReturn(Optional.of(UserDevice.builder().id(d1).build()));
        when(deviceRepo.findById(d2)).thenReturn(Optional.of(UserDevice.builder().id(d2).revokedAt(null).build()));
        when(deviceRepo.findById(d3)).thenReturn(Optional.of(UserDevice.builder().id(d3).build()));

        var req = new DTOs.SendMessageRequest("text/plain", 7, 1001L,
                Base64.getEncoder().encodeToString("cipher!".getBytes()));

        var resp = service.send(convId, senderUser, senderDevice, req);

        assertThat(resp.messageId()).isNotNull();
        assertThat(resp.deliveries()).isEqualTo(2); // d1 & d2, Sendergerät rausgefiltert
        verify(messageDeliveryRepo, times(2)).save(any(MessageDelivery.class));
    }

    @Test
    void send_rejectsNonMember() {
        var convId = UUID.randomUUID();
        var senderUser = UUID.randomUUID();
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(Conversation.builder().id(convId).isGroup(false).createdAt(Instant.now()).build()));
        when(memberRepo.existsByConversationIdAndUserId(convId, senderUser)).thenReturn(false);

        var req = new DTOs.SendMessageRequest("text/plain", 1, 1L, Base64.getEncoder().encodeToString("x".getBytes()));

        assertThatThrownBy(() -> service.send(convId, senderUser, UUID.randomUUID(), req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sender not a member");
    }

    @Test
    void distributeCK_ok() throws JsonProcessingException {
        var convId = UUID.randomUUID();
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(Conversation.builder().id(convId).isGroup(true).createdAt(Instant.now()).build()));

        when(messageCoreRepo.save(any(MessageCore.class))).thenAnswer(inv -> {
            var m = inv.getArgument(0, MessageCore.class);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            if (m.getCreatedAt() == null) m.setCreatedAt(Instant.now());
            return m;
        });

        var d1 = UUID.randomUUID();
        var d2 = UUID.randomUUID();

        when(memberDeviceRepo.existsByConversationIdAndDeviceId(convId, d1)).thenReturn(true);
        when(memberDeviceRepo.existsByConversationIdAndDeviceId(convId, d2)).thenReturn(true);

        var req = new DTOs.DistributeCKRequest(
                3,
                java.util.Map.of(
                        d1, Base64.getEncoder().encodeToString("sealed1".getBytes()),
                        d2, Base64.getEncoder().encodeToString("sealed2".getBytes())
                ),
                "sigB64",
                UUID.randomUUID()
        );

        var resp = service.distributeCK(convId, req);

        assertThat(resp.deliveries()).isEqualTo(2);
        verify(messageDeliveryRepo, times(2)).save(any(MessageDelivery.class));
    }
}
