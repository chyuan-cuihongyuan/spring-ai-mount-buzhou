package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.function.Supplier;

/**
 * Redis {@link UnitOfWork}：每个事务独占一条连接（{@link RedisClient#connect()}），进 MULTI 模式后
 * 把命令句柄经 {@link RedisSync} 绑到当前线程——store 的写操作因此自动入队该事务，
 * {@code exec} 原子提交、{@code discard} 回滚。
 *
 * <p><b>为什么独占连接而非共享连接 MULTI</b>：Lettuce 连接的事务状态是连接级、非线程隔离；
 * 多线程共享同一连接做 MULTI 会互相串入。独占连接保证事务隔离，{@link RedisSync} 按 ThreadLocal
 * 绑定保证 store 路由到正确句柄。
 *
 * <p><b>边界</b>（spec 08 开放问题 #2）：事务内忌「读-改-写」——MULTI 下读命令入队、exec 前无返回；
 * 本实现用于「一轮写批」（消息多条 + state 多 key），CAS/lease 等需即时返回的操作不在事务内调用。
 */
public class RedisUnitOfWork implements UnitOfWork {

    private final RedisClient client;
    private final RedisSync sync;

    public RedisUnitOfWork(RedisClient client, RedisSync sync) {
        this.client = client;
        this.sync = sync;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> work) {
        try (StatefulRedisConnection<String, String> conn = client.connect()) {
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
    }

    private static void safeDiscard(RedisCommands<String, String> commands) {
        try {
            commands.discard();
        } catch (RuntimeException ignored) {
            // 连接异常或已非事务态：忽略，事务连接随 try-with-resources 关闭
        }
    }
}
