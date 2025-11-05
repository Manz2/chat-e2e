package com.chat.e2e.backend.device;

import com.chat.e2e.backend.user.AppUser;
import com.chat.e2e.backend.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static com.chat.e2e.backend.keys.KeyCurve.x25519;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserDeviceRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Flyway + nur Validierung (Schema kommt aus V1__init.sql)
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired AppUserRepository userRepo;
    @Autowired UserDeviceRepository deviceRepo;

    private UserDevice baseDevice(AppUser u, String platform) {
        return UserDevice.builder()
                .user(u)
                .platform(platform)
                .deviceName(platform + "_dev")
                .publicIdentityKey("BASE64_IK_" + platform) // NOT NULL
                .keyCurve(x25519)
                .createdAt(Instant.now())
                // NICHT setzen: certPayload (jsonb) -> sonst Typkonflikt bei String
                // .certPayload(objectNode) nur wenn Entity-Feld als JsonNode/Map typisiert ist
                .build();
    }

    @Test
    void findByUserHandle_shouldFilterRevoked() {
        var u = userRepo.save(AppUser.builder()
                .handle("alice")
                .displayName("Alice")
                .passwordHash("$2a$10$x")
                .build());

        var active = deviceRepo.save(baseDevice(u, "android"));
        var revoked = baseDevice(u, "ios");
        revoked.setRevokedAt(Instant.now());
        deviceRepo.save(revoked);

        List<UserDevice> onlyActive = deviceRepo.findByUserHandle("alice", false);
        assertThat(onlyActive)
                .extracting(UserDevice::getId)
                .contains(active.getId())
                .doesNotContain(revoked.getId());

        List<UserDevice> all = deviceRepo.findByUserHandle("alice", true);
        assertThat(all)
                .extracting(UserDevice::getId)
                .contains(active.getId(), revoked.getId());
    }

    @Test
    void findByIdAndUser_Handle_shouldReturnOnlyOwnedDevice() {
        var u1 = userRepo.save(AppUser.builder().handle("bob").displayName("Bob").passwordHash("x").build());
        var u2 = userRepo.save(AppUser.builder().handle("eve").displayName("Eve").passwordHash("y").build());

        var d1 = deviceRepo.save(baseDevice(u1, "android"));
        deviceRepo.save(baseDevice(u2, "ios"));

        var found = deviceRepo.findByIdAndUser_Handle(d1.getId(), "bob");
        assertThat(found).isPresent();

        var notFound = deviceRepo.findByIdAndUser_Handle(d1.getId(), "eve");
        assertThat(notFound).isNotPresent();
    }
}
