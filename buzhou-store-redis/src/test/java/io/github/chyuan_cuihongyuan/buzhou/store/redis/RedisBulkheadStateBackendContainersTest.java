package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 127 / T473：Redis 舱占用心跳后端容器测试——双实例求和 / 同实例覆盖 /
 * 过期剔除 / 后端不可达降级空快照。redis:7-alpine（无 Docker 跳过）。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisBulkheadStateBackendContainersTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static RedisClient client;

    @BeforeAll
    static void connect() {
        client = RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @AfterAll
    static void disconnect() {
        if (client != null) {
            client.shutdown();
        }
    }

    private BulkheadStateBackend.InstanceOccupancy beat(String instance, String agent, int occ) {
        return new BulkheadStateBackend.InstanceOccupancy(instance, agent, occ, 5, Instant.now());
    }

    @Test
    void aggregatesAcrossInstancesAndAgents() {
        try (RedisBulkheadStateBackend backend = new RedisBulkheadStateBackend(client, "t1:")) {
            backend.heartbeat(beat("i-1", "alpha", 2), Duration.ofSeconds(30));
            backend.heartbeat(beat("i-2", "alpha", 3), Duration.ofSeconds(30));
            backend.heartbeat(beat("i-1", "beta", 1), Duration.ofSeconds(30));

            assertThat(backend.clusterOccupancy())
                    .containsEntry("alpha", 5).containsEntry("beta", 1);
            assertThat(backend.clusterInstances())
                    .containsEntry("alpha", 2).containsEntry("beta", 1);
        }
    }

    @Test
    void sameInstanceOverwritesAndExpiryDrops() throws InterruptedException {
        try (RedisBulkheadStateBackend backend = new RedisBulkheadStateBackend(client, "t2:")) {
            backend.heartbeat(beat("i-1", "alpha", 4), Duration.ofSeconds(30));
            backend.heartbeat(beat("i-1", "alpha", 1), Duration.ofSeconds(30));
            assertThat(backend.clusterOccupancy()).containsEntry("alpha", 1);

            backend.heartbeat(beat("i-1", "stale", 2), Duration.ofMillis(150));
            Thread.sleep(250);
            assertThat(backend.clusterOccupancy()).doesNotContainKey("stale");
        }
    }

    @Test
    void unreachableBackendDegradesToEmptySnapshot() {
        RedisClient deadClient = RedisClient.create("redis://127.0.0.1:1/");
        try (RedisBulkheadStateBackend backend = new RedisBulkheadStateBackend(deadClient, "t3:")) {
            backend.heartbeat(beat("i-1", "alpha", 1), Duration.ofSeconds(30)); // 静默降级
            assertThat(backend.clusterOccupancy()).isEmpty();
            assertThat(backend.clusterInstances()).isEmpty();
        } finally {
            deadClient.shutdown();
        }
    }
}
