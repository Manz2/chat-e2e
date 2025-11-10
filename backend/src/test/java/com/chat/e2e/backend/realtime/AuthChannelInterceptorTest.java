// com.chat.e2e.backend.realtime.AuthChannelInterceptorTest
package com.chat.e2e.backend.realtime;

import com.chat.e2e.backend.api.dto.DTOs;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthChannelInterceptorTest {

    @Test
    void setsPrincipalOnConnect() {
        // Arrange
        JwtVerifier verifier = mock(JwtVerifier.class);
        PrincipalFactory pf = new PrincipalFactory();

        var userId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();
        var claims = new DTOs.JwtClaims(userId, deviceId);

        // Fake-JWT (InsecureDevJwtVerifier wäre egal; wir mocken Verifier direkt)
        String token = "header.payload.sig";
        when(verifier.verify(token)).thenReturn(claims);

        var interceptor = new AuthChannelInterceptor(verifier, pf);

        // STOMP CONNECT Frame mit Native-Header "Authorization: Bearer <token>"
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setNativeHeader("Authorization", "Bearer " + token);
        acc.setLeaveMutable(true); // wichtig, damit setUser persistiert

        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        // Act
        Message<?> out = interceptor.preSend(msg, Mockito.mock(org.springframework.messaging.MessageChannel.class));

        // Assert
        StompHeaderAccessor outAcc = MessageHeaderAccessor.getAccessor(out, StompHeaderAccessor.class);
        assertThat(outAcc).isNotNull();
        assertThat(outAcc.getUser()).isNotNull();
        assertThat(outAcc.getUser().getName()).isEqualTo(userId.toString());
        assertThat(((UserDevicePrincipal) outAcc.getUser()).deviceId()).isEqualTo(deviceId);
    }

    @Test
    void missingAuthorizationHeaderThrows() {
        JwtVerifier verifier = mock(JwtVerifier.class);
        var interceptor = new AuthChannelInterceptor(verifier, new PrincipalFactory());

        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setLeaveMutable(true);
        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(msg, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing Authorization");
    }
}
