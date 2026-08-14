package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Redis {@link UnitOfWork}：每个事务独占一条<b>池化</b>连接（spec 13 §stores-7 / ticket 32
 * 经 Lettuce {@code ConnectionPoolSupport} 池化，上限可配
 * {@code buzhou.store.redis.pool-max-size}），进 MULTI 模式后把命令句柄经
 * {@link RedisSync} 绑到当前线程——store 的写操作因此自动入队该事务，
 * {@code exec} 原子提交、{@code discard} 回滚；事务结束连接归还池中复用。
 *
 * <p><b>为什么独占连接而非共享连接 MULTI</b>：Lettuce 连接的事务状态是连接级、非线程隔离；
 * 多线程共享同一连接做 MULTI 会互相串入。独占连接保证事务隔离，{@link RedisSync} 按 ThreadLocal
 * 绑定保证 store 路由到正确句柄。
 *
 * <p><b>池化与生命周期</b>：池由调用方（{@link RedisBuzhouStores#createPooled} 或装配层
 * 的 {@code GenericObjectPool} bean）创建并持有；借出异常（池耗尽等）原样上抛
 * （FAIL_TURN 语义）。旧构造器（直连 {@link RedisClient#connect()}，每事务新建连接）
 * 保留兼容手工装配调用方。
 *
 * <p><b>边界</b>（spec 08 开放问题 #2）：事务内忌「读-改-写」——MULTI 下读命令入队、
 * exec 前无返回；本实现用于「一轮写批」（消息多条 + state 多 key），CAS/lease 等
 * 需即时返回的操作不在事务内调用。
 */
public class RedisUnitOfWork implements UnitOfWork {

    private static final Logger LOG = LoggerFactory.getLogger(RedisUnitOfWork.class);

    private final GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;

    /** 池未提供时按需直连的客户端（旧路径：每事务 {@code client.connect()} 新建连接）。 */
    private final RedisClient fallbackClient;

    private final RedisSync sync;

    /** 旧路径：每事务新建独占连接（未池化，保留兼容既有手工装配）。 */
    public RedisUnitOfWork(RedisClient client, RedisSync sync) {
        this.connectionPool = null;
        this.fallbackClient = client;
        this.sync = sync;
    }

    /** 池化路径（ticket 32）：事务连接从共享池借出、用毕归还。 */
    public RedisUnitOfWork(GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool,
                           RedisSync sync) {
        this.connectionPool = connectionPool;
        this.fallbackClient = null;
        this.sync = sync;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> work) {
        return connectionPool != null ? executePooled(work) : executeWithFreshConnection(work);
    }

    private <T> T executePooled(Supplier<T> work) {
        StatefulRedisConnection<String, String> conn;
        try {
            conn = connectionPool.borrowObject();
        } catch (Exception e) {
            // 池借出失败（耗尽/中断）原样上抛：存储不可用不属于可降级范畴
            throw new IllegalStateException("Redis 事务连接池借出失败", e);
        }
        try {
            return runInMulti(conn, work);
        } finally {
            try {
                // 成功与失败路径统一归还（失败路径 discard 已把连接恢复非事务态；
                // 池配 testOnReturn：损坏连接在归还时经校验销毁重建——Lettuce 借出的是
                // 代理对象，commons-pool2 的 invalidateObject 不接受代理，故不走销毁通道）
                connectionPool.returnObject(conn);
            } catch (RuntimeException e) {
                LOG.warn("Redis 事务连接归还池失败（该槽位可能泄漏直至池重建）", e);
            }
        }
    }

    private <T> T executeWithFreshConnection(Supplier<T> work) {
        try (StatefulRedisConnection<String, String> conn = fallbackClient.connect()) {
            return runInMulti(conn, work);
        }
    }

    private <T> T runInMulti(StatefulRedisConnection<String, String> conn, Supplier<T> work) {
        RedisCommands<String, String> commands = conn.sync();
        commands.multi();
        sync.bindTransaction(commands);
        try {
            T result = work.get();
            commands.exec();
            return result;
        } catch (RuntimeException e) {
            safeDiscard(commands);
            throw e;
        } finally {
            sync.clearTransaction();
        }
    }

    private static void safeDiscard(RedisCommands<String, String> commands) {
        try {
            commands.discard();
        } catch (RuntimeException ignored) {
            // 连接异常或已非事务态：忽略——归还池时 testOnReturn 校验会销毁坏连接
        }
    }
}
