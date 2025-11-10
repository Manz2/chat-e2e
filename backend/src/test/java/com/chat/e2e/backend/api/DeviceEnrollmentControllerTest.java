package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.DeviceEnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceEnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DeviceEnrollmentControllerTest.MockConfig.class)
class DeviceEnrollmentControllerTest {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        DeviceEnrollmentService mockDeviceEnrollmentService() {
            return org.mockito.Mockito.mock(DeviceEnrollmentService.class);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @MockBean DeviceEnrollmentService service;

    @BeforeEach
    void setup() {
        reset(service);
    }

    @Test
    void shouldStartEnrollment() throws Exception {
        var resp = new DTOs.EnrollmentStartResponse(
                UUID.randomUUID(),
                "nonceB64",
                Instant.parse("2025-11-06T00:00:00Z")
        );
        Mockito.when(service.start(any())).thenReturn(resp);

        var req = new DTOs.EnrollmentStartRequest("alice");

        mockMvc.perform(post("/v1/devices/enroll/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonce").value("nonceB64"));
    }

    @Test
    void shouldFinishEnrollment() throws Exception {
        var deviceId = UUID.randomUUID();
        // Neues finish(deviceId, req)
        Mockito.doNothing().when(service).finish(any(), any());

        var req = new DTOs.EnrollmentFinishRequest(
                "base64-ed25519-pub",   // ikPub
                "base64-x25519-pub",    // kxPub
                "base64-bindingSig",    // bindingSig
                "base64-proof",         // proof
                "android",              // platform
                "Pixel 9"               // deviceName
        );

        mockMvc.perform(post("/v1/devices/{id}/enroll/finish", deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRevokeDevice() throws Exception {
        var deviceId = UUID.randomUUID();
        Mockito.doNothing().when(service).revoke(any(), any());

        mockMvc.perform(post("/v1/devices/{id}/revoke", deviceId)
                        .header("X-User-Handle", "alice"))
                .andExpect(status().isNoContent());
    }
}
