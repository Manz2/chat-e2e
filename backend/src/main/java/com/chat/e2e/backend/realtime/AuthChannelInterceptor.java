package com.chat.e2e.backend.realtime;

import com.chat.e2e.backend.api.dto.DTOs;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtVerifier jwtVerifier;
    private final PrincipalFactory principalFactory;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null) return message;

        if (StompCommand.CONNECT.equals(acc.getCommand())) {
            String auth = firstNonNull(
                    acc.getFirstNativeHeader("Authorization"),
                    acc.getFirstNativeHeader("authorization")
            );

            if (auth == null || !auth.toLowerCase().startsWith("bearer ")) {
                throw new IllegalArgumentException("Missing Authorization: Bearer <jwt> in STOMP CONNECT");
            }
            String token = auth.substring(7).trim();

            DTOs.JwtClaims claims = jwtVerifier.verify(token);
            var principal = principalFactory.fromClaims(claims);

            // wichtig: Principal setzen + Header mutierbar lassen
            acc.setUser(principal);
            acc.setLeaveMutable(true);
        }

        return message;
    }

    private static String firstNonNull(String a, String b) { return a != null ? a : b; }
}
