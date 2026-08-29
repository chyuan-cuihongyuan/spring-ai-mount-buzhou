package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.fppt.jedismock.RedisServer;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 98 §B / T366：Redis 键序区间覆写红队（jedis-mock 内嵌——契约三栈的 Redis 侧
 * 单测补位）：键序升序 + 排他上界 + limit 截断 + 命中键批量取值（免全量值读）。
 */
class RedisScanByKeyRangeTest {

    private RedisServer server;
    private io.lettuce.core.RedisClient client;
    private io.lettuce.core.api.StatefulRedisConnection<String, String> conn;
    private RedisSessionStateStore store;
    private static final AtomicInteger PORT = new AtomicInteger(16390);

    @BeforeEach
    void setUp() throws IOException {
        server = new RedisServer(PORT.incrementAndGet());
        server.start();
        client = io.lettuce.core.RedisClient.create(
                "redis://127.0.0.1:" + server.getBindPort());
        conn = client.connect();
        store = new RedisSessionStateStore(new RedisSync(conn), new RedisKeys("buzhou"));
    }

    @AfterEach
    void tearDown() throws IOException {
        conn.close();
        client.shutdown();
        server.stop();
    }

    private static StateEntry entry(String key, String value) {
        return new StateEntry(key, value, "idx", 0, null, Instant.now());
    }

    @Test
    void keyRangeOrdersBoundsAndLimits() {
        String sid = "redis-range-1";
        store.put(sid, entry("d.0000000000000005.e1", "v1"));
        store.put(sid, entry("d.0000000000000010.e2", "v2"));
        store.put(sid, entry("d.0000000000000020.e3", "v3"));
        store.put(sid, entry("dead.e1", "other"));

        // 排他上界
        assertThat(store.scanByKeyRange(sid, "d.", null, "d.0000000000000010", 10).keySet())
                .containsExactly("d.0000000000000005.e1");
        // 含界下界 + 键序
        assertThat(store.scanByKeyRange(sid, "d.", "d.0000000000000010.e2", null, 10).keySet())
                .containsExactly("d.0000000000000010.e2", "d.0000000000000020.e3");
        // limit 截断 + 值回读正确（批量 HGETALL 只打命中键）
        var limited = store.scanByKeyRange(sid, "d.", null, null, 1);
        assertThat(limited.keySet()).containsExactly("d.0000000000000005.e1");
        assertThat(limited.get("d.0000000000000005.e1").value()).isEqualTo("v1");
        // 非法 limit
        assertThat(store.scanByKeyRange(sid, "d.", null, null, 0)).isEmpty();
    }
}
