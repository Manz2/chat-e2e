package com.chat.e2e.backend.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")              // wss://…/ws
                .setAllowedOriginPatterns("*");  // TODO: CORS sauber setzen (Domains whitelisten)
        // registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); // optional Fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");   // Client -> Server
        registry.enableSimpleBroker("/topic", "/queue");      // Server -> Client (dev)
        registry.setUserDestinationPrefix("/user");           // /user/queue/… für convertAndSendToUser
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);     // <-- HIER registriert
    }
}
