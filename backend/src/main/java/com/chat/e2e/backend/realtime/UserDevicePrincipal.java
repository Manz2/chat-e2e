package com.chat.e2e.backend.realtime;

import java.security.Principal;
import java.util.UUID;

public class UserDevicePrincipal implements Principal {
    private final UUID userId;
    private final UUID deviceId;
    public UserDevicePrincipal(UUID userId, UUID deviceId) {
        this.userId = userId; this.deviceId = deviceId;
    }
    @Override public String getName() { return userId.toString(); }
    public UUID userId() { return userId; }
    public UUID deviceId() { return deviceId; }
}
