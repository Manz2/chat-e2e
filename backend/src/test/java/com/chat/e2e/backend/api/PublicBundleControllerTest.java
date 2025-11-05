package com.chat.e2e.backend.api;


import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.keys.PrekeyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.chat.e2e.backend.keys.KeyCurve.x448;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicBundleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PublicBundleControllerTest.Config.class)
class PublicBundleControllerTest {

    @TestConfiguration
    static class Config {
        @Bean @Primary
        PrekeyService prekeyService() {
            return Mockito.mock(PrekeyService.class);
        }
    }


    @Autowired MockMvc mockMvc;
    @Autowired PrekeyService service;

    @Test
    void shouldReturnBundle() throws Exception {
        var resp = new DTOs.PrekeyBundleResponse(
                "IK_BASE64", x448,
                new DTOs.PrekeyBundleResponse.Spk(23,"SPK_PUB","SPK_SIG"),
                new DTOs.PrekeyBundleResponse.Opk(1001,"OPK_PUB"),
                null, null, null
        );
        Mockito.when(service.buildBundle(any())).thenReturn(resp);

        mockMvc.perform(get("/v1/prekey-bundle").param("deviceId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ikPub").value("IK_BASE64"))
                .andExpect(jsonPath("$.spk.keyId").value(23))
                .andExpect(jsonPath("$.opk.keyId").value(1001));
    }
}
