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

    private final io.lettuce.core.api.StatefulRedisConnection<String, String> sharedConnection;
    private final RedisCommands<String, String> shared;
    private final ThreadLocal<RedisCommands<String, String>> txCommands = new ThreadLocal<>();

    RedisSync(StatefulRedisConnection<String, String> sharedConnection) {
        this.sharedConnection = sharedConnection;
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

    /**
     * 批量 HGETALL（spec 58 §A / T259）：共享连接 async 流水线（N 次往返 → 一次批量
     * 提交，响应按序收回）；事务绑定线程退化为逐键 sync（MULTI 队列内禁与 async 混用）。
     */
    java.util.List<java.util.Map<String, String>> batchHgetAll(java.util.List<String> keys) {
        if (keys.isEmpty()) {
            return java.util.List.of();
        }
        RedisCommands<String, String> tx = txCommands.get();
        if (tx != null) {
            return keys.stream().map(tx::hgetall).toList();
        }
        io.lettuce.core.api.async.RedisAsyncCommands<String, String> async = sharedConnection.async();
        java.util.List<io.lettuce.core.RedisFuture<java.util.Map<String, String>>> futures =
                new java.util.ArrayList<>(keys.size());
        for (String k : keys) {
            futures.add(async.hgetall(k));
        }
        java.util.List<java.util.Map<String, String>> results = new java.util.ArrayList<>(keys.size());
        for (io.lettuce.core.RedisFuture<java.util.Map<String, String>> f : futures) {
            try {
                results.add(f.get(10, java.util.concurrent.TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new IllegalStateException("Redis 批量 HGETALL 等待失败", e);
            }
        }
        return results;
    }
}
