package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.lettuce.core.RedisClient;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 125 / T472：共享 Redis 语义向量缓存容器测试——跨实例命中（A 写 B 读）/
 * 阈值下 miss / 惰性过期清除 / 容量驱逐最旧 / 后端不可达旁路 miss。
 * Testcontainers redis:7-alpine（先例 RedisSessionIndexContractTest；无 Docker 跳过）。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisSemanticVectorCacheContainersTest {

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

    private float[] vec(float... values) {
        return values;
    }

    @Test
    void crossInstancePutAndNearestHit() {
        try (RedisSemanticVectorCache writer = new RedisSemanticVectorCache(client, "t1:", 16, 0.9);
             RedisSemanticVectorCache reader = new RedisSemanticVectorCache(client, "t1:", 16, 0.9)) {
            writer.put("faq-bucket", "q1", vec(1f, 0f, 0f), "answer-1", Duration.ofSeconds(60));

            // 另一实例（独立连接）同义问法（轻微扰动仍 ≥ 0.9）命中共享条目
            Optional<RedisSemanticVectorCache.Hit> hit =
                    reader.findNearest("faq-bucket", vec(0.99f, 0.1f, 0f));
            assertThat(hit).isPresent();
            assertThat(hit.get().payload()).isEqualTo("answer-1");
            assertThat(hit.get().similarity()).isGreaterThanOrEqualTo(0.9);
        }
    }

    @Test
    void belowThresholdIsMiss() {
        try (RedisSemanticVectorCache cache = new RedisSemanticVectorCache(client, "t2:", 16, 0.95)) {
            cache.put("faq-bucket", "q1", vec(1f, 0f), "answer-1", Duration.ofSeconds(60));
            assertThat(cache.findNearest("faq-bucket", vec(0f, 1f))).isEmpty();
            assertThat(cache.findNearest("faq-bucket", vec(0.7f, 0.7f))).isEmpty();
        }
    }

    @Test
    void lazyExpiryRemovesStaleEntries() throws InterruptedException {
        try (RedisSemanticVectorCache cache = new RedisSemanticVectorCache(client, "t3:", 16, 0.9)) {
            cache.put("faq-bucket", "q1", vec(1f, 0f), "stale", Duration.ofMillis(150));
            Thread.sleep(250);
            assertThat(cache.findNearest("faq-bucket", vec(1f, 0f))).isEmpty();
            assertThat(cache.size("faq-bucket")).isZero();
        }
    }

    @Test
    void capacityEvictsOldestSeq() {
        try (RedisSemanticVectorCache cache = new RedisSemanticVectorCache(client, "t4:", 2, 0.9)) {
            cache.put("faq-bucket", "e1", vec(1f, 0f), "p1", Duration.ofSeconds(60));
            cache.put("faq-bucket", "e2", vec(0f, 1f), "p2", Duration.ofSeconds(60));
            cache.put("faq-bucket", "e3", vec(0f, 0f, 1f), "p3", Duration.ofSeconds(60));

            assertThat(cache.size("faq-bucket")).isEqualTo(2);
            // 最旧 e1 被驱逐；e3 可命中
            assertThat(cache.findNearest("faq-bucket", vec(1f, 0f))).isEmpty();
            Optional<RedisSemanticVectorCache.Hit> latest =
                    cache.findNearest("faq-bucket", vec(0f, 0f, 0.99f));
            assertThat(latest).isPresent();
            assertThat(latest.get().payload()).isEqualTo("p3");
        }
    }

    @Test
    void unreachableBackendBypassesAsMiss() {
        RedisClient deadClient = RedisClient.create("redis://127.0.0.1:1/");
        try (RedisSemanticVectorCache cache =
                     new RedisSemanticVectorCache(deadClient, "t5:", 16, 0.9)) {
            // 连接建立后服务器不可达（端口 1 无服务）——查询旁路 miss、写入静默放弃，均不抛
            assertThat(cache.findNearest("faq-bucket", vec(1f, 0f))).isEmpty();
            cache.put("faq-bucket", "q1", vec(1f, 0f), "p", Duration.ofSeconds(60));
            assertThat(cache.size("faq-bucket")).isEqualTo(-1);
        } finally {
            deadClient.shutdown();
        }
    }
}
