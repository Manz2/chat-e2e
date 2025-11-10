package com.chat.e2e.backend.realtime;

import com.chat.e2e.backend.api.dto.DTOs;

public interface JwtVerifier {
    DTOs.JwtClaims verify(String bearerToken);
}