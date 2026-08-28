package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.CircuitBreakerStateBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.fppt.jedismock.RedisServer;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 共享熔断闸后端单测（spec 57 §B / T256）：TTL 键语义——recordTrip 存活 /
 * activeTrip 带剩余冷却与 trips / 过期即空（免清理）/ clear 幂等 / 异常态（无 TTL 残键）
 * 按可探测处理（不永久锁死）。
 */
class RedisCircuitBreakerStateBackendTest {

    private static RedisServer server;
    private static io.lettuce.core.RedisClient client;
    private static io.lettuce.core.api.StatefulRedisConnection<String, String> adminConn;
    private static io.lettuce.core.api.sync.RedisCommands<String, String> admin;
    private static RedisCircuitBreakerStateBackend backend;

    @BeforeAll
    static void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = io.lettuce.core.RedisClient.create("redis://" + server.getHost() + ":" + server.getBindPort());
        adminConn = client.connect();
        admin = adminConn.sync();
        backend = new RedisCircuitBreakerStateBackend(client, "buzhou:cb:");
    }

    @AfterAll
    static void tearDown() throws IOException {
        backend.close();
        adminConn.close();
        client.shutdown();
        server.stop();
    }

    @BeforeEach
    void flushBetweenTests() {
        admin.flushdb();
    }

    @Test
    void recordTripIsVisibleWithRemainingCooldownAndTrips() {
        backend.recordTrip("gpt-4o", Instant.now(), 5_000, 3);

        assertThat(backend.activeTrip("gpt-4o")).isPresent();
        CircuitBreakerStateBackend.TripMarker trip = backend.activeTrip("gpt-4o").orElseThrow();
        assertThat(trip.cooldownMs()).isBetween(1L, 5_000L); // 剩余冷却
        assertThat(trip.consecutiveTrips()).isEqualTo(3);
        assertThat(trip.openedAt()).isBeforeOrEqualTo(Instant.now());

        assertThat(backend.activeTrip("other-model")).isEmpty(); // 模型隔离
    }

    @Test
    void expiredKeyMeansProbingAllowedWithoutCleanup() throws InterruptedException {
        backend.recordTrip("m", Instant.now(), 50, 1); // 50ms 冷却
        assertThat(backend.activeTrip("m")).isPresent();
        Thread.sleep(120); // TTL 自然过期
        assertThat(backend.activeTrip("m")).isEmpty(); // 过期 = 可探测，无需清理任务
    }

    @Test
    void clearIsIdempotent() {
        backend.recordTrip("m", Instant.now(), 60_000, 1);
        backend.clear("m");
        backend.clear("m"); // 幂等
        assertThat(backend.activeTrip("m")).isEmpty();
    }

    @Test
    void noTtlResidueKeyTreatedAsProbingAllowed() {
        // 异常态注入：键存而无 TTL（运维手工 SET 等）——按可探测处理，不永久锁死
        admin.set("buzhou:cb:m", "5@" + Instant.now().toEpochMilli());
        assertThat(backend.activeTrip("m")).isEmpty();
    }

    @Test
    void modelNameSanitizedIntoKeyAndKind() {
        backend.recordTrip("claude/sonnet:4.5!", Instant.now(), 1_000, 1);
        assertThat(admin.exists("buzhou:cb:claude_sonnet_4.5_")).isEqualTo(1L);
        assertThat(backend.kind()).isEqualTo("redis");
    }
}
