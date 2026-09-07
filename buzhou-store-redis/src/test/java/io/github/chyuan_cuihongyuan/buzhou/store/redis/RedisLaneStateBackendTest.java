package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 共享泳道许可后端单测（spec 316 / impl-339）：Lua 原子取/还——cap 边界 /
 * release floor-0 / 跨实例合流 / reset 运维清零。
 */
class RedisLaneStateBackendTest {

    private static RedisServer server;
    private static io.lettuce.core.RedisClient client;
    private static RedisLaneStateBackend backend;
    private static RedisLaneStateBackend secondInstance;

    @BeforeAll
    static void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = io.lettuce.core.RedisClient
                .create("redis://" + server.getHost() + ":" + server.getBindPort());
        backend = new RedisLaneStateBackend(client, "buzhou:lane:");
        secondInstance = new RedisLaneStateBackend(client, "buzhou:lane:");
    }

    @AfterAll
    static void tearDown() throws IOException {
        backend.close();
        secondInstance.close();
        client.shutdown();
        server.stop();
    }

    @Test
    void acquireUpToCapThenReject() {
        backend.reset("slow-db");
        assertThat(backend.tryAcquire("slow-db", 2)).isTrue();
        assertThat(backend.tryAcquire("slow-db", 2)).isTrue();
        assertThat(backend.tryAcquire("slow-db", 2)).isFalse(); // 满
    }

    @Test
    void releaseRestoresPermit_floorZero() {
        backend.reset("r-lane");
        backend.release("r-lane"); // 多还不欠（floor-0）
        assertThat(backend.tryAcquire("r-lane", 1)).isTrue();
        backend.release("r-lane");
        assertThat(backend.tryAcquire("r-lane", 1)).isTrue();
    }

    @Test
    void permitsSharedAcrossInstances() {
        backend.reset("shared-lane");
        assertThat(backend.tryAcquire("shared-lane", 1)).isTrue();
        assertThat(secondInstance.tryAcquire("shared-lane", 1))
                .as("跨实例同一容量互斥").isFalse();
        secondInstance.release("shared-lane"); // 未持有也 floor-0 不炸
        backend.release("shared-lane");
        assertThat(secondInstance.tryAcquire("shared-lane", 1)).isTrue();
    }

    @Test
    void resetClearsLeaks() {
        backend.reset("leaky");
        assertThat(backend.tryAcquire("leaky", 1)).isTrue();
        // 模拟崩溃：不 release 直接 reset（运维回收）
        backend.reset("leaky");
        assertThat(backend.tryAcquire("leaky", 1)).isTrue();
    }
}
