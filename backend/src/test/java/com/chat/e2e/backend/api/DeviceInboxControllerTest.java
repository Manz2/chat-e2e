package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.DeviceInboxController;
import com.chat.e2e.backend.chat.MessageService;
import org.junit.jupiter.api.Test;                       // <-- JUnit 5
import org.junit.jupiter.api.extension.ExtendWith;     // <-- JUnit 5
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DeviceInboxController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceInboxControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    MessageService messageService;

    @Test
    void inbox() throws Exception {
        var item = new DTOs.DeliveryDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "text/plain",
                "{\"epoch\":1,\"counter\":1}",
                Base64.getEncoder().encodeToString("ct".getBytes()),
                Instant.now()
        );

        Mockito.when(messageService.fetchInbox(any(UUID.class), any(), anyInt()))
                .thenReturn(List.of(item));
        Mockito.when(messageService.computeNextCursor(anyList(), any()))
                .thenReturn("123:abc");

        mvc.perform(get("/v1/devices/{id}/inbox", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].contentType").value("text/plain"))
                .andExpect(jsonPath("$.nextCursor").value("123:abc"));
    }
}
