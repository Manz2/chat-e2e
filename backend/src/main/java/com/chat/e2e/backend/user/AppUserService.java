package com.chat.e2e.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository repo;
    private final BCryptPasswordEncoder passwordEncoder;

    public List<AppUser> findAll() {
        return repo.findAll();
    }

    public Optional<AppUser> findByHandle(String handle) {
        return repo.findByHandle(handle);
    }

    public AppUser createUser(String handle, String displayName) {
        AppUser user = AppUser.builder()
                .handle(handle)
                .displayName(displayName)
                .build();
        return repo.save(user);
    }

    public AppUser register(String handle, String displayName, String rawPassword) {
        String hash = passwordEncoder.encode(rawPassword);

        AppUser user = AppUser.builder()
                .handle(handle)
                .displayName(displayName)
                .passwordHash(hash)
                .build();

        return repo.save(user);
    }

    public boolean verifyPassword(AppUser user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
