package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ResilienceModule;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimitExceededException;
import io.github.chyuan_cuihongyuan.buzhou.store.redis.RedisRateLimitBackend;
import io.lettuce.core.RedisClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * effort#14 多实例共享限流演示（spec 54 §E / T228 / impl-189；container 依赖标注：
 * CI Docker 可用时跑，本机无 Docker 自动跳过）——宿主视角「双实例协同样例」：
 * 两个独立 runtime（模拟双进程部署，各自持有独立 Redis 连接/后端）共享同一 Redis
 * 固定窗：实例 A 耗尽的 RPM 额度，实例 B 的下一次调用立即感知（共享总闸）；
 * 对照组（内存后端，默认单进程形态）实例 B 不受 A 影响——差异即共享语义本体。
 *
 * <p>生产等价配置：两个进程都设 {@code buzhou.store.type=redis} +
 * {@code buzhou.resilience.rate-limit.requests-per-minute=2} +
 * {@code ...overload-policy=FAIL_FAST}——starter 自动装配 Redis 共享后端（spec 54 §B）。
 */
@Testcontainers(disabledWithoutDocker = true)
class SharedRateLimitTwoInstanceDemoTest {

    @Container
    static final org.testcontainers.containers.GenericContainer<?> REDIS =
            new org.testcontainers.containers.GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static RedisClient client;
    private static final String SHARED_MODEL = "shared-gpt";

    @BeforeAll
    static void setUp() {
        client = RedisClient.create("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @AfterAll
    static void tearDown() {
        client.shutdown();
    }

    @Test
    void secondInstancePerceivesSharedQuotaExhaustedByFirst() {
        // 实例 A 与 B：独立 runtime + 独立后端连接，共享同一 Redis（模拟双进程部署）
        ScriptedChatModel modelA = new ScriptedChatModel();
        modelA.enqueueText("A-回答-1");
        modelA.enqueueText("A-回答-2");
        ScriptedChatModel modelB = new ScriptedChatModel();
        modelB.enqueueText("B-回答");

        AgentRuntime instanceA = runtimeWithRedisBackend(modelA);
        AgentRuntime instanceB = runtimeWithRedisBackend(modelB);

        // 实例 A 消耗全部共享额度（rpm=2：两次调用放行）
        try (var sessionA = instanceA.spawn("app", "agent", "user-a")) {
            assertThat(sessionA.chat("你好")).isEqualTo("A-回答-1");
            assertThat(sessionA.chat("你好")).isEqualTo("A-回答-2");
        }

        // 实例 B 的首次调用即被共享闸拒绝（额度是 A+B 总量，不是各 2）——
        // 多实例部署总闸正确：不会 N 倍超额打爆 provider 配额
        assertThatThrownBy(() -> {
            try (var sessionB = instanceB.spawn("app", "agent", "user-b")) {
                sessionB.chat("你好");
            }
        }).isInstanceOf(ModelRateLimitExceededException.class)
                .hasMessageContaining("RPM");
        // 拒绝不放大：B 的模型零调用
        assertThat(modelB.seenPrompts).isEmpty();

        // 对照组：默认内存后端（单进程形态）——B 有自己的桶，不受 A 影响
        AgentRuntime singleProcessB = Buzhou.runtime(modelB, Buzhou.inMemoryStores(),
                ResilienceModule.configure(rateLimitProps(), SHARED_MODEL, new ResilienceStats(),
                        null, null));
        try (var sessionB2 = singleProcessB.spawn("app", "agent", "user-b2")) {
            assertThat(sessionB2.chat("你好")).isEqualTo("B-回答");
        }
    }

    /** 双实例共享后端装配：runtime + 独立 RedisRateLimitBackend（同 Redis、同前缀、同容量）。 */
    private static AgentRuntime runtimeWithRedisBackend(ScriptedChatModel model) {
        RedisRateLimitBackend backend = new RedisRateLimitBackend(client, "buzhou:rl:", 2, null);
        return Buzhou.runtime(model, Buzhou.inMemoryStores(),
                ResilienceModule.configure(rateLimitProps(), SHARED_MODEL, new ResilienceStats(),
                        null, null, backend));
    }

    /** rpm=2 / FAIL_FAST / 短排队超时（拒绝立即可观察）。 */
    private static ResilienceProperties rateLimitProps() {
        return new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                new ResilienceProperties.RateLimit(2, null, Duration.ofMillis(200), "FAIL_FAST"));
    }
}
