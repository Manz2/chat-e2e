package com.chat.e2e.backend.user;

import com.chat.e2e.backend.api.AppUserController;
import com.chat.e2e.backend.api.dto.DTOs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AppUserControllerTest.TestConfig.class)
class AppUserControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        AppUserService appUserService() {
            return Mockito.mock(AppUserService.class);
        }
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserService service;

    @BeforeEach
    void resetMocks() { reset(service); }

    @Test
    void shouldRegisterNewUser() throws Exception {
        var req = new DTOs.RegisterUserRequest("alice", "Alice", "pw");
        var saved = AppUser.builder().id(UUID.randomUUID()).handle("alice").displayName("Alice").build();

        when(service.register("alice","Alice","pw")).thenReturn(saved);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.handle").value("alice"));

        verify(service).register("alice","Alice","pw");
    }

    @Test
    void shouldListAllUsers() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                AppUser.builder().id(UUID.randomUUID()).handle("bob").displayName("Bob").build()
        ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handle").value("bob"));

        verify(service).findAll();
    }
}
