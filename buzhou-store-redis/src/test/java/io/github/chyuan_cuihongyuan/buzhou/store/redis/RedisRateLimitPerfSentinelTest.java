package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 54 §E / T227 / impl-188：Redis 限流哨兵（@Tag("perf")，nightly 以 -Dgroups=perf
 * 激活；container 环境口径——本机/CI 容器内 Redis 往返，跨机器绝对值不可比，只看趋势）。
 * 单次 tryAcquire = 1 次 INCRBY（命中）或 INCRBY+DECRBY+GET（超限回滚路径）——设计预期
 * 单数毫秒量级；哨兵按 10 倍宽幅硬顶（越顶 = 量级回归信号，人工 profiling，不调阈值了事）。
 */
@Tag("perf")
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimitPerfSentinelTest {

    /** 单次 tryAcquire P95 硬顶 ms（container 本地预期 <5ms 量级，10 倍宽幅）。 */
    private static final double SINGLE_P95_MAX_MILLIS = 50;

    /** 30 连发总耗硬顶 ms（含命中 + 超限回滚混合路径；预期 <150ms，10 倍宽幅）。 */
    private static final double BATCH30_MAX_MILLIS = 1500;

    @Container
    static final org.testcontainers.containers.GenericContainer<?> REDIS =
            new org.testcontainers.containers.GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static RedisClient client;
    private static RedisRateLimitBackend backend;

    @BeforeAll
    static void setUp() {
        client = RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        backend = new RedisRateLimitBackend(client, "buzhou:rl:perf:", 400, 1_000_000);
    }

    @AfterAll
    static void tearDown() {
        backend.close();
        client.shutdown();
    }

    /** 单次往返 P95：大容量下连续 100 次 tryAcquire（全命中路径）。 */
    @Test
    void singleTryAcquireRoundTripP95() {
        long[] nanos = new long[100];
        for (int i = 0; i < nanos.length; i++) {
            String model = "perf-" + ThreadLocalRandom.current().nextInt(8); // 分散窗口键
            long start = System.nanoTime();
            boolean admitted = backend.tryAcquire(model, "RPM", 1);
            nanos[i] = System.nanoTime() - start;
            assertThat(admitted).isTrue();
        }
        java.util.Arrays.sort(nanos);
        double p95 = percentile(nanos, 0.95) / 1e6;
        System.out.printf("[perf] redis tryAcquire single: p95=%.2fms (哨兵 p95 < %.0fms)%n",
                p95, SINGLE_P95_MAX_MILLIS);
        assertThat(p95).as("Redis 限流单次往返 10 倍级回归哨兵").isLessThan(SINGLE_P95_MAX_MILLIS);
    }

    /** 30 连发总耗：容量 6 之下 30 次抢（6 命中 + 24 超限回滚 INCRBY/DECRBY 混合路径）。 */
    @Test
    void batch30TotalWallTime() {
        RedisRateLimitBackend small = new RedisRateLimitBackend(client, "buzhou:rl:perf:small:",
                6, null);
        try {
            String model = "batch30-" + System.currentTimeMillis() / 60_000; // 固定当前窗
            long start = System.nanoTime();
            int admitted = 0;
            for (int i = 0; i < 30; i++) {
                if (small.tryAcquire(model, "RPM", 1)) {
                    admitted++;
                }
            }
            double totalMs = (System.nanoTime() - start) / 1e6;
            System.out.printf("[perf] redis tryAcquire batch30: total=%.2fms admitted=%d (哨兵 < %.0fms)%n",
                    totalMs, admitted, BATCH30_MAX_MILLIS);
            assertThat(admitted).isEqualTo(6);
            assertThat(totalMs).as("Redis 限流 30 连发总耗哨兵").isLessThan(BATCH30_MAX_MILLIS);
        } finally {
            small.close();
        }
    }

    private static long percentile(long[] sorted, double q) {
        int idx = (int) Math.min(sorted.length - 1, Math.round(q * (sorted.length - 1)));
        return sorted[idx];
    }
}
