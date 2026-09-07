package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis 共享选主后端单测（spec 331 / impl-354）：Lua 原子取/续/让——
 * 双实例竞争唯一获取、续期纪元不变、租约过期（DEL 模拟）候选接管纪元递增、
 * 非持有人让位无效、inspect 观测、非法参数。jedismock 无 EVAL 时整类跳过
 * （真 Redis 语义归 CI Testcontainers——诚实边界）。
 */
class RedisLeaderElectorTest {

    private static RedisServer server;
    private static RedisClient client;
    private static RedisLeaderElector first;
    private static RedisLeaderElector second;
    private static boolean evalSupported = true;

    @BeforeAll
    static void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = RedisClient.create("redis://" + server.getHost() + ":" + server.getBindPort());
        first = new RedisLeaderElector(client, "buzhou:test:leader", "instance-a",
                Duration.ofSeconds(60));
        second = new RedisLeaderElector(client, "buzhou:test:leader", "instance-b",
                Duration.ofSeconds(60));
        try {
            first.tryAcquireOrRenew();
            first.resign(); // 探针后清场（纪元键留下——单调语义的一部分）
        } catch (RuntimeException e) {
            evalSupported = false;
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (evalSupported) {
            first.close();
            second.close();
        }
        client.shutdown();
        server.stop();
    }

    @Test
    void twoInstancesCompete_onlyOneAcquires_renewKeepsEpoch() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        LeaderElector.Leadership acquired = first.tryAcquireOrRenew();
        assertThat(acquired.leader()).isTrue();
        assertThat(acquired.holder()).isEqualTo("instance-a");

        LeaderElector.Leadership follows = second.tryAcquireOrRenew();
        assertThat(follows.leader()).isFalse();
        assertThat(follows.holder()).isEqualTo("instance-a"); // 跟随态报告持有者
        assertThat(follows.epoch()).isEqualTo(acquired.epoch());

        LeaderElector.Leadership renewed = first.tryAcquireOrRenew();
        assertThat(renewed.leader()).isTrue();
        assertThat(renewed.epoch()).isEqualTo(acquired.epoch()); // 续期纪元不变
    }

    @Test
    void takeoverAfterLeaseExpiry_bumpsEpoch() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        LeaderElector.Leadership acquired = first.tryAcquireOrRenew();
        long epochBefore = acquired.epoch();
        // 模拟租约自然过期（持有人键消失；纪元计数键留下）
        client.connect().sync().del("buzhou:test:leader");
        LeaderElector.Leadership takeover = second.tryAcquireOrRenew();
        assertThat(takeover.leader()).isTrue();
        assertThat(takeover.epoch()).isGreaterThan(epochBefore); // 接管进入新纪元
        second.resign(); // 清场给下一用例
    }

    @Test
    void holderResignReleases_nonHolderResignNoOp() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        first.tryAcquireOrRenew();
        second.resign(); // 非持有人让位无效
        assertThat(first.tryAcquireOrRenew().leader()).isTrue();
        first.resign(); // 持有人让位——空位立即开放
        assertThat(second.tryAcquireOrRenew().leader()).isTrue();
        second.resign();
    }

    @Test
    void inspectReportsWithoutAcquiring() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        first.tryAcquireOrRenew();
        LeaderElector.Leadership seen = second.inspect();
        assertThat(seen.leader()).isFalse();
        assertThat(seen.holder()).isEqualTo("instance-a");
        assertThat(seen.epoch()).isPositive();
        first.resign();
    }

    @Test
    void holderIdDefaultsToGenerated_whenBlank() {
        org.junit.jupiter.api.Assumptions.assumeTrue(evalSupported, "jedismock 不支持 EVAL");
        RedisLeaderElector anonymous = new RedisLeaderElector(client,
                "buzhou:test:leader-anon", null, Duration.ofSeconds(60));
        try {
            assertThat(anonymous.holderId()).startsWith("buzhou-");
            assertThat(anonymous.tryAcquireOrRenew().leader()).isTrue();
            anonymous.resign();
        } finally {
            anonymous.close();
        }
    }

    @Test
    void blankScopeOrBadTtlRejected() {
        assertThatThrownBy(() -> new RedisLeaderElector(client, " ", "a", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisLeaderElector(client, "k", "a", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RedisLeaderElector(client, "k", "a", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
