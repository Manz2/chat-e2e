package com.chat.e2e.backend.realtime;

import com.chat.e2e.backend.api.dto.DTOs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

//TODO Make Secure
@Component
class InsecureDevJwtVerifier implements JwtVerifier {

    private final ObjectMapper om = new ObjectMapper();

    // Optional: shared secret; NICHT verwendet hier (weil "insecure")
    public InsecureDevJwtVerifier(@Value("${auth.dev.acceptUnsigned:true}") boolean acceptUnsigned) {}

    @Override
    public DTOs.JwtClaims verify(String token) {
        try {
            // VERY INSECURE: nur zum Starten! (Header.Payload.Signature)
            String[] parts = token.split("\\.");
            if (parts.length < 2) throw new IllegalArgumentException("Malformed JWT");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String,Object> payload = om.readValue(payloadJson, Map.class);

            UUID userId = UUID.fromString((String) payload.get("sub"));
            UUID deviceId = UUID.fromString((String) payload.get("did"));
            return new DTOs.JwtClaims(userId, deviceId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
