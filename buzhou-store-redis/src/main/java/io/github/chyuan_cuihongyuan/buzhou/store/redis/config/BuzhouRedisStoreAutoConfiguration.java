package io.github.chyuan_cuihongyuan.buzhou.store.redis.config;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.store.redis.RedisBuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.store.redis.WriteFailurePolicy;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Redis store 自装配（spec 08 / 09 / ticket 22 + spec 13 §stores-7 ticket 32）。
 *
 * <p>当 {@code buzhou.store.type=redis} 时按 {@code buzhou.store.redis.uri} 自建
 * {@link RedisClient}（生命周期由容器管理，销毁时 shutdown）；UoW 事务连接池
 * （{@code buzhou.store.redis.pool-max-size}，默认 8）注册为容器 bean
 * （{@code destroyMethod="close"}），由 {@link RedisBuzhouStores#createPooled}
 * 产出 {@link BuzhouStores}——{@code RedisUnitOfWork} 借池跑 MULTI/EXEC，
 * 事务连接复用不再每事务新建；写失败策略经 {@code buzhou.store.write-failure-policy}
 * （默认 FAIL_TURN）装饰观测槽。
 */
@AutoConfiguration
@ConditionalOnClass(RedisClient.class)
@ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "redis")
@EnableConfigurationProperties({RedisStoreProperties.class, WriteFailurePolicyProperties.class})
public class BuzhouRedisStoreAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedisClient buzhouRedisClient(RedisStoreProperties props) {
        return RedisClient.create(props.uri());
    }

    /** UoW 事务连接池（ticket 32）：上限 {@code buzhou.store.redis.pool-max-size}，容器关闭时释放。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public GenericObjectPool<StatefulRedisConnection<String, String>> buzhouRedisTransactionConnectionPool(
            RedisClient client, RedisStoreProperties props) {
        return RedisBuzhouStores.newConnectionPool(client, props.poolMaxSize());
    }

    /** 会话索引（spec 30 / T109 / impl-84）：ZSET+STRING；独立连接（写频低，不占事务池）。 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore buzhouSessionIndexStore(
            RedisClient client, RedisStoreProperties props) {
        return io.github.chyuan_cuihongyuan.buzhou.store.redis.RedisSessionIndexStore.create(
                client, props.keyPrefix());
    }

    /**
     * 共享限流后端（spec 54 §A / T224 / effort#14）：store.type=redis 且配置任一限流容量
     * （buzhou.resilience.rate-limit.requests-per-minute / tokens-per-minute）时供
     * RateLimitBackend bean（分钟固定窗，多实例共享额度）；容量 env 直读（与 resilience
     * 配置同源）；resilience auto-config 经 ObjectProvider 优先消费。
     * 未配容量时不供 bean（不白开 Lettuce 独占连接；内存令牌桶默认零变化）。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
            "#{environment['buzhou.resilience.rate-limit.requests-per-minute'] != null "
                    + "or environment['buzhou.resilience.rate-limit.tokens-per-minute'] != null}")
    public io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend buzhouSharedRateLimitBackend(
            RedisClient client, RedisStoreProperties props, org.springframework.core.env.Environment env) {
        Integer rpm = positiveOrNull(env.getProperty("buzhou.resilience.rate-limit.requests-per-minute"));
        Integer tpm = positiveOrNull(env.getProperty("buzhou.resilience.rate-limit.tokens-per-minute"));
        return new io.github.chyuan_cuihongyuan.buzhou.store.redis.RedisRateLimitBackend(
                client, props.keyPrefix() + "rl:", rpm, tpm);
    }

    private static Integer positiveOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? (int) Math.min(parsed, Integer.MAX_VALUE) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public BuzhouStores buzhouStores(RedisClient client, RedisStoreProperties props,
                                     GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool,
                                     WriteFailurePolicyProperties writePolicy) {
        WriteFailurePolicy policy = writePolicy.writeFailurePolicy();
        return RedisBuzhouStores.createPooled(client, props.keyPrefix(), props.snapshotTtl(),
                connectionPool, policy);
    }
}
