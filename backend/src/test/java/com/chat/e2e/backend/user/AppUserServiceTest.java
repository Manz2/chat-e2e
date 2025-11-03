package com.chat.e2e.backend.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppUserServiceTest {

    private AppUserRepository repo;
    private BCryptPasswordEncoder encoder;
    private AppUserService service;

    @BeforeEach
    void setup() {
        repo = mock(AppUserRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new AppUserService(repo, encoder);

        when(repo.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }


    @Test
    void register_shouldHashPasswordAndSaveUser() {
        // given
        when(repo.findByHandle("alice")).thenReturn(Optional.empty());

        // when
        AppUser savedUser = service.register("alice", "Alice", "secret123");

        // then
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(repo).save(captor.capture());

        AppUser user = captor.getValue();
        assertThat(user.getHandle()).isEqualTo("alice");
        assertThat(user.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", user.getPasswordHash())).isTrue();
        assertThat(savedUser.getId()).isNull(); // id wird von DB gesetzt
    }

    @Test
    void register_shouldThrowIfHandleAlreadyExists() {
        // given
        when(repo.findByHandle("bob")).thenReturn(Optional.of(new AppUser()));

        // then
        assertThatThrownBy(() ->
                service.register("bob", "Bob", "pw")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyPassword_shouldReturnTrueForValidPassword() {
        String raw = "mypw";
        String hash = encoder.encode(raw);
        AppUser user = AppUser.builder().passwordHash(hash).build();

        assertThat(service.verifyPassword(user, raw)).isTrue();
        assertThat(service.verifyPassword(user, "wrong")).isFalse();
    }
}
