package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

/**
 * Redis store 装配入口（spec 08 store 实现按需引入；用户配 {@code buzhou.store.type=redis} 激活）。
 *
 * <pre>{@code
 * RedisClient client = RedisClient.create("redis://localhost:6379");
 * BuzhouStores stores = RedisBuzhouStores.createPooled(client, "buzhou:", Duration.ZERO, 8, null);
 * }</pre>
 *
 * <p>共享一条连接跑常规读写；{@link RedisUnitOfWork}（ticket 32 / spec 13 §stores-7）
 * 事务连接经 Lettuce {@link ConnectionPoolSupport} 池化复用（上限可配，见
 * {@link #createPooled}）。调用方拥有 {@link RedisClient} 生命周期
 * （{@code client.shutdown()} 会关闭派生连接；自建池的完整关闭见
 * {@link #createPooled(RedisClient, String, Duration, int, WriteFailurePolicy)} 注记）。
 *
 * @param client      Lettuce RedisClient（调用方拥有生命周期）
 * @param keyPrefix   key 前缀（默认 {@code buzhou:}，spec 08 {@code buzhou.store.redis.key-prefix}）
 * @param snapshotTtl 注入快照 TTL；{@link Duration#ZERO} 不过期（默认）。spec 03 {@code snapshot.ttl}
 */
public final class RedisBuzhouStores {

    /** 事务连接池默认上限（{@code buzhou.store.redis.pool-max-size} 缺省值）。 */
    public static final int DEFAULT_POOL_MAX_SIZE = 8;

    private RedisBuzhouStores() {
    }

    /** 默认前缀 {@code buzhou:}、快照不过期、FAIL_TURN、每事务新建连接（兼容旧路径）。 */
    public static BuzhouStores create(RedisClient client, String keyPrefix) {
        return create(client, keyPrefix, Duration.ZERO);
    }

    /**
     * 旧装配路径（保留兼容）：每事务 {@code client.connect()} 新建独占连接。
     *
     * @deprecated 改用 {@link #createPooled}（事务连接池化复用，spec 13 §stores-7）。
     */
    @Deprecated(since = "0.1.0", forRemoval = false)
    public static BuzhouStores create(RedisClient client, String keyPrefix, Duration snapshotTtl) {
        RedisSync sync = new RedisSync(client.connect());
        RedisKeys keys = new RedisKeys(keyPrefix);
        return assemble(sync, keys, snapshotTtl, new RedisUnitOfWork(client, sync));
    }

    /**
     * 池化装配（推荐入口，ticket 32）：事务连接池上限 {@code poolMaxSize}、
     * 写失败策略 {@code writeFailurePolicy}（null 按 {@link WriteFailurePolicy#FAIL_TURN}）。
     *
     * <p><b>池生命周期注记</b>：本便捷重载内部自建池且不外泄句柄——适用于进程生命周期内
     * 常驻的 store（应用退出随进程回收）。需要显式关闭池的部署（多租户/热重建）改用
     * {@link #createPooled(RedisClient, String, Duration, GenericObjectPool, WriteFailurePolicy)}
     * 自持池并在停机时 {@code pool.close()}。
     *
     * @param client             Lettuce RedisClient
     * @param keyPrefix          key 前缀
     * @param snapshotTtl        注入快照 TTL
     * @param poolMaxSize        事务连接池上限（<=0 按默认 {@value #DEFAULT_POOL_MAX_SIZE}）
     * @param writeFailurePolicy 写失败策略（null = FAIL_TURN）
     * @return core 六槽存储组合
     */
    public static BuzhouStores createPooled(RedisClient client, String keyPrefix, Duration snapshotTtl,
                                            int poolMaxSize, WriteFailurePolicy writeFailurePolicy) {
        return createPooled(client, keyPrefix, snapshotTtl,
                newConnectionPool(client, poolMaxSize), writeFailurePolicy);
    }

    /**
     * 池化装配（调用方持有池生命周期）：装配层（{@code BuzhouRedisStoreAutoConfiguration}）
     * 把池注册为 {@code destroyMethod="close"} 的 bean 时用本重载。
     *
     * @param connectionPool 事务连接池（经 {@link #newConnectionPool} 或等价方式创建）
     * @param writeFailurePolicy 写失败策略（null = FAIL_TURN）
     */
    public static BuzhouStores createPooled(RedisClient client, String keyPrefix, Duration snapshotTtl,
                                            GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool,
                                            WriteFailurePolicy writeFailurePolicy) {
        RedisSync sync = new RedisSync(client.connect());
        RedisKeys keys = new RedisKeys(keyPrefix);
        return assemble(sync, keys, snapshotTtl, new RedisUnitOfWork(connectionPool, sync),
                writeFailurePolicy);
    }

    /**
     * 建 Lettuce 连接池（ConnectionPoolSupport：借出连接、归还未损坏连接复用）。
     *
     * <p>{@code testOnReturn}：事务失败路径（discard 后）归还的连接经 lettuce 工厂的
     * {@code isOpen} 校验，坏连接销毁重建——Lettuce 借出的是代理对象，
     * commons-pool2 的 {@code invalidateObject} 不接受代理，故以「归还 + 校验」实现坏连接淘汰。
     */
    public static GenericObjectPool<StatefulRedisConnection<String, String>> newConnectionPool(
            RedisClient client, int maxSize) {
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> config =
                new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxSize > 0 ? maxSize : DEFAULT_POOL_MAX_SIZE);
        config.setTestOnReturn(true);
        return ConnectionPoolSupport.createGenericObjectPool(client::connect, config);
    }

    private static BuzhouStores assemble(RedisSync sync, RedisKeys keys, Duration snapshotTtl,
                                         RedisUnitOfWork unitOfWork) {
        return assemble(sync, keys, snapshotTtl, unitOfWork, null);
    }

    private static BuzhouStores assemble(RedisSync sync, RedisKeys keys, Duration snapshotTtl,
                                         RedisUnitOfWork unitOfWork, WriteFailurePolicy writeFailurePolicy) {
        ObservabilityStore observability = new RedisObservabilityStore(sync, keys, snapshotTtl);
        if (writeFailurePolicy == WriteFailurePolicy.DEGRADE) {
            // DEGRADE 才装饰（观测类写 WARN + 计数继续）；FAIL_TURN 保持既有原样抛语义
            observability = new DegradingObservabilityStore(observability, writeFailurePolicy);
        }
        return new BuzhouStores(
                new RedisMessageStore(sync, keys),
                new RedisSummaryStore(sync, keys),
                new RedisSessionStateStore(sync, keys),
                new RedisSessionLeaseStore(sync, keys),
                observability,
                unitOfWork);
    }
}
