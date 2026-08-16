package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 54 §C / T225 / impl-186：多实例共享额度验证——两个 backend 实例（模拟双进程）
 * 共享同一 Redis：RPM 各扣各半、合并超容量即拒；TPM 记账跨实例累计；窗口滚动恢复；
 * Redis 不可达 fail-fast（不静默 fail-open）。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisRateLimitBackendContainersTest {

    @Container
    static final org.testcontainers.containers.GenericContainer<?> REDIS =
            new org.testcontainers.containers.GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> adminConn;
    private static RedisCommands<String, String> admin;

    private RedisRateLimitBackend instanceA;
    private RedisRateLimitBackend instanceB;

    @BeforeAll
    static void setUp() {
        client = RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        adminConn = client.connect();
        admin = adminConn.sync();
    }

    @AfterAll
    static void tearDown() {
        adminConn.close();
        client.shutdown();
    }

    @BeforeEach
    void freshInstances() {
        admin.flushdb();
        // 两实例独立连接 + 独立键前缀隔离（同一「部署逻辑闸」视角）
        instanceA = new RedisRateLimitBackend(client, "buzhou:rl:", 4, 100);
        instanceB = new RedisRateLimitBackend(client, "buzhou:rl:", 4, 100);
    }

    /** 共享 RPM：容量 4，A 扣 2、B 扣 2 → 合并满；第 5 次（任一实例）拒绝。 */
    @Test
    void sharedRpmQuotaAcrossInstances() {
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceB.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceB.tryAcquire("m", "RPM", 1)).isTrue();
        // 第 5 次：A 与 B 都被共享闸拒绝
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isFalse();
        assertThat(instanceB.tryAcquire("m", "RPM", 1)).isFalse();
        // 拒绝不泄漏额度（回滚）：available 仍 0，且下窗（键切换）恢复由滚动测试覆盖
        assertThat(instanceA.available("m", "RPM")).isZero();
        // 模型分桶：另一模型不受影响
        assertThat(instanceA.tryAcquire("other", "RPM", 1)).isTrue();
    }

    /** 共享 TPM 记账：A 记 60、B 记 40 → 合并 100 满；预检拒绝；B 单记超容量致负余额。 */
    @Test
    void sharedTpmAccountingAcrossInstances() {
        instanceA.consume("m", "TPM", 60);
        instanceB.consume("m", "TPM", 40);
        assertThat(instanceA.available("m", "TPM")).isZero();
        assertThat(instanceA.tryAcquire("m", "TPM", 0)).isFalse(); // 预检（桶空拒绝）
        instanceB.consume("m", "TPM", 30); // 超额记账（诚实负余额，下窗重置）
        assertThat(instanceA.available("m", "TPM")).isZero();
    }

    /** 窗口滚动：当前窗满 → 下一分钟窗全量恢复（固定窗语义钉住）。 */
    @Test
    void windowRollsOverToFullQuota() {
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceA.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(instanceB.tryAcquire("m", "RPM", 1)).isFalse();
        // 手动推进：删当前窗键模拟进入下一窗（真实滚动 = 时间推进，语义等价钉住）
        admin.keys("buzhou:rl:*").forEach(admin::del);
        assertThat(instanceB.tryAcquire("m", "RPM", 1)).isTrue();
    }

    /** 等待提示：固定窗语义给出下一窗剩余时间（0..60s + 微抖动）。 */
    @Test
    void waitHintWithinWindowBounds() {
        double wait = instanceA.secondsUntilAvailable("m", "RPM", 1);
        assertThat(wait).isGreaterThan(0).isLessThanOrEqualTo(61);
    }

    /** 故障语义：不可达 Redis fail-fast 带修法（不静默 fail-open）。 */
    @Test
    void unreachableRedisFailsFast() {
        try (RedisClient dead = RedisClient.create("redis://localhost:1/");
             RedisRateLimitBackend deadBackend = new RedisRateLimitBackend(dead, "buzhou:rl:", 4, 100)) {
            assertThatThrownBy(() -> deadBackend.tryAcquire("m", "RPM", 1))
                    .isInstanceOf(BuzhouException.class)
                    .hasMessageContaining("fail-fast");
        }
    }
}
