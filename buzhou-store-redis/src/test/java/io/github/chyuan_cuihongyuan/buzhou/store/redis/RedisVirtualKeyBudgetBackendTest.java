package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 共享虚拟 key 配额后端单测（spec 315 / impl-338）：Lua 原子扣减——
 * 限额内提交/越限拒绝/清零重置/跨实例（两个 backend 同键）计数合流。
 * jedismock 不支持 EVAL 时整类跳过（真 Redis 语义归 CI Testcontainers——诚实边界）。
 */
class RedisVirtualKeyBudgetBackendTest {

    private static RedisServer server;
    private static io.lettuce.core.RedisClient client;
    private static RedisVirtualKeyBudgetBackend backend;
    private static RedisVirtualKeyBudgetBackend secondInstance;
    private static boolean evalSupported = true;

    @BeforeAll
    static void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = io.lettuce.core.RedisClient
                .create("redis://" + server.getHost() + ":" + server.getBindPort());
        backend = new RedisVirtualKeyBudgetBackend(client, "buzhou:vk:");
        secondInstance = new RedisVirtualKeyBudgetBackend(client, "buzhou:vk:");
        try {
            backend.trySpend("probe", 1L, 10L);
            backend.reset("probe");
        } catch (RuntimeException e) {
            evalSupported = false; // jedismock 无 EVAL——跳过整类
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (evalSupported) {
            backend.close();
            secondInstance.close();
        }
        client.shutdown();
        server.stop();
    }

    @Test
    void spendWithinLimitCommits_beyondRejected() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        backend.reset("k1");

        assertThat(backend.trySpend("k1", 60L, 100L)).isTrue();
        assertThat(backend.usedTokens("k1")).isEqualTo(60L);
        assertThat(backend.trySpend("k1", 60L, 100L)).isFalse(); // 120 > 100
        assertThat(backend.usedTokens("k1")).isEqualTo(60L); // 拒绝不留痕
    }

    @Test
    void resetRestoresQuota() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        backend.reset("k2");
        assertThat(backend.trySpend("k2", 100L, 100L)).isTrue();
        assertThat(backend.trySpend("k2", 1L, 100L)).isFalse();

        backend.reset("k2");
        assertThat(backend.usedTokens("k2")).isZero();
        assertThat(backend.trySpend("k2", 100L, 100L)).isTrue();
    }

    @Test
    void countersSharedAcrossInstances() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        backend.reset("k3");
        assertThat(backend.trySpend("k3", 50L, 100L)).isTrue();
        // 另一实例同键续扣——合流计数（多实例共享额度的本义）
        assertThat(secondInstance.trySpend("k3", 50L, 100L)).isTrue();
        assertThat(secondInstance.trySpend("k3", 1L, 100L)).isFalse();
        assertThat(secondInstance.usedTokens("k3")).isEqualTo(100L);
    }
}
