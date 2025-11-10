package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.ConversationService;
import com.chat.e2e.backend.chat.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConversationControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean ConversationService conversationService;
    @MockBean MessageService messageService;

    @BeforeEach void setup() { Mockito.reset(conversationService, messageService); }

    @Test
    void createConversation() throws Exception {
        var resp = new DTOs.CreateConversationResponse(
                UUID.randomUUID(), Instant.parse("2025-11-06T00:00:00Z"));
        Mockito.when(conversationService.createConversation(any())).thenReturn(resp);

        var req = new DTOs.CreateConversationRequest(false, Set.of("alice","bob"));

        mvc.perform(post("/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(resp.conversationId().toString()));
    }

    @Test
    void addMemberDevices() throws Exception {
        var req = new DTOs.AddMemberDevicesRequest(
                "alice", Set.of(UUID.randomUUID(), UUID.randomUUID()));

        mvc.perform(post("/v1/conversations/{id}/members/devices", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void distributeCK() throws Exception {
        var convId = UUID.randomUUID();
        var sendResp = new DTOs.SendMessageResponse(UUID.randomUUID(), Instant.now(), 3);
        Mockito.when(messageService.distributeCK(eq(convId), any())).thenReturn(sendResp);

        var req = new DTOs.DistributeCKRequest(
                2,
                Map.of(UUID.randomUUID(),
                        java.util.Base64.getEncoder().encodeToString("sealed".getBytes())),
                "sigB64",
                UUID.randomUUID()
        );

        mvc.perform(post("/v1/conversations/{id}/control/ck", convId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveries").value(3));
    }

    @Test
    void sendMessage() throws Exception {
        var convId = UUID.randomUUID();
        var resp = new DTOs.SendMessageResponse(UUID.randomUUID(), Instant.now(), 5);
        Mockito.when(messageService.send(eq(convId), any(), any(), any())).thenReturn(resp);

        var req = new DTOs.SendMessageRequest(
                "text/plain", 1, 42L,
                java.util.Base64.getEncoder().encodeToString("cipher".getBytes()));

        mvc.perform(post("/v1/conversations/{id}/messages", convId)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-Device-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveries").value(5));
    }
}
