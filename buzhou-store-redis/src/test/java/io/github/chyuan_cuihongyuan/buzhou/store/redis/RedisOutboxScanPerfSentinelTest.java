package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 58 §C / T261 / impl-202：outbox SCAN 哨兵（@Tag("perf")，nightly -Dgroups=perf；
 * container 环境口径，跨机器绝对值不可比，只看量级）。2k pending 量级下：
 * countByPrefix = 1 次 SMEMBERS（键集侧，零值读）；scanByPrefix = SMEMBERS + 一次
 * 流水线批量 HGETALL（N 次往返 → 1 次批量提交）。哨兵按宽幅硬顶（越顶 = 量级回归
 * 信号，人工 profiling，不调阈值了事）。
 */
@Tag("perf")
@Testcontainers(disabledWithoutDocker = true)
class RedisOutboxScanPerfSentinelTest {

    /** 2k pending 下 countByPrefix 硬顶 ms（预期 <50ms 量级，宽幅 20 倍）。 */
    private static final long COUNT_MAX_MILLIS = 1_000;

    /** 2k pending 下 scanByPrefix（含 2k 值读）硬顶 ms（预期 <500ms 量级，宽幅 10 倍）。 */
    private static final long SCAN_2K_MAX_MILLIS = 5_000;

    @Container
    static final org.testcontainers.containers.GenericContainer<?> REDIS =
            new org.testcontainers.containers.GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static final int PENDING = 2_000;

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> conn;
    private static RedisCommands<String, String> admin;
    private static RedisSync sync;
    private static RedisSessionStateStore store;

    @BeforeAll
    static void setUp() {
        client = RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        conn = client.connect();
        admin = conn.sync();
        sync = new RedisSync(conn);
        store = new RedisSessionStateStore(sync, new RedisKeys("buzhou:perf:"));
        for (int i = 0; i < PENDING; i++) {
            store.put("__perf__", new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                    "outbox.e" + i, "{\"seq\":" + i + "}", "webhook-outbox", 0, null, Instant.now()));
        }
        // 干扰项：同会话其他前缀键（计数不得误计）
        for (int i = 0; i < 100; i++) {
            store.put("__perf__", new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                    "dead.e" + i, "x", "webhook-outbox", 0, null, Instant.now()));
        }
    }

    @AfterAll
    static void tearDown() {
        admin.flushdb();
        conn.close();
        client.shutdown();
    }

    @Test
    void countByPrefixStaysKeySide() {
        long start = System.nanoTime();
        int count = store.countByPrefix("__perf__", "outbox.");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(count).isEqualTo(PENDING);
        assertThat(elapsedMs).as("countByPrefix 2k pending 耗时（键集侧，预期 <50ms）").isLessThan(COUNT_MAX_MILLIS);
    }

    @Test
    void scanByPrefixStaysPipelined() {
        long start = System.nanoTime();
        int size = store.scanByPrefix("__perf__", "outbox.").size();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(size).isEqualTo(PENDING);
        assertThat(elapsedMs).as("scanByPrefix 2k pending 耗时（流水线批量值读，预期 <500ms）")
                .isLessThan(SCAN_2K_MAX_MILLIS);
    }
}
