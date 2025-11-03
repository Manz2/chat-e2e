package com.chat.e2e.backend.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class AppUserController {

    private final AppUserService service;

    @GetMapping
    @Operation(summary = "List all users")
    public List<AppUser> getAll() {
        return service.findAll();
    }

    @GetMapping("/{handle}")
    @Operation(summary = "Get user by handle")
    public ResponseEntity<AppUser> getByHandle(@PathVariable String handle) {
        return service.findByHandle(handle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new user")
    public ResponseEntity<AppUser> create(@RequestBody CreateUserRequest req) {
        AppUser user = service.createUser(req.handle(), req.displayName());
        return ResponseEntity.ok(user);
    }
    public record CreateUserRequest(String handle, String displayName) {}


    @PostMapping("/register")
    public ResponseEntity<AppUser> register(@RequestBody RegisterUserRequest req) {
        AppUser user = service.register(req.handle(), req.displayName(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    public record RegisterUserRequest(String handle, String displayName, String password) {}
}
