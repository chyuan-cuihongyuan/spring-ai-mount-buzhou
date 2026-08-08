package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis 命令解析器：按线程返回当前应使用的同步命令句柄。
 *
 * <p>正常路径返回共享连接的 {@link RedisCommands}；当线程处于 {@link RedisUnitOfWork} 的事务中时，
 * 返回该事务独占连接（已进入 MULTI 模式）的句柄——store 的写操作因此自动入队事务，
 * exec 时原子提交、discard 时回滚，store 无需感知事务边界。
 *
 * <p>虚拟线程安全：绑定状态走 {@link ThreadLocal}（与 core SpanContext 显式传递的抗串味口径一致）。
 */
final class RedisSync {

    private final RedisCommands<String, String> shared;
    private final ThreadLocal<RedisCommands<String, String>> txCommands = new ThreadLocal<>();

    RedisSync(StatefulRedisConnection<String, String> sharedConnection) {
        this.shared = sharedConnection.sync();
    }

    RedisCommands<String, String> commands() {
        RedisCommands<String, String> tx = txCommands.get();
        return tx != null ? tx : shared;
    }

    /** 事务开启：绑定独占连接（已 multi）。 */
    void bindTransaction(RedisCommands<String, String> commands) {
        txCommands.set(commands);
    }

    /** 事务结束：解除绑定。 */
    void clearTransaction() {
        txCommands.remove();
    }
}
