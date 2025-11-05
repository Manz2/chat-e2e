package com.chat.e2e.backend.api;

import com.chat.e2e.backend.device.UserDevice;
import com.chat.e2e.backend.device.UserDeviceRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicDeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PublicDeviceControllerTest.MockConfig.class)
class PublicDeviceControllerTest {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        UserDeviceRepository userDeviceRepository() {
            return org.mockito.Mockito.mock(UserDeviceRepository.class);
        }
    }

    @Autowired MockMvc mockMvc;
    @MockBean
    UserDeviceRepository repo;

    @BeforeEach void resetMocks(){ org.mockito.Mockito.reset(repo); }

    @Test
    void shouldListPublicDevices_defaultExcludesRevoked() throws Exception {
        var d1 = com.chat.e2e.backend.device.UserDevice.builder()
                .id(java.util.UUID.randomUUID())
                .deviceName("Pixel")
                .platform("android")
                .createdAt(java.time.Instant.parse("2025-01-01T00:00:00Z"))
                .lastSeenAt(java.time.Instant.parse("2025-01-02T00:00:00Z"))
                .build();

        org.mockito.Mockito.when(repo.findByUserHandle("alice", false))
                .thenReturn(java.util.List.of(d1));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/users/{handle}/devices", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value(d1.getId().toString()))
                .andExpect(jsonPath("$[0].platform").value("android"))
                .andExpect(jsonPath("$[0].revoked").value(false));
    } @Test
    void shouldIncludeRevokedIfRequested() throws Exception {
        when(repo.findByUserHandle("bob", true)).thenReturn(List.of());

        mockMvc.perform(get("/v1/users/{h}/devices?include_revoked=true", "bob"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

}
