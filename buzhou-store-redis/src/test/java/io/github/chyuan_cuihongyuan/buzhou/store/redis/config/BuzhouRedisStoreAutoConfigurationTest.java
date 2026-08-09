package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
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
}
