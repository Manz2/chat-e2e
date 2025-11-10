package com.chat.e2e.backend.api.dto;

import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class AppUserController {

    private final AppUserService service;

    @GetMapping
    @Operation(summary = "List all users")
    public List<UserResponse> getAll() {
        return service.findAll().stream().map(UserResponse::from).toList();
    }

    public record UserResponse(UUID id, String handle, String displayName, boolean twoFaEnabled, Instant createdAt) {
        static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getHandle(), u.getDisplayName(), u.isTwoFaEnabled(), u.getCreatedAt());
        }
    }

    @GetMapping("/{handle}")
    @Operation(summary = "Get user by handle")
    public ResponseEntity<AppUser> getByHandle(@PathVariable String handle) {
        return service.findByHandle(handle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping("/register")
    public ResponseEntity<AppUser> register(@RequestBody DTOs.RegisterUserRequest req) {
        AppUser user = service.register(req.handle(), req.displayName(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

}

