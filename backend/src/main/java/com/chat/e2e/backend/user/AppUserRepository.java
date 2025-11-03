package com.chat.e2e.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByHandle(String handle);
    Optional<AppUser> findByDisplayName(String displayName);
}
