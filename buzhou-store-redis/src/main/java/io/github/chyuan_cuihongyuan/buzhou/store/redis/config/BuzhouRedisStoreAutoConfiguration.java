package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.store.redis.RedisBuzhouStores;
import io.lettuce.core.RedisClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Redis store 自装配（spec 08 / 09 / ticket 22）。
 *
 * <p>当 {@code buzhou.store.type=redis} 时按 {@code buzhou.store.redis.uri} 自建
 * {@link RedisClient}（生命周期由容器管理，销毁时 shutdown），再由
 * {@link RedisBuzhouStores#create} 产出 {@link BuzhouStores}，替换 core 的内存默认。
 */
@AutoConfiguration
@ConditionalOnClass(RedisClient.class)
@ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "redis")
@EnableConfigurationProperties(RedisStoreProperties.class)
public class BuzhouRedisStoreAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedisClient buzhouRedisClient(RedisStoreProperties props) {
        return RedisClient.create(props.uri());
    }

    @Bean
    @ConditionalOnMissingBean
    public BuzhouStores buzhouStores(RedisClient client, RedisStoreProperties props) {
        return RedisBuzhouStores.create(client, props.keyPrefix(), props.snapshotTtl());
    }
}
