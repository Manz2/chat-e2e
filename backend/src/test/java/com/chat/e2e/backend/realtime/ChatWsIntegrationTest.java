package com.chat.e2e.backend.realtime;

import com.chat.e2e.backend.BackendApplication;
import com.chat.e2e.backend.api.PublicDeviceController;
import com.chat.e2e.backend.api.dto.DTOs;
import com.chat.e2e.backend.chat.ConversationService;
import com.chat.e2e.backend.chat.MessageService;
import com.chat.e2e.backend.device.DeviceEnrollmentService;
import com.chat.e2e.backend.user.AppUserRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none"
        }
)
@ActiveProfiles("test")
class ChatWsIntegrationTest {

    @LocalServerPort
    int port;

    @MockBean MessageService messageService;
    @MockBean AuthChannelInterceptor authChannelInterceptor;
    @MockBean AppUserRepository appUserRepository;
    @MockBean ConversationService conversationService;
    @MockBean DeviceEnrollmentService deviceEnrollmentService;
    @MockBean
    PublicDeviceController publicDeviceController;

    WebSocketStompClient stomp;

    static final UUID TEST_USER   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID TEST_DEVICE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @TestConfiguration
    static class StubJwtConfig {
        @Bean
        JwtVerifier jwtVerifier() {
            return token -> {
                if (!"good".equals(token)) throw new IllegalArgumentException("bad token");
                return new DTOs.JwtClaims(TEST_USER, TEST_DEVICE);
            };
        }
    }

    @BeforeEach
    void init() {
        stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        stomp.setTaskScheduler(new ConcurrentTaskScheduler());
        stomp.setDefaultHeartbeat(new long[]{10000, 10000});

        // Mock: fügt Principal in jede STOMP-Message ein
        Mockito.lenient().when(authChannelInterceptor.preSend(any(), any()))
                .thenAnswer(inv -> {
                    var msg = (org.springframework.messaging.Message<?>) inv.getArgument(0);
                    var accessor = org.springframework.messaging.simp.SimpMessageHeaderAccessor.wrap(msg);

                    // Authorization Header aus Native-Headern lesen
                    String auth = accessor.getFirstNativeHeader("Authorization");
                    if (auth == null) {
                        return msg; // kein Auth-Header → einfach durchreichen oder ggf. null zurückgeben
                    }

                    if (!"Bearer good".equals(auth)) {
                        return null; // simuliert Abweisung
                    }

                    // Wenn kein Principal gesetzt ist, fügen wir ihn hinzu
                    if (accessor.getUser() == null) {
                        accessor.setUser(new UserDevicePrincipal(TEST_USER, TEST_DEVICE));
                    }

                    // Neue Message mit aktualisierten Headers zurückgeben
                    return org.springframework.messaging.support.MessageBuilder
                            .withPayload(msg.getPayload())
                            .copyHeaders(accessor.toMessageHeaders())
                            .build();
                });
    }



    @AfterEach
    void shutdown() {
        if (stomp != null) stomp.stop();
    }

    private StompSession connect() throws Exception {
        String url = "ws://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer good");

        CountDownLatch connected = new CountDownLatch(1);

        ListenableFuture<StompSession> f = stomp.connect(url, new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connected.countDown();
            }
        }, connectHeaders, new Object[0]);

        StompSession session = f.get(5, TimeUnit.SECONDS);
        assertThat(connected.await(3, TimeUnit.SECONDS)).as("connected").isTrue();
        return session;
    }

    private void subscribeQuiet(StompSession session, String destination, StompFrameHandler handler) throws InterruptedException {
        session.subscribe(destination, handler);
        // kurze Wartezeit, damit die Subscription serverseitig registriert ist
        TimeUnit.MILLISECONDS.sleep(300);
    }


    private void sendQuiet(StompSession session, String destination, Object payload, String authHeader) {
        StompHeaders h = new StompHeaders();
        h.setDestination(destination);
        if (authHeader != null) h.add("Authorization", authHeader);
        session.send(h, payload);
    }

    @Test
    void sendMessage_yieldsSendAck_onUserQueue() throws Exception {
        UUID convId = UUID.randomUUID();
        UUID msgId  = UUID.randomUUID();

        DTOs.SendMessageResponse svcResp =
                new DTOs.SendMessageResponse(msgId, Instant.parse("2025-11-06T00:00:00Z"), 3);
        Mockito.when(messageService.send(eq(convId), eq(TEST_USER), eq(TEST_DEVICE), any(DTOs.SendMessageRequest.class)))
                .thenReturn(svcResp);

        StompSession session = connect();
        BlockingQueue<DTOs.SendAckEvent> acks = new ArrayBlockingQueue<>(1);

        // vollständige Handler-Implementierung
        subscribeQuiet(session, "/user/queue/device", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) {
                return DTOs.SendAckEvent.class;
            }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                acks.offer((DTOs.SendAckEvent) payload);
            }
        });

        DTOs.SendWsMessage wsMsg = new DTOs.SendWsMessage(
                convId, "text/plain", 1, 42L, Base64.getEncoder().encodeToString("cipher".getBytes())
        );
        sendQuiet(session, "/app/messages.send", wsMsg, "Bearer good");

        DTOs.SendAckEvent ack = acks.poll(5, TimeUnit.SECONDS);
        assertThat(ack).isNotNull();
        assertThat(ack.conversationId()).isEqualTo(convId);
        assertThat(ack.messageId()).isEqualTo(msgId);
        assertThat(ack.deliveries()).isEqualTo(3);
    }

    @Test
    void read_sendsReadEvent_onTopic() throws Exception {
        StompSession session = connect();
        UUID convId = UUID.randomUUID();
        UUID msgId  = UUID.randomUUID();

        Mockito.doNothing().when(messageService).markRead(eq(TEST_DEVICE), eq(msgId));

        BlockingQueue<DTOs.ReadEvent> events = new ArrayBlockingQueue<>(1);

        subscribeQuiet(session, "/topic/conversation." + convId, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) {
                return DTOs.ReadEvent.class;
            }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                events.offer((DTOs.ReadEvent) payload);
            }
        });

        DTOs.ReadWsMessage read = new DTOs.ReadWsMessage(convId, msgId);
        sendQuiet(session, "/app/messages.read", read, "Bearer good");

        DTOs.ReadEvent ev = events.poll(5, TimeUnit.SECONDS);
        assertThat(ev).isNotNull();
        assertThat(ev.conversationId()).isEqualTo(convId);
        assertThat(ev.messageId()).isEqualTo(msgId);
        assertThat(ev.byDeviceId()).isEqualTo(TEST_DEVICE);
    }
}
