package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.lettuce.core.RedisClient;

import java.time.Duration;

/**
 * Redis store 装配入口（spec 08 store 实现按需引入；用户配 {@code buzhou.store.type=redis} 激活）。
 *
 * <pre>{@code
 * RedisClient client = RedisClient.create("redis://localhost:6379");
 * BuzhouStores stores = RedisBuzhouStores.create(client, "buzhou:");
 * }</pre>
 *
 * <p>共享一条连接跑常规读写；{@link RedisUnitOfWork} 每事务独占一条连接做 MULTI/EXEC。
 * 调用方拥有 {@link RedisClient} 生命周期（{@code client.shutdown()} 会关闭派生连接）。
 *
 * @param client      Lettuce RedisClient（调用方拥有生命周期）
 * @param keyPrefix   key 前缀（默认 {@code buzhou:}，spec 08 {@code buzhou.store.redis.key-prefix}）
 * @param snapshotTtl 注入快照 TTL；{@link Duration#ZERO} 不过期（默认）。spec 03 {@code snapshot.ttl}
 */
public final class RedisBuzhouStores {

    private RedisBuzhouStores() {
    }

    /** 默认前缀 {@code buzhou:}、快照不过期。 */
    public static BuzhouStores create(RedisClient client, String keyPrefix) {
        return create(client, keyPrefix, Duration.ZERO);
    }

    public static BuzhouStores create(RedisClient client, String keyPrefix, Duration snapshotTtl) {
        RedisSync sync = new RedisSync(client.connect());
        RedisKeys keys = new RedisKeys(keyPrefix);
        return new BuzhouStores(
                new RedisMessageStore(sync, keys),
                new RedisSummaryStore(sync, keys),
                new RedisSessionStateStore(sync, keys),
                new RedisSessionLeaseStore(sync, keys),
                new RedisObservabilityStore(sync, keys, snapshotTtl),
                new RedisUnitOfWork(client, sync));
    }
}
