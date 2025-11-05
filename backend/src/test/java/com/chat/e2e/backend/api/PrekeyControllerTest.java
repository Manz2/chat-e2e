package com.chat.e2e.backend.api;

import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.device.UserDeviceRepository;
import com.chat.e2e.backend.keys.PrekeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrekeyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PrekeyControllerTest.MockConfig.class)
class PrekeyControllerTest {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        UserDeviceRepository deviceRepo() { return Mockito.mock(UserDeviceRepository.class); }

        @Bean @Primary
        PrekeyService prekeyService() { return Mockito.mock(PrekeyService.class); }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserDeviceRepository deviceRepo;
    @Autowired PrekeyService prekeyService;

    @BeforeEach void setup() { reset(deviceRepo, prekeyService); }

    @Test
    void uploadSpk_shouldSucceed() throws Exception {
        var devId = UUID.randomUUID();
        Mockito.when(deviceRepo.findById(devId))
                .thenReturn(java.util.Optional.of(
                        com.chat.e2e.backend.device.UserDevice.builder().id(devId).build()
                ));

        var req = new DTOs.SpkUploadRequest(23, "SPK_PUB", "SPK_SIG", "2026-01-01T00:00:00Z");

        mockMvc.perform(put("/v1/devices/{id}/prekeys/signed", devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        Mockito.verify(prekeyService).uploadSpk(
                any(com.chat.e2e.backend.device.UserDevice.class),
                eq(23), eq("SPK_PUB"), eq("SPK_SIG"), eq(Instant.parse("2026-01-01T00:00:00Z"))
        );
    }

    @Test
    void uploadOpks_shouldReturnStoredCount() throws Exception {
        var devId = UUID.randomUUID();
        Mockito.when(deviceRepo.findById(devId))
                .thenReturn(java.util.Optional.of(
                        com.chat.e2e.backend.device.UserDevice.builder().id(devId).build()
                ));
        Mockito.when(prekeyService.uploadOpks(any(), any())).thenReturn(2);

        var req = new DTOs.OpkUploadRequest(List.of(new DTOs.OpkItem(1001,"OPK1"), new DTOs.OpkItem(1002,"OPK2")));

        mockMvc.perform(put("/v1/devices/{id}/prekeys/one-time", devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stored").value(2));
    }

    @Test
    void status_shouldReturnAvailable() throws Exception {
        var devId = UUID.randomUUID();
        Mockito.when(deviceRepo.findById(devId))
                .thenReturn(java.util.Optional.of(
                        com.chat.e2e.backend.device.UserDevice.builder().id(devId).build()
                ));
        Mockito.when(prekeyService.countAvailableOpk(any())).thenReturn(137L);

        mockMvc.perform(get("/v1/devices/{id}/prekeys/one-time/status", devId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(137));
    }
}
