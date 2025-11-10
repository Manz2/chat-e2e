package com.chat.e2e.backend.chat;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;
    private final ConversationMemberDeviceRepository memberDeviceRepo;
    private final AppUserRepository userRepo;
    private final UserDeviceRepository deviceRepo;

    @Transactional
    public DTOs.CreateConversationResponse createConversation(DTOs.CreateConversationRequest req) {
        var conv = Conversation.builder()
                .isGroup(Boolean.TRUE.equals(req.isGroup()))
                .createdAt(Instant.now())
                .build();
        conv = conversationRepo.save(conv);

        // Mitglieder anlegen
        if (req.memberHandles() != null) {
            for (var handle : req.memberHandles()) {
                AppUser u = userRepo.findByHandle(handle)
                        .orElseThrow(() -> new IllegalArgumentException("user not found: " + handle));
                var m = ConversationMember.builder()
                        .conversationId(conv.getId())
                        .userId(u.getId())
                        .role("member")
                        .build();
                memberRepo.save(m);
            }
        }
        return new DTOs.CreateConversationResponse(conv.getId(), conv.getCreatedAt());
    }

    @Transactional
    public void addMemberDevices(UUID conversationId, DTOs.AddMemberDevicesRequest req) {
        // Check, dass User Mitglied der Konversation ist
        var user = userRepo.findByHandle(req.handle())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        boolean isMember = memberRepo.existsByConversationIdAndUserId(conversationId, user.getId());
        if (!isMember) throw new IllegalArgumentException("user not member");

        // Jedes Gerät muss dem User gehören
        for (UUID devId : req.deviceIds()) {
            UserDevice d = deviceRepo.findById(devId)
                    .orElseThrow(() -> new IllegalArgumentException("device not found: " + devId));
            if (!d.getUser().getId().equals(user.getId()))
                throw new IllegalArgumentException("device does not belong to user: " + devId);

            // Eintrag in member_device
            var cmd = ConversationMemberDevice.builder()
                    .conversationId(conversationId)
                    .userId(user.getId())
                    .deviceId(devId)
                    .build();
            memberDeviceRepo.save(cmd);
        }
    }
}
