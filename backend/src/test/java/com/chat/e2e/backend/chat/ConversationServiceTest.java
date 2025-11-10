package com.chat.e2e.backend.chat;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ConversationRepository conversationRepo;
    private ConversationMemberRepository memberRepo;
    private ConversationMemberDeviceRepository memberDeviceRepo;
    private AppUserRepository userRepo;
    private UserDeviceRepository deviceRepo;
    private ConversationService service;

    @BeforeEach
    void setup() {
        conversationRepo = mock(ConversationRepository.class);
        memberRepo = mock(ConversationMemberRepository.class);
        memberDeviceRepo = mock(ConversationMemberDeviceRepository.class);
        userRepo = mock(AppUserRepository.class);
        deviceRepo = mock(UserDeviceRepository.class);
        service = new ConversationService(conversationRepo, memberRepo, memberDeviceRepo, userRepo, deviceRepo);

        when(conversationRepo.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            if (c.getCreatedAt() == null) c.setCreatedAt(Instant.now());
            return c;
        });
    }

    @Test
    void createConversation_ok() {
        when(userRepo.findByHandle("alice")).thenReturn(Optional.of(AppUser.builder().id(UUID.randomUUID()).handle("alice").build()));
        when(userRepo.findByHandle("bob")).thenReturn(Optional.of(AppUser.builder().id(UUID.randomUUID()).handle("bob").build()));

        var resp = service.createConversation(new DTOs.CreateConversationRequest(false, Set.of("alice","bob")));

        assertThat(resp.conversationId()).isNotNull();
        verify(memberRepo, times(2)).save(any(ConversationMember.class));
    }

    @Test
    void addMemberDevices_ok() {
        var convId = UUID.randomUUID();
        var user = AppUser.builder().id(UUID.randomUUID()).handle("alice").build();

        when(userRepo.findByHandle("alice")).thenReturn(Optional.of(user));
        when(memberRepo.existsByConversationIdAndUserId(convId, user.getId())).thenReturn(true);

        var d1 = UserDevice.builder().id(UUID.randomUUID()).user(user).build();
        var d2 = UserDevice.builder().id(UUID.randomUUID()).user(user).build();
        when(deviceRepo.findById(d1.getId())).thenReturn(Optional.of(d1));
        when(deviceRepo.findById(d2.getId())).thenReturn(Optional.of(d2));

        service.addMemberDevices(convId, new DTOs.AddMemberDevicesRequest("alice", Set.of(d1.getId(), d2.getId())));

        verify(memberDeviceRepo, times(2)).save(any(ConversationMemberDevice.class));
    }

    @Test
    void addMemberDevices_rejectsNonMember() {
        var convId = UUID.randomUUID();
        var user = AppUser.builder().id(UUID.randomUUID()).handle("mallory").build();
        when(userRepo.findByHandle("mallory")).thenReturn(Optional.of(user));
        when(memberRepo.existsByConversationIdAndUserId(convId, user.getId())).thenReturn(false);

        assertThatThrownBy(() ->
                service.addMemberDevices(convId, new DTOs.AddMemberDevicesRequest("mallory", Set.of(UUID.randomUUID()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user not member");
    }
}
