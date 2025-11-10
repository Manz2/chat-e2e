package com.chat.e2e.backend.realtime;

import com.chat.e2e.backend.api.dto.DTOs;

@org.springframework.stereotype.Component
class PrincipalFactory {
    public UserDevicePrincipal fromClaims(DTOs.JwtClaims c) {
        return new UserDevicePrincipal(c.userId(), c.deviceId());
    }
}
