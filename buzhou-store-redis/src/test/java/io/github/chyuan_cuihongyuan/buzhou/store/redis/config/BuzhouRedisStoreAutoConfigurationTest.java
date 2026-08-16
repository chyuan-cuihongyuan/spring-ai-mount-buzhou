package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis store 装配测试（ticket 22）：store.type=redis + 进程内 jedis-mock（hermetic，无 Docker）。
 */
class BuzhouRedisStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouRedisStoreAutoConfiguration.class));

    @Test
    void redisStoreAssemblesOnRedisType() throws Exception {
        RedisServer server = new RedisServer(0);
        server.start();
        try {
            String uri = "redis://" + server.getHost() + ":" + server.getBindPort();
            runner.withPropertyValues("buzhou.store.type=redis", "buzhou.store.redis.uri=" + uri)
                    .run(ctx -> {
                        assertThat(ctx).hasSingleBean(BuzhouStores.class);
                        assertThat(ctx.getBean(BuzhouStores.class).messageStore().getClass().getSimpleName())
                                .contains("Redis");
                    });
        } finally {
            server.stop();
        }
    }

    @Test
    void notActiveForMemoryType() {
        runner.withPropertyValues("buzhou.store.type=memory")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(BuzhouStores.class));
    }

    /** spec 54 §B / T224：store.type=redis 即供共享限流后端 bean；容量 env 直读同源。 */
    @Test
    void sharedRateLimitBackendAssemblesWithCapacityFromEnv() throws Exception {
        RedisServer server = new RedisServer(0);
        server.start();
        try {
            String uri = "redis://" + server.getHost() + ":" + server.getBindPort();
            runner.withPropertyValues(
                            "buzhou.store.type=redis",
                            "buzhou.store.redis.uri=" + uri,
                            "buzhou.resilience.rate-limit.requests-per-minute=7",
                            "buzhou.resilience.rate-limit.tokens-per-minute=900")
                    .run(ctx -> {
                        assertThat(ctx).hasSingleBean(
                                io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend.class);
                        var backend = ctx.getBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend.class);
                        assertThat(backend.kind()).isEqualTo("redis");
                        assertThat(backend.capacity("RPM")).isEqualTo(7);
                        assertThat(backend.capacity("TPM")).isEqualTo(900);
                    });
            // 未配容量 = 不供 bean（不白开 Lettuce 独占连接；resilience 走内存令牌桶默认）
            runner.withPropertyValues(
                            "buzhou.store.type=redis",
                            "buzhou.store.redis.uri=" + uri)
                    .run(ctx -> assertThat(ctx).doesNotHaveBean(
                            io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend.class));
        } finally {
            server.stop();
        }
    }

    @Test
    void transactionConnectionPoolAssemblesWithConfigurableMaxSize() throws Exception {
        RedisServer server = new RedisServer(0);
        server.start();
        try {
            String uri = "redis://" + server.getHost() + ":" + server.getBindPort();
            runner.withPropertyValues(
                            "buzhou.store.type=redis",
                            "buzhou.store.redis.uri=" + uri,
                            "buzhou.store.redis.pool-max-size=3")
                    .run(ctx -> {
                        assertThat(ctx).hasSingleBean(GenericObjectPool.class);
                        assertThat(ctx.getBean(GenericObjectPool.class).getMaxTotal()).isEqualTo(3);
                    });
        } finally {
            server.stop();
        }
    }

    @Test
    void observabilityStoreWrappedOnlyWhenDegradePolicyConfigured() throws Exception {
        RedisServer server = new RedisServer(0);
        server.start();
        try {
            String uri = "redis://" + server.getHost() + ":" + server.getBindPort();
            // DEGRADE：观测槽经降级装饰器包装（ticket 32）
            runner.withPropertyValues(
                            "buzhou.store.type=redis",
                            "buzhou.store.redis.uri=" + uri,
                            "buzhou.store.write-failure-policy=DEGRADE")
                    .run(ctx ->
                            assertThat(ctx.getBean(BuzhouStores.class).observabilityStore()
                                    .getClass().getSimpleName())
                                    .isEqualTo("DegradingObservabilityStore"));
            // 默认 FAIL_TURN：观测槽保持裸实现（既有原样抛语义）
            runner.withPropertyValues(
                            "buzhou.store.type=redis",
                            "buzhou.store.redis.uri=" + uri)
                    .run(ctx ->
                            assertThat(ctx.getBean(BuzhouStores.class).observabilityStore()
                                    .getClass().getSimpleName())
                                    .isEqualTo("RedisObservabilityStore"));
        } finally {
            server.stop();
        }
    }
}
